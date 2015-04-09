package com.abstratt.mdd.core.runtime;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.uml2.uml.Operation;

import com.abstratt.mdd.core.runtime.types.BasicType;

public class MethodInvoker {
    /**
     * Tries to invoke a method operation on a Java object or class (if static).
     */
    public static Object tryToInvoke(ExecutionContext context, Class<?> javaClass, Object target, Operation operation, Object... arguments)
            throws NoSuchMethodException, IllegalAccessException {
        Class<?>[] argumentTypes = new Class[arguments.length + 1];
        argumentTypes[0] = ExecutionContext.class;
        for (int i = 0; i < arguments.length; i++)
            argumentTypes[i + 1] = arguments[i] == null ? BasicType.class : arguments[i].getClass();
        Object[] enhancedArguments = new Object[arguments.length + 1];
        enhancedArguments[0] = context;
        System.arraycopy(arguments, 0, enhancedArguments, 1, arguments.length);
        try {
            return MethodInvoker.invokeOperation(javaClass, target, operation.getName(), enhancedArguments);
        } catch (InvocationTargetException e) {
            Throwable unexpected = e.getTargetException();
            if (unexpected instanceof RuntimeException)
                throw (RuntimeException) unexpected;
            if (unexpected instanceof Error)
                throw (Error) unexpected;
            throw new RuntimeException(unexpected);
        }
    }

    private static Object invokeOperation(Class<?> javaClass, Object target, String operationName, Object[] arguments)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method[] methods = javaClass.getDeclaredMethods();
        for (Method method : methods)
            if (method.getName().equals(operationName) && MethodInvoker.isCompatibleArguments(method.getParameterTypes(), arguments))
                return method.invoke(target, arguments);
        Class<?> superClass = javaClass.getSuperclass();
        if (superClass == null || superClass == Object.class)
            throw new NoSuchMethodException(operationName);
        return MethodInvoker.invokeOperation(superClass, target, operationName, arguments);
    }

    private static boolean isCompatibleArguments(Class<?>[] parameterTypes, Object[] arguments) {
        if (parameterTypes.length != arguments.length)
            return false;
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i] == null) {
                if (parameterTypes[i].isPrimitive())
                    return false;
            } else if (!parameterTypes[i].isAssignableFrom(arguments[i].getClass()))
                return false;
        }
        return true;
    }
}
