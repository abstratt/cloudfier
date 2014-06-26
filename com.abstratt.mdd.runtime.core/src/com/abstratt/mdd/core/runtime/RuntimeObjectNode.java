package com.abstratt.mdd.core.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.uml2.uml.ObjectNode;
import org.eclipse.uml2.uml.ValueSpecification;

import com.abstratt.mdd.core.runtime.types.BasicType;
import com.abstratt.mdd.core.runtime.types.IntegerType;

public abstract class RuntimeObjectNode {
    private RuntimeAction action;
    private List<RuntimeObjectFlow> incoming;

    private ObjectNode instance;
    private List<RuntimeObjectFlow> outgoing;
    private List<BasicType> values;

    public RuntimeObjectNode(RuntimeAction action, ObjectNode instance) {
        super();
        this.action = action;
        this.instance = instance;
        this.values = new ArrayList<BasicType>();
        this.incoming = new ArrayList<RuntimeObjectFlow>();
        this.outgoing = new ArrayList<RuntimeObjectFlow>();
    }

    public void addIncoming(RuntimeObjectFlow edge) {
        this.incoming.add(edge);
    }

    public void addOutgoing(RuntimeObjectFlow edge) {
        this.outgoing.add(edge);
    }

    /**
     * <strong>12.3.38 ObjectNode (from BasicActivities,
     * CompleteActivities)</strong> - &quot;An object node may not contain more
     * tokens than its upper bound.&quot;
     * 
     * @param newValue
     */
    public void addValue(BasicType newValue) {
        basicAddValue(newValue);
    }

    public void basicSetValue(BasicType newValue) {
        // assert getUpperBound() == 1 || newValue instanceof CollectionType :
        // instance.getName() + " - " + newValue.getClassifierName() +
        // " - upper: " + getUpperBound();
        assert values.isEmpty();
        basicAddValue(newValue);
    }

    public void buildDeepDebugString(StringBuffer buffer, String prefix) {
        RuntimeAction.buildDeepDebugLine(buffer, prefix, toString());
        String childPrefix = " " + prefix;
        for (RuntimeObjectFlow flow : this.incoming)
            RuntimeAction.buildDeepDebugLine(buffer, childPrefix, "<----" + flow.getSource().toString());
        for (RuntimeObjectFlow flow : this.outgoing)
            RuntimeAction.buildDeepDebugLine(buffer, childPrefix, "---->" + flow.getTarget().toString());
    }

    public BasicType consumeValue() {
        if (values.isEmpty())
            throw new NoDataAvailableException();
        return values.remove(0);
    }

    public RuntimeAction getAction() {
        return action;
    }

    public List<RuntimeObjectFlow> getIncoming() {
        return Collections.unmodifiableList(incoming);
    }

    public ObjectNode getInstance() {
        return instance;
    }

    public List<RuntimeObjectFlow> getOutgoing() {
        return Collections.unmodifiableList(outgoing);
    }

    /**
     * Returns this node's value or <code>null</code>. This operation is illegal
     * if the upper bound is greater than 1.
     * 
     * @return the node value or <code>null</code>
     */
    public BasicType getValue() {
        return consumeValue();
    }

    public int getValueCount() {
        return values.size();
    }

    public boolean isFull() {
        int upperBound = getUpperBound();
        return values.size() >= upperBound && upperBound != -1;
    }

    public abstract boolean isInput();

    public boolean isReady() {
        for (RuntimeObjectFlow current : incoming)
            if (!current.getSource().getAction().isComplete())
                return false;
        return true;
    }

    public List<BasicType> peekAllValues() {
        return values;
    }

    public BasicType peekValue() {
        if (values.isEmpty())
            throw new NoDataAvailableException();
        return values.get(0);
    }

    public void reset(boolean propagate) {
        this.values.clear();
        if (propagate)
            for (RuntimeObjectFlow edge : outgoing)
                edge.getTarget().getAction().reset(true);
    }

    /**
     * Sets this node's value. This operation is illegal if the upper bound is
     * greater than 1, or if a value has already been set.
     * 
     * @param the
     *            new value
     */
    public void setValue(BasicType newValue) {
        basicSetValue(newValue);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(this)) + ": " + values.toString()
                + " - action: " + action;
    }

    public void transferValues() {
        for (RuntimeObjectFlow current : outgoing)
            current.tryToTransfer();
    }

    protected void basicAddValue(BasicType newValue) {
        if (isFull())
            throw new ObjectNodeIsFullException(this.toString());
        values.add(newValue);
    }

    private int getUpperBound() {
        ValueSpecification upperBoundSpec = instance.getUpperBound();
        if (upperBoundSpec == null)
            // default upper bound is unlimited
            return -1;
        return ((IntegerType) RuntimeUtils.extractValueFromSpecification(upperBoundSpec)).primitiveValue().intValue();
    }
}