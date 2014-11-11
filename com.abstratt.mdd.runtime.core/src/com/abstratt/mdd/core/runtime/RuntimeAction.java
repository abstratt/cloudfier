package com.abstratt.mdd.core.runtime;

import static com.abstratt.mdd.core.runtime.RuntimeActionState.COMPLETE;
import static com.abstratt.mdd.core.runtime.RuntimeActionState.EXECUTING;
import static com.abstratt.mdd.core.runtime.RuntimeActionState.READY;
import static com.abstratt.mdd.core.runtime.RuntimeActionState.WAITING;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.InputPin;
import org.eclipse.uml2.uml.ObjectFlow;
import org.eclipse.uml2.uml.ObjectNode;
import org.eclipse.uml2.uml.OutputPin;
import org.eclipse.uml2.uml.Pin;

import com.abstratt.mdd.core.runtime.types.BasicType;
import com.abstratt.mdd.core.util.ActivityUtils;

/**
 * Represents an executing action, its state and runtime connections with other
 * actions.
 *
 * <p>
 * Clients should not extend this class.
 * </p>
 */
public abstract class RuntimeAction {

    static void buildDeepDebugLine(StringBuffer result, String prefix, String line) {
        result.append(prefix);
        result.append(line);
        result.append("\n");
    }

    private Action instance;

    protected List<RuntimeObjectNode> objectNodes = new ArrayList<RuntimeObjectNode>();

    private CompositeRuntimeAction parent;

    protected RuntimeActionState state = WAITING;

    public RuntimeAction(Action instance, CompositeRuntimeAction parent) {
        this.instance = instance;
        this.parent = parent;
        createObjectNodes();
    }

    public void buildConnections() {
        for (RuntimeObjectNode source : objectNodes) {
            List outgoings = source.getInstance().getOutgoings();
            for (Iterator j = outgoings.iterator(); j.hasNext();) {
                ObjectNode destinationNode = (ObjectNode) ((ObjectFlow) j.next()).getTarget();
                // XXX is the assumption that an action will always contain its
                // object nodes valid?
                // XXX do ExpansionRegions need to contain their
                // ExpansionNodes??? Don't think so...
                Action targetOwner = (Action) destinationNode.getOwner();
                RuntimeAction runtimeTargetOwner = getRuntimeAction(targetOwner);
                RuntimeObjectNode target = runtimeTargetOwner.getRuntimeObjectNode(destinationNode);
                if (target == null)
                    throw new IllegalStateException(destinationNode.eClass().getName() + " not found in " + targetOwner.eClass().getName());
                new RuntimeObjectFlow(source, target);
            }
        }
    }

    /**
     * Executes this action.
     * 
     * @param context
     * 
     * @throws IllegalStateException
     *             if the action state is not READY
     */
    public final void execute(final ExecutionContext context) {
        if (getState() != READY)
            throw new IllegalStateException();
        try {
            context.currentFrame().recordCallSite(instance);
            setState(EXECUTING);
            this.executeBehavior(context);
            // run after advices
            // execution completed
            setState(COMPLETE);
            transferValues();
            if (ActivityUtils.isFinal(getInstance()))
                throw new ActivityFinishedException();
        } catch (ModelExecutionException rre) {
            throw rre;
        } catch (ActivityFinishedException afe) {
            throw afe;
        }
    }

    public RuntimeObjectNode findRuntimeObjectNode(ObjectNode node) {
        return getRuntimeObjectNode(node);
    }

    public List<RuntimeObjectNode> getInputs() {
        List outputPins = getStaticInputs();
        List<RuntimeObjectNode> runtimeInputPins = new ArrayList<RuntimeObjectNode>(outputPins.size());
        for (Iterator i = outputPins.iterator(); i.hasNext();)
            runtimeInputPins.add(getRuntimeObjectNode((ObjectNode) i.next()));
        return runtimeInputPins;
    }

    public Action getInstance() {
        return this.instance;
    }

