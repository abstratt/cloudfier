package com.abstratt.mdd.core.runtime;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Operation;

import com.abstratt.mdd.core.runtime.types.BasicType;

/**
 * Base protocol for metaclasses.
 */
public interface MetaClass<T> {
    MetaClass<?> NOT_IMPLEMENTED = new MetaClass<Object>() {
        @Override
        public BasicType runOperation(ExecutionContext context, BasicType target, Operation operation, BasicType... arguments) {
            throw new UnsupportedOperationException();
        }
    };

    public default void handleEvent(RuntimeEvent runtimeEvent) {
    	
    }
    
    public default List<? extends BasicType> getAllInstances(Classifier classifier, boolean includeSubTypes) {
    	return Collections.emptyList();
    }

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
    public BasicType runOperation(ExecutionContext context, BasicType target, Operation operation, BasicType... arguments);
}
