/**
 * 
 */
package com.abstratt.mdd.target.engine.st;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.antlr.stringtemplate.AttributeRenderer;
import org.apache.commons.lang.StringUtils;

public class StringRenderer implements AttributeRenderer {
	public String toString(Object o, String formatName) {
		try {
			Method formatMethod = StringUtils.class.getMethod(formatName, String.class);
			return (String) formatMethod.invoke(null, o.toString());
		} catch (NoSuchMethodException e) {
			return toString(o);
		} catch (IllegalAccessException e) {
			return toString(o);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			return toString(o);
		}
	}

	public String toString(Object o) {
		return o.toString();
	}
}