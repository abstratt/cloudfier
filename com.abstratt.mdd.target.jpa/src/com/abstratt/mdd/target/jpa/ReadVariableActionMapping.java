package com.abstratt.mdd.target.jpa;

import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ReadVariableAction;
import org.eclipse.uml2.uml.Variable;

import com.abstratt.mdd.core.target.IActionMapper;
import com.abstratt.mdd.core.target.IMappingContext;
import com.abstratt.mdd.core.util.ActivityUtils;
import com.abstratt.mdd.core.util.MDDExtensionUtils;

public class ReadVariableActionMapping implements IActionMapper<ReadVariableAction> {
	public String map(ReadVariableAction action, IMappingContext context) {
		Variable variable = action.getVariable();
		/*
		 * Possible scenarios:
		 * 
		 * 1) we are reading a closure parameter
		 * 2) we are reading a query parameter inside or outside of a closure
		 * 3) we are reading a true local query variable inside or outside of a closure
		 * 4) we are reading a true local closure variable inside of a closure
		 * 
		 * 4) We will not support for now (or maybe never).
		 * 3) We may be able to get away with not supporting 3 for now.
		 * 2) 
		 */
		Activity scopeActivity = variable.getScope().getActivity();
		if (MDDExtensionUtils.isClosure(scopeActivity))
			// no output for reading the implicit object in the closure (could be the alias used in the query )
			return JPAMapper.getAliasFor(variable.getType());
		// not a closure var, probably a query parameter
		return ":" + variable.getName();
	}
}
