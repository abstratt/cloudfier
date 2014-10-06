package com.abstratt.mdd.internal.target.pojo;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.AddVariableValueAction;

import com.abstratt.mdd.core.target.IActionMapper;
import com.abstratt.mdd.core.target.IMappingContext;
import com.abstratt.mdd.core.target.IMappingContext.Style;
import com.abstratt.mdd.core.util.ActivityUtils;

public class AddVariableValueActionMapping implements IActionMapper<AddVariableValueAction> {
	public String map(AddVariableValueAction avva, IMappingContext context) {
        StringBuffer result = new StringBuffer();
        // HACK: return value is written to anonymous variable
        if ("".equals(avva.getVariable().getName())) {
            if (context.getCurrentStyle() == Style.STATEMENT)
            	result.append("return ");
        } else {
            result.append(avva.getVariable().getName());
            result.append(" = ");
        }
        Action  value = (Action) ActivityUtils.getSource(avva.getValue()).getOwner();
        result.append(context.map(value));
        return result.toString();

    }
}
