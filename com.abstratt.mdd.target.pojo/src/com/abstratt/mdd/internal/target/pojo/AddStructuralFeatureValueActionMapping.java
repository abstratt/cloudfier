package com.abstratt.mdd.internal.target.pojo;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.AddStructuralFeatureValueAction;

import com.abstratt.mdd.core.target.IActionMapper;
import com.abstratt.mdd.core.target.IMappingContext;
import com.abstratt.mdd.core.util.ActivityUtils;

public class AddStructuralFeatureValueActionMapping implements IActionMapper<AddStructuralFeatureValueAction> {
	public String map(AddStructuralFeatureValueAction asfva, IMappingContext context) {
        StringBuffer result = new StringBuffer();
        Action  target = (Action) ActivityUtils.getSource(asfva.getObject()).getOwner();
        result.append(context.map(target));
        result.append(".");
        result.append(asfva.getStructuralFeature().getName());
        result.append(" = ");
        Action  value = (Action) ActivityUtils.getSource(asfva.getValue()).getOwner();
        result.append(context.map(value));
        return result.toString();
    }
}
