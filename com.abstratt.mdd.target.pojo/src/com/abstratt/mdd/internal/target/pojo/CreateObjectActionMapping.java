package com.abstratt.mdd.internal.target.pojo;

import org.eclipse.uml2.uml.CreateObjectAction;

import com.abstratt.mdd.core.target.IActionMapper;
import com.abstratt.mdd.core.target.IMappingContext;
import com.abstratt.mdd.target.pojo.POJOMapper;


public class CreateObjectActionMapping implements IActionMapper<CreateObjectAction> {

	public String map(CreateObjectAction createObjectAction, IMappingContext context) {
        StringBuffer result = new StringBuffer();
        result.append("new ");
        result.append(((POJOMapper) context.getLanguageMapper()).mapTypeReference(createObjectAction.getClassifier()));
        result.append("(");
        // TODO generate arguments
        result.append(")");
        return result.toString();
    }

}
