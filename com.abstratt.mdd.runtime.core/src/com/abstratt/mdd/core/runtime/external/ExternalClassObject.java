//package com.abstratt.mdd.core.runtime.external;
//
//import com.abstratt.mdd.core.runtime.ExecutionContext;
//import com.abstratt.mdd.core.runtime.MetaClass;
//import com.abstratt.mdd.core.runtime.RuntimeObject;
//import com.abstratt.mdd.core.runtime.types.BasicType;
//import java.lang.reflect.InvocationTargetException;
//import java.lang.reflect.Method;
//import org.eclipse.uml2.uml.Operation;
//
//public class ExternalClassObject extends BasicType implements MetaClass<Object> {
//    private Object delegate;
//	private ExternalClass externalClass;
//
//	private static Object invokeMethod(ExecutionContext context, Object target, Method method, Object... arguments) {
//		try {
//			return method.invoke(target, new Object[] {context, arguments});
//		} catch (IllegalAccessException e) {
//			throw new RuntimeException(e.toString(), e);
//		} catch (InvocationTargetException e) {
//			throw new RuntimeException(e.toString(), e.getTargetException());
//		}
//	}
//
//	public ExternalClassObject(ExternalClass externalClass, Object delegate) {
//		this.externalClass = externalClass;
//		this.delegate = delegate;
//	}
//
//	@Override
//	public String getClassifierName() {
//		return this.externalClass.getClassifier().getQualifiedName();
//	}
//
//	public boolean isClassObject() {
//		return delegate == null;
//	}
//
//	public Object runClassOperation(ExecutionContext context, Operation operation, Object... arguments) {
//		try {
//			Method externalMethod = externalClass.getClass().getMethod(operation.getName(), new Class[] {ExecutionContext.class, Object[].class});
//			return invokeMethod(context, null, externalMethod, arguments);
//		} catch (NoSuchMethodException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		return null;
//	}
//	
//	public Object runOperation(ExecutionContext context, Object target, Operation operation, Object... arguments) {
//		try {
//			Method externalMethod = externalClass.selectMethod(delegate, operation.getName(), new Class[] {ExecutionContext.class, RuntimeObject.class, Object[].class});
//			return invokeMethod(context, delegate, externalMethod, arguments);
//		} catch (NoSuchMethodException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		return null;
//	}
//	
//	@Override
//	public Object runOperation(ExecutionContext context, Operation operation,
//	        Object... arguments) {
//	    return runOperation(context, delegate, operation, arguments);
//	}
//}
