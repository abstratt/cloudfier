package com.abstratt.mdd.internal.target.pojo.hibernate;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.ReadStructuralFeatureAction;

import com.abstratt.mdd.core.target.IActionMapper;
import com.abstratt.mdd.core.target.IMappingContext;

public class ReadStructuralFeatureActionMapping implements IActionMapper {

	public String map(Action action, IMappingContext context) {
		return "\"" + ((ReadStructuralFeatureAction) action).getStructuralFeature().getName() + "\"";
	}

}
