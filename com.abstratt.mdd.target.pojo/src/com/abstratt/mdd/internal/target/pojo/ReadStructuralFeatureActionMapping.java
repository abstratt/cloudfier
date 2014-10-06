package com.abstratt.mdd.internal.target.pojo;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.ReadStructuralFeatureAction;

import com.abstratt.mdd.core.target.IActionMapper;
import com.abstratt.mdd.core.target.IMappingContext;
import com.abstratt.mdd.core.util.ActivityUtils;
import com.abstratt.mdd.target.pojo.POJOMapper;

public class ReadStructuralFeatureActionMapping implements IActionMapper<ReadStructuralFeatureAction> {

	public String map(ReadStructuralFeatureAction rsfa, IMappingContext context) {
		StringBuffer result = new StringBuffer();
		if (rsfa.getStructuralFeature().isStatic()) {
			final Classifier owner = (Classifier) rsfa.getStructuralFeature().getOwner();
			result.append(((POJOMapper) context.getLanguageMapper()).mapTypeReference(owner));
		} else {
			final Action target = (Action) ActivityUtils.getSource(rsfa.getObject()).getOwner();
			result.append(context.map(target));
		}
		result.append(".");
		result.append(rsfa.getStructuralFeature().getName());
		return result.toString();
	}

}
