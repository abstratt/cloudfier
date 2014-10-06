package com.abstratt.mdd.internal.target.pojo.hibernate;

import org.eclipse.uml2.uml.Action;

import com.abstratt.mdd.core.target.IActionMapper;
import com.abstratt.mdd.core.target.IMappingContext;

public class CriteriaMapping implements IActionMapper {
	private static CriteriaMapping instance = new CriteriaMapping();

	public static CriteriaMapping getInstance() {
		return instance;
	}

	private CriteriaMapping() {
	}

	private IActionMapper getMapping(Action action) {
		// class name is <this package>.<action class name>Mapping
		String actionName = action.eClass().getName();
		String className = getClass().getPackage().getName() + '.' + actionName + "Mapping";
		try {
			Class mappingClass = Class.forName(className);
			return (IActionMapper) mappingClass.newInstance();
		} catch (ClassNotFoundException e) {
			// mapping not defined
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String map(Action target, IMappingContext context) {
		IActionMapper mapping = getMapping(target);
		return (mapping != null) ? mapping.map(target, null) : "//FIXME: mapping for " + target.eClass().getName() + " not implemented";
	}

}
