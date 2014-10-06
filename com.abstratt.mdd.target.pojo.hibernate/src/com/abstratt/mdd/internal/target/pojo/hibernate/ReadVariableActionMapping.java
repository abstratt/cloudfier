package com.abstratt.mdd.internal.target.pojo.hibernate;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ReadVariableAction;
import org.eclipse.uml2.uml.Variable;

import com.abstratt.mdd.core.target.IActionMapper;
import com.abstratt.mdd.core.target.IMappingContext;

public class ReadVariableActionMapping implements IActionMapper {
	public String map(Action action, IMappingContext context) {
		final Variable variable = ((ReadVariableAction) action).getVariable();
		if (variable.getOwner().getOwner() instanceof Activity) {
			// external scope local var
			return variable.getName();
		} else
			// no output for reading the implicit object
			return "";
	}

}
