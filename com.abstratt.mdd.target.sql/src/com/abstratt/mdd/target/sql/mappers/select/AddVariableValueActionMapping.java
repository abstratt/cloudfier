package com.abstratt.mdd.target.sql.mappers.select;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.AddVariableValueAction;

import com.abstratt.mdd.core.target.IActionMapper;
import com.abstratt.mdd.core.target.IMappingContext;
import com.abstratt.mdd.core.util.ActivityUtils;

public class AddVariableValueActionMapping implements IActionMapper {
	public String map(Action action, IMappingContext context) {
		AddVariableValueAction avva = (AddVariableValueAction) action;
        StringBuffer result = new StringBuffer();
        // XXX HACK: we know return value is written to anonymous variable
        if (!"".equals(avva.getVariable().getName()))
        	throw new UnsupportedOperationException("The only variable that can be written to is the closure return value");
        Action  value = (Action) ActivityUtils.getSource(avva.getValue()).getOwner();
        result.append(context.map(value));
        return result.toString();
    }
}
