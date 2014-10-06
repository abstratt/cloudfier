package com.abstratt.mdd.internal.target.pojo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.CallOperationAction;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.InputPin;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.ObjectNode;
import org.eclipse.uml2.uml.Operation;

import com.abstratt.mdd.core.target.IActionMapper;
import com.abstratt.mdd.core.target.IMappingContext;
import com.abstratt.mdd.core.util.ActivityUtils;
import com.abstratt.mdd.core.util.StereotypeUtils;
import com.abstratt.mdd.target.pojo.ExternalTypeMappingManager;
import com.abstratt.mdd.target.pojo.IExternalTypeMapper;
import com.abstratt.mdd.target.pojo.POJOMapper;

public class CallOperationActionMapping implements IActionMapper<CallOperationAction> {
	
	private static final Map<String, String> FUNCTION_MAPPING = asMap(new String[][] { {"add", "+"}, {"subtract", "-"}, {"multiply", "*"}, {"divide", "/"}, {"minus", "-"}, {"and", "&&"}, {"or", "||"}, {"not", "!"}, {"lowerThan", "<"}, {"greaterThan", ">"}, {"lowerOrEquals", "<="}, {"greaterOrEquals", ">="}, { /*TODO*/"equals", "=="}, {"same", "=="}});
	
	private static Map<String, String> asMap(String[][] pairs) {
		Map<String, String> result = new HashMap<String, String>(pairs.length);
		for (int i = 0; i < pairs.length; i++)
			result.put(pairs[i][0], pairs[i][1]);
		return result;
	}

	private String getJavaOperator(String functionName) {
		String javaOp = FUNCTION_MAPPING.get(functionName);
		if (javaOp == null)
			return null;
		return javaOp;
	}
	
	public String map(CallOperationAction cao, IMappingContext context) {
		final Operation operation = cao.getOperation();
		String javaOperator = getJavaOperator(operation.getName());
		if (javaOperator != null)
			return generateJavaOperatorExpression(cao, context);
		final NamedElement targetType = (Classifier) operation.getOwner();
		if (StereotypeUtils.hasStereotype(targetType, POJOMapper.EXTERNAL_STEREOTYPE)) {
			final String targetTypeName = targetType.getQualifiedName();
			IExternalTypeMapper mapper = ExternalTypeMappingManager.getInstance().getMapper(targetTypeName);
			if (mapper == null)
				return "//FIXME: could not find mapper for external type " + targetTypeName;
			return mapper.mapOperationCall(cao, context);
		}
		StringBuffer result = new StringBuffer();

		boolean classOperation = cao.getTarget() == null;
		if (classOperation) {
			result.append(((POJOMapper) context.getLanguageMapper()).mapTypeReference(operation.getClass_()));
		} else {
			Action target = (Action) ActivityUtils.getSource(cao.getTarget()).getOwner();
			result.append(context.map(target));
		}
		result.append(".");
		result.append(operation.getName());
		result.append("(");
		List<InputPin> arguments = cao.getArguments();
		if (!arguments.isEmpty()) {
			for (Object current : arguments) {
				Action argument = (Action) ActivityUtils.getSource(((InputPin) current)).getOwner();
				result.append(context.map(argument));
				result.append(',');
			}
			result.deleteCharAt(result.length() - 1);
		}
		result.append(")");
		return result.toString();
	}

	private String generateJavaOperatorExpression(CallOperationAction cao, IMappingContext context) {
		String javaOperator = getJavaOperator(cao.getOperation().getName());
		List<InputPin> arguments = cao.getArguments();
		StringBuffer result = new StringBuffer();
		if (arguments.size() == 1) {
			result.append(mapSourceAction(cao.getTarget(), context));
			result.append(" ");
			result.append(javaOperator);
			result.append(" ");
			result.append(mapSourceAction(arguments.get(0), context));
			return result.toString();
		} else {
			result.append(javaOperator);
			result.append("(");
			result.append(mapSourceAction(cao.getTarget(), context));
			result.append(")");
		}
		return result.toString();
	}

	private String mapSourceAction(ObjectNode targetPin, IMappingContext context) {
		return context.map(ActivityUtils.getSourceAction(targetPin));
	}
}
