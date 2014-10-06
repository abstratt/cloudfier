package com.abstratt.mdd.target.pojo;

import org.eclipse.uml2.uml.CallOperationAction;
import org.eclipse.uml2.uml.Type;

import com.abstratt.mdd.core.target.IMappingContext;

public interface IExternalTypeMapper {
	public String mapOperationCall(CallOperationAction action, IMappingContext context);
	public String mapTypeReference(Type externalType);
}
