package com.abstratt.mdd.target.jpa;

import org.eclipse.uml2.uml.ReadStructuralFeatureAction;
import org.eclipse.uml2.uml.StructuralFeature;

import com.abstratt.mdd.core.target.IActionMapper;
import com.abstratt.mdd.core.target.IMappingContext;
import com.abstratt.mdd.core.target.spi.ActionMappingUtils;

public class ReadStructuralFeatureActionMapping implements IActionMapper<ReadStructuralFeatureAction> {
	public String map(ReadStructuralFeatureAction action, IMappingContext context) {
		StringBuffer result = new StringBuffer();
		StructuralFeature structuralFeature = action.getStructuralFeature();
		result.append(ActionMappingUtils.mapSourceAction(action.getObject(), context));
		result.append(".");
		result.append(structuralFeature.getName());
		return result.toString();
	}
}
