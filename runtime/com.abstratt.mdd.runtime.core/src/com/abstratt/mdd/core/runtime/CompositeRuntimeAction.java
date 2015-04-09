package com.abstratt.mdd.core.runtime;

import static com.abstratt.mdd.core.runtime.RuntimeActionState.READY;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.ObjectNode;
import org.eclipse.uml2.uml.StructuredActivityNode;

import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.util.MDDUtil;

/**
 * A composite runtime action can contain other runtime actions.
 */
public abstract class CompositeRuntimeAction extends RuntimeAction {
    private Set<RuntimeAction> visited = new HashSet<RuntimeAction>();
    private List<RuntimeAction> sequence = new LinkedList<RuntimeAction>();
    private Map<Action, RuntimeAction> subActions = new HashMap<Action, RuntimeAction>();

    public CompositeRuntimeAction(Action actionNode, CompositeRuntimeAction parent) {
        super(actionNode, parent);
        StructuredActivityNode structuredActionNode = (StructuredActivityNode) actionNode;
        Collection<Action> subActions = MDDUtil.filterByClass(structuredActionNode.getNodes(), IRepository.PACKAGE.getAction());
        createSubActions(subActions);
        connectSubActions();
    }

    /**
     * 
     * @param runtimeAction
     * @param context
     * @return
     */
    public int executeContainedAction(RuntimeAction runtimeAction, ExecutionContext context) {
        if (!visited.add(runtimeAction))
            return 0;
        try {
            switch (runtimeAction.getState()) {
            case READY:
                runtimeAction.execute(context);
                return 1;
            case WAITING:
                break;
            default:
                return 0;
            }
            int executed = 0;
            for (RuntimeAction source : runtimeAction.getSourceActions())
                executed += executeContainedAction(source, context);
            if (READY == runtimeAction.getState()) {
                runtimeAction.execute(context);
                executed++;
            }
            return executed;
        } finally {
            visited.remove(runtimeAction);
        }
    }

    @Override
    public RuntimeObjectNode findRuntimeObjectNode(ObjectNode node) {
        RuntimeObjectNode found = getRuntimeObjectNode(node);
        if (found != null)
            return found;
        for (RuntimeAction subAction : subActions.values())
            if ((found = subAction.findRuntimeObjectNode(node)) != null)
                return found;
        return null;
    }

    @Override
    public void reset(boolean propagate) {
        super.reset(propagate);
        for (Object element : this.subActions.values()) {
            RuntimeAction current = (RuntimeAction) element;
            current.reset(propagate);
        }
    }

    @Override
    protected void createObjectNodes() {
        super.createObjectNodes();
        List ownedElements = ((StructuredActivityNode) getInstance()).getNodes();
        for (Iterator i = ownedElements.iterator(); i.hasNext();) {
            Element current = (Element) i.next();
            if (current instanceof ObjectNode)
                objectNodes.add(createRuntimeObjectNode((ObjectNode) current));
        }
    }

    /**
     * Returns the contained runtime action that corresponds to the given
     * instance.
     */
    @Override
    protected RuntimeAction getRuntimeAction(Action action) {
        RuntimeAction localResult = subActions.get(action);
        return localResult != null ? localResult : super.getRuntimeAction(action);
    }

    /**
     * Triggers the creation of runtime object flows for this action and any
     * actions found under it.
     * <p>
     * The composite runtime action provides a scope where runtime actions are
     * created.
     * </p>
     * XXX an undesirable effect is that currently no sub actions may have flows
     * to/from actions in different scopes
     * 
     * @param nodes
     */
    private void connectSubActions() {
        for (Action node : subActions.keySet()) {
            RuntimeAction action = getRuntimeAction(node);
            assert action != null;
            action.buildConnections();
        }
    }

    private RuntimeAction createRuntimeAction(Action action) {
        final RuntimeAction runtimeAction = Runtime.getCurrentRuntime().createAction(action, this);
        sequence.add(runtimeAction);
        return subActions.put(action, runtimeAction);
    }

    private void createSubActions(Collection<Action> nodes) {
        for (Action action : nodes)
            createRuntimeAction(action);
    }

}