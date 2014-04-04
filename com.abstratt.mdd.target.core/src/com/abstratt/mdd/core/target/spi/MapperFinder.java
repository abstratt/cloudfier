package com.abstratt.mdd.core.target.spi;

import java.util.Arrays;
import java.util.List;

import org.eclipse.uml2.uml.Action;

import com.abstratt.mdd.core.target.IActionMapper;

public class MapperFinder {
	private List<Package> basePackages;
	private ClassLoader loader; 
	public MapperFinder(Class<?> clazz) {
		this.basePackages = Arrays.<Package>asList(clazz.getPackage());
		this.loader = clazz.getClassLoader(); 
	}

	public IActionMapper<?> getMapping(Action action) {
		// class name is <this package>.<action class name>Mapping
		String actionName = action.eClass().getName();
		Class<?> mappingClass = findMapping(actionName);
		if (mappingClass == null)
			// mapping not defined
			return null;
		try {
			return (IActionMapper<?>) mappingClass.newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}
	private Class<?> findMapping(String actionName) {
		for (Package basePackage : basePackages) {
			String className = basePackage.getName() + '.' + actionName + "Mapping";
			try {
				return Class.forName(className, true, loader);
			} catch (ClassNotFoundException e) {
				// mapping not defined
			} 	
		}
		return null;
	}
}
