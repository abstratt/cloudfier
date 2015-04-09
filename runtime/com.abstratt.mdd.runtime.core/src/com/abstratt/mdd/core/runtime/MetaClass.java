package com.abstratt.mdd.core.runtime;

import org.eclipse.uml2.uml.Operation;

import com.abstratt.mdd.core.runtime.types.BasicType;

/**
 * Base protocol for metaclasses.
 */
public interface MetaClass<T> {
    MetaClass<?> NOT_IMPLEMENTED = new MetaClass<Object>() {
        @Override
        public void handleEvent(RuntimeEvent runtimeEvent) {
        }

        @Override
        public Object runOperation(ExecutionContext context, BasicType target, Operation operation, Object... arguments) {
            throw new UnsupportedOperationException();
        }
    };

    public void handleEvent(RuntimeEvent runtimeEvent);

    /**
     * Invokes the given class operation for this metaclass.
     * 
     * @param context
     * @param target
     *            object, possibly null
     * @param operation
     * @param arguments
     * @return
     * @throws UnsupportedOperationException
     *             if this invokable is not a class object
     */
    public Object runOperation(ExecutionContext context, BasicType target, Operation operation, Object... arguments);
}
