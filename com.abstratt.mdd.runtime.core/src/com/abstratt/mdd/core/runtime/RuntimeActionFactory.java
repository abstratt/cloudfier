package com.abstratt.mdd.core.runtime;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.uml2.uml.Action;

public class RuntimeActionFactory {
	private static RuntimeActionFactory instance = new RuntimeActionFactory();

	public static RuntimeActionFactory getInstance() {
		return instance;
	}

	private RuntimeActionFactory() {
		// this is a singleton
	}

	@SuppressWarnings("unchecked")
	public RuntimeAction createRuntimeAction(Action descriptor, CompositeRuntimeAction parent) {
		Class<? extends RuntimeAction> runtimeActionClass;
		try {
			String runtimeActionClassName = getClassName(descriptor);
			runtimeActionClass = (Class<? extends RuntimeAction>) Class.forName(runtimeActionClassName);
			Constructor<? extends RuntimeAction> constructor = runtimeActionClass.getConstructor(Action.class, CompositeRuntimeAction.class);
			return constructor.newInstance(descriptor, parent);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e.getTargetException());
		}
	}

	private String getClassName(Action action) {
		final String fullClassName = action.eClass().getInstanceClassName();
		final String simpleClassName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
		return RuntimeAction.class.getPackage().getName() + ".action.Runtime" + simpleClassName;
	}
}
