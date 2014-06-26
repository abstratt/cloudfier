//package com.abstratt.mdd.core.runtime.external;
//
//import java.lang.reflect.Constructor;
//import java.lang.reflect.InvocationTargetException;
//import java.lang.reflect.Method;
//
//import org.eclipse.uml2.uml.Classifier;
//import org.eclipse.uml2.uml.NamedElement;
//
//import com.abstratt.mdd.core.IRepository;
//import com.abstratt.mdd.core.runtime.AbstractRuntimeClass;
//import com.abstratt.mdd.core.runtime.ExecutionContext;
//import com.abstratt.mdd.core.runtime.RuntimeObject;
//import com.abstratt.mdd.core.runtime.types.BasicType;
//import com.abstratt.mdd.core.util.MDDExtensionUtils;
//
//// TODO error handling
//public class ExternalClass extends AbstractRuntimeClass {
//
//	private Classifier classifier;
//	private ExternalClassObject classObject;
//
//	@SuppressWarnings("unchecked")
//	private static java.lang.Class<? extends ExternalClass> getExternalClass(Classifier classifier, IRepository repository) {
//		if (!MDDExtensionUtils.isExternal(classifier))
//			return null;
//		String className = MDDExtensionUtils.getExternalClassName(classifier);
//		if (className == null) {
//			// no class name, assume it is under the external package
//			String externalPackageName = ExternalClass.class.getPackage().getName();
//			className = externalPackageName + '.' + classifier.getQualifiedName().replaceAll(NamedElement.SEPARATOR, ".");
//		}
//		try {
//			return (Class<? extends ExternalClass>) Class.forName(className);
//		} catch (ClassNotFoundException e) {
//			// not found
//			return null;
//		}
//	}
//
//	public static boolean isExternal(Classifier classifier, IRepository repository) {
//		return getExternalClass(classifier, repository) != null;
//	}
//
//	public static ExternalClass loadExternalClass(Classifier classifier, IRepository repository) {
//		try {
//			final Class<? extends ExternalClass> externalClass = getExternalClass(classifier, repository);
//			if (externalClass != null) {
//				Constructor<? extends ExternalClass> constructor = externalClass.getDeclaredConstructor(Classifier.class);
//				return constructor.newInstance(classifier);
//			}
//		} catch (InstantiationException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IllegalAccessException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (NoSuchMethodException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (InvocationTargetException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		return null;
//	}
//
//	@SuppressWarnings("unused")
//	public ExternalClass(Classifier classifier) {
//		classObject = new ExternalClassObject(this, null);
//		this.classifier = classifier;
//	}
//
//	public Classifier getClassifier() {
//		return classifier;
//	}
//
//	/*
//	 * (non-Javadoc)
//	 * @see com.abstratt.mdd.core.runtime.AbstractRuntimeClass#getClassObject()
//	 */
//	@Override
//	public BasicType getClassObject() {
//		return classObject;
//	}
//
//	/*
//	 * (non-Javadoc)
//	 * @see com.abstratt.mdd.core.runtime.AbstractRuntimeClass#newInstance()
//	 */
//	@Override
//	public BasicType newInstance() {
//		return new ExternalClassObject(this, this);
//	}
//
//	public Method selectMethod(Object delegate, String operationName, Class[] classes) throws NoSuchMethodException {
//		return delegate.getClass().getMethod(operationName, new Class[] {ExecutionContext.class, RuntimeObject.class, Object[].class});
//	}
// }
