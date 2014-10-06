package com.abstratt.mdd.target.sql.mappers.select;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.ReadStructuralFeatureAction;
import org.eclipse.uml2.uml.StructuralFeature;

import com.abstratt.mdd.core.target.IActionMapper;
import com.abstratt.mdd.core.target.IMappingContext;
import com.abstratt.mdd.target.sql.SQLMapper;

public class ReadStructuralFeatureActionMapping implements IActionMapper {
	public String map(Action action, IMappingContext context) {
		StringBuffer result = new StringBuffer();
		ReadStructuralFeatureAction rsfa = (ReadStructuralFeatureAction) action;
		StructuralFeature structuralFeature = rsfa.getStructuralFeature();
		result.append(SQLMapper.getAliasFor((Classifier) structuralFeature.getOwner()));
		result.append(".");
		result.append(structuralFeature.getName());
		return result.toString();
	}
}
