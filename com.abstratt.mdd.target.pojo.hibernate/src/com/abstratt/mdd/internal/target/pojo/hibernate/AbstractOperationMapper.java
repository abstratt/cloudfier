package com.abstratt.mdd.internal.target.pojo.hibernate;

import java.util.Arrays;
import java.util.List;

import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.Parameter;
import org.eclipse.uml2.uml.ParameterDirectionKind;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.TypedElement;

import com.abstratt.mdd.core.target.ILanguageMapper;
import com.abstratt.mdd.core.util.StructuralFeatureUtils;
import com.abstratt.mdd.target.pojo.IOperationMapper;
import com.abstratt.mdd.target.pojo.POJOMapper;

public abstract class AbstractOperationMapper implements IOperationMapper {

	protected List<IOperationMapper> following;

	@SuppressWarnings("unchecked")
	public List<Parameter> getInputParameters(Operation operation) {
		return StructuralFeatureUtils.filterParameters(operation.getOwnedParameters(), ParameterDirectionKind.IN_LITERAL);
	}

	@SuppressWarnings("unchecked")
	public Parameter getReturnParameter(Operation operation) {
		List<Parameter> returnPar = StructuralFeatureUtils.filterParameters(operation.getOwnedParameters(), ParameterDirectionKind.RETURN_LITERAL);
		return returnPar.isEmpty() ? null : returnPar.get(0);
	}

	@Override
	public String mapBehavior(Operation operation, List<IOperationMapper> following) {
		return POJOMapper.getInstance().mapBehavior(operation, Arrays.asList(new IOperationMapper[0]));
	}

	public String mapTypedElementType(TypedElement element, boolean specific) {
		return POJOMapper.getInstance().mapTypedElementType(element, specific);
	}

	public String mapTypeReference(String qualifiedName) {
		return POJOMapper.getInstance().mapQualifiedName(qualifiedName);
	}

	public String mapTypeReference(Type type) {
		return POJOMapper.getInstance().mapTypeReference(type);
	}
}