    public List<RuntimeObjectNode> getOutputs() {
        List outputPins = getStaticOutputs();
        List<RuntimeObjectNode> runtimeOutputPins = new ArrayList<RuntimeObjectNode>(outputPins.size());
        for (Iterator i = outputPins.iterator(); i.hasNext();)
            runtimeOutputPins.add(getRuntimeObjectNode((ObjectNode) i.next()));
        return runtimeOutputPins;
    }

    public RuntimeObjectNode getRuntimeObjectNode(ObjectNode node) {
        for (RuntimeObjectNode current : objectNodes)
            if (current.getInstance() == node)
                return current;
        if (parent != null)
            return parent.getRuntimeObjectNode(node);
        return null;
    }

    public List<RuntimeObjectNode> getRuntimeObjectNodes() {
        return Collections.unmodifiableList(this.objectNodes);
    }

    /**
     * Returns a collection of source actions. Source actions are actions that
     * own object nodes that feed any of this action's input pins.
     * 
     * @return a collection of source actions
     */
    public Collection<RuntimeAction> getSourceActions() {
        Set<RuntimeAction> sources = new HashSet<RuntimeAction>(objectNodes.size());
        for (RuntimeObjectNode current : objectNodes)
            if (current.isInput())
                for (RuntimeObjectFlow flow : current.getIncoming())
                    sources.add(flow.getSource().getAction());
        return sources;
    }

    public RuntimeActionState getState() {
        if (peekState() == WAITING)
            tryToBeReady();
        return peekState();
    }

    public boolean isComplete() {
        return peekState() == COMPLETE;
    }

    public RuntimeActionState peekState() {
        return state;
    }

    public void reset(boolean propagate) {
        setState(WAITING);
        if (propagate)
            for (RuntimeObjectNode current : objectNodes)
                current.reset(true);
    }

    @Override
    public String toString() {
        return this.instance.eClass().getName();
    }

    /**
     * Tries to transition to the READY state if not there yet (are we there
     * yet?).
     */
    public final void tryToBeReady() {
        // not waiting anymore
        if (peekState() != WAITING)
            return;
        // the parent action (e.g. group) is not in the EXECUTING state, so
        // should not change state
        if (parent != null && parent.getState() != EXECUTING)
            return;
        // usually, all an action needs to be ready is that its input pins are
        // set
        // (some more specialized actions - such as ExpansionRegions - impose
        // further restrictions)
        for (RuntimeObjectNode current : objectNodes)
            if (current.isInput() && !current.isReady())
                // an input pin has not been fed yet, keep waiting
                return;
        // now we are ready
        setState(READY);
    }

    protected void addResultValue(OutputPin resultPin, BasicType resultValue) {
        getRuntimeObjectNode(resultPin).addValue(resultValue);
    }

    protected void createObjectNodes() {
        for (Element current : instance.getOwnedElements())
            if (current instanceof Pin)
                objectNodes.add(createRuntimeObjectNode((ObjectNode) current));
    }

    protected RuntimeObjectNode createRuntimeObjectNode(ObjectNode node) {
        assert node instanceof Pin : node.getClass();
        return node instanceof InputPin ? new RuntimeInputPin(this, (InputPin) node) : new RuntimeOutputPin(this, (OutputPin) node);
    }

    /**
     * Performs the runtime behavior of an action Input/Output pins will be
     * available off the action object.
     * 
     * @param context
     */
    protected abstract void executeBehavior(ExecutionContext context);

    protected CompositeRuntimeAction getParent() {
        return parent;
    }

    protected RuntimeAction getRuntimeAction(Action instance) {
        if (getInstance() == instance)
            return this;
        return parent != null ? parent.getRuntimeAction(instance) : null;
    }

    protected List<InputPin> getStaticInputs() {
        return ActivityUtils.getActionInputs(instance);
    }

    protected List<OutputPin> getStaticOutputs() {
        return ActivityUtils.getActionOutputs(instance);
    }

    private void setState(RuntimeActionState newState) {
        this.state = newState;
    }

    private void transferValues() {
        List<RuntimeObjectNode> outputs = getOutputs();
        for (RuntimeObjectNode node : outputs)
            node.transferValues();
    }

}