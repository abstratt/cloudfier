package com.abstratt.mdd.core.runtime.types;

import org.eclipse.uml2.uml.Operation;

import com.abstratt.mdd.core.runtime.ExecutionContext;
import com.abstratt.mdd.core.runtime.MetaClass;
import com.abstratt.mdd.core.runtime.MethodInvoker;
import com.abstratt.mdd.core.runtime.ModelExecutionException;
import com.abstratt.mdd.core.runtime.ObjectNotActiveException;
import com.abstratt.mdd.core.runtime.Runtime;
import com.abstratt.pluginutils.LogUtils;

/**
 * A basic ancestor for all runtime types.
 */
public abstract class BasicType {

    // public abstract ITypeInfo getTypeInfo();

    /**
     * <p>
     * Tries to invoke a Java method corresponding the given UML operation. If a
     * suitable method cannot be found, throws a corresponding checked
     * exception. If the method can be found but execution fails, throws as
     * runtime exception wrapping the original issue. Otherwise, if it succeeds,
     * returns the result of the invocation.
     */
    public static Object runNativeOperation(ExecutionContext context, Class javaClass, Object target, Operation operation,
            Object... arguments) {
        try {
            return MethodInvoker.tryToInvoke(context, javaClass, target, operation, arguments);
        } catch (NullPointerException e) {
            LogUtils.logWarning(Runtime.ID, "Null was dereferenced", e);
            throw new ModelExecutionException("Null was dereferenced", operation, null);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e + " - " + target.getClass().getName());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns whether this object is equal to the given object
     * 
     * @param context
     * @param another
     * @return
     */
    public BooleanType equals(ExecutionContext context, BasicType another) {
        return BooleanType.fromValue(this.equals(another));
    }

    public abstract String getClassifierName();

    public abstract MetaClass<?> getMetaClass();

    public boolean isCollection() {
        return false;
    }

    public boolean isEmpty() {
        return false;
    }

    public BooleanType notEquals(ExecutionContext context, BasicType another) {
        return equals(context, another).not(context);
    }

    /**
     * @param context
     */
    public BooleanType same(ExecutionContext context, BasicType other) {
        return BooleanType.fromValue(this == other);
    }

    /**
     * @param context
     */
    public StringType toString(ExecutionContext context) {
        return new StringType(this.toString());
    }

    /**
     * Should be invoked by any method implementing instance method invocation.
     * <p>
     * Does nothing by default. Subclasses that support the concept of
     * active/inactive instances should override.
     * </p>
     * 
     * @throws ObjectNotActiveException
     */
    protected void ensureActive() throws ObjectNotActiveException {
        // does nothing by default
    }

    /**
     * Converts model operation names to Java method names. By default, does
     * nothing.
     * 
     * @param modelName
     *            the operation name as modeled
     * @return the corresponding Java method name
     */
    protected String translateOperationName(String modelName) {
        return null;
    }

}