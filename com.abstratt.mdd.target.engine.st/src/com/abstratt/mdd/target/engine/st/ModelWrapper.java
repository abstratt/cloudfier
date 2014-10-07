/**
 * Copyright 2010 Rafael Chaves (Abstratt Technologies - http://abstratt.com)
 */
package com.abstratt.mdd.target.engine.st;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

/**
 * A wrapper that allows decorating a model object with additional properties.
 * It exposes a target object as a map that works as expected by StringTemplate.
 * Even though this class has no dependency on ST code, it is implementation is tailored
 * for StringTemplate.
 */
public class ModelWrapper implements Map<String, Object> {

	/**
	 * Custom property handler.
	 */
	public interface PropertyHandler<T> {
		public Object getProperty(T target, String propertyName);
	}

	private Object target;
	private Map<Class<?>, PropertyHandler<?>> handlers;

	private ModelWrapper(Object target,
			Map<Class<?>, PropertyHandler<?>> handlers) {
		this.target = target;
		this.handlers = handlers;
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsValue(Object arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<java.util.Map.Entry<String, Object>> entrySet() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object get(Object arg) {
		String key = arg.toString();
		Object resolvedProperty = resolveProperty(key);
		if (resolvedProperty != null)
			return wrapModelObject(resolvedProperty, handlers);
		Method method = findMethod(key);
		if (method != null)
			return wrapModelObject(invokeMethod(method), handlers);
		return null;
	}

	private Object resolveProperty(String key) {
		for (Entry<Class<?>, PropertyHandler<?>> entry : handlers.entrySet()) {
			if (!entry.getKey().isAssignableFrom(target.getClass()))
				continue;
			PropertyHandler<Object> value = (PropertyHandler<Object>) entry
					.getValue();
			Object propertyValue = value.getProperty(target, key);
			if (propertyValue != null)
				return propertyValue;
		}
		return null;
	}

	/**
	 * Wraps the given object into a dynamic map form, backed by the target object.
	 * A null reference is accepted, but not wrapped.
	 * Collections are handled properly, but instances of other Java classes are not handled at all.
	 *  
	 * @param toWrap object to wrap
	 * @param handlers custom property handlers
	 * @return the wrapped object
	 */
	public static Object wrapModelObject(Object toWrap,
			Map<Class<?>, PropertyHandler<?>> handlers) {
		if (toWrap == null)
			return null;
		if (toWrap instanceof Collection<?>) {
			Collection<?> resultAsCollection = (Collection<?>) toWrap;
			List<Object> actualResult = new ArrayList<Object>(
					resultAsCollection.size());
			for (Object object : resultAsCollection)
				actualResult.add(wrapModelObject(object, handlers));
			return actualResult;
		}
		if (toWrap instanceof Map<?,?>) {
			Map<?,?> resultAsCollection = (Map<?,?>) toWrap;
			Map<Object, Object> actualResult = new LinkedHashMap<Object, Object>(
					resultAsCollection.size());
			for (Map.Entry entry : resultAsCollection.entrySet())
				actualResult.put(entry.getKey(), wrapModelObject(entry.getValue(), handlers));
			return actualResult;
		}
		if (toWrap.getClass().isArray())
			return wrapModelObject(Arrays.asList((Object[]) toWrap), handlers);
		if (toWrap.getClass().getName().startsWith("java"))
			return toWrap;
		return new ModelWrapper(toWrap, handlers);
	}

	/**
	 * Invokes the given method. Wraps invocation exceptions into runtime exceptions.
	 *   
	 * @param toInvoke method to invoke
	 * @return the result of the invocation
	 */
	private Object invokeMethod(Method toInvoke) {
		try {
			return toInvoke.invoke(target);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e.getTargetException());
		}
	}

	@Override
	public boolean containsKey(Object arg) {
		String key = arg.toString();
		return findMethod(key) != null || resolveProperty(key) != null;
	}

	/**
	 * Finds an accessor for the given property.
	 * 
	 * @param propertyName
	 *            name of the property
	 * @return the property found
	 */
	private Method findMethod(String propertyName) {
		try {
			return target.getClass().getMethod(
					"get" + StringUtils.capitalize(propertyName));
		} catch (NoSuchMethodException e) {
			//
		}
		try {
			Method booleanAccessor = target.getClass().getMethod(
					"is" + StringUtils.capitalize(propertyName));
			Class<?> returnType = booleanAccessor.getReturnType();
			if (returnType == boolean.class || returnType == Boolean.class)
				return booleanAccessor;
		} catch (NoSuchMethodException e) {
			//
		}
		return null;
	}

	@Override
	public boolean isEmpty() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<String> keySet() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object put(String arg0, Object arg1) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object remove(Object arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		// a non empty map is deemed as true (and so is any object)
		return 1;
	}

	@Override
	public Collection<Object> values() {
		return new AbstractCollection<Object>() {
			@Override
			public Iterator<Object> iterator() {
				// this prevents ST from trying to convert this to an iterator
				// of entries
				return null;
			}

			@Override
			public int size() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public String toString() {
		return target.toString();
	}
}