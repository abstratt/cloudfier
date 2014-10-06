package com.abstratt.mdd.internal.target.pojo.hibernate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.InputPin;

import com.abstratt.mdd.core.target.IActionMapper;
import com.abstratt.mdd.core.target.IMappingContext;
import com.abstratt.mdd.core.util.ActivityUtils;
import com.abstratt.mdd.target.pojo.POJOMapper;

public class CallBehaviorActionMapping implements IActionMapper {

	// this has to be kept in sync with the functions supported by the
	// ApplyFunction class
	// TODO equals can be == or equals, depending on the type of the operand -
	// should have two different functions (e.g. same/equals)
	private static final Map ARITHMETIC_MAPPING = asMap(new String[][] { {"add", "+"}, {"subtract", "-"}, {"multiply", "*"}, {"divide", "/"}, {"minus", "-"}});
	private static final Map FUNCTION_MAPPING = asMap(new String[][] { {"and", "eq"}, {"or", "or"}, {"not", "not"}, {"lowerThan", "lt"}, {"greaterThan", "gt"}, {"lowerOrEquals", "le"}, {"greaterOrEquals", "ge"}, {"equals", "eq"}, {"same", "idEq"}});

	private static Map<String, String> asMap(String[][] pairs) {
		Map<String, String> result = new HashMap<String, String>(pairs.length);
		for (int i = 0; i < pairs.length; i++)
			result.put(pairs[i][0], pairs[i][1]);
		return result;
	}

	public String map(Action action, IMappingContext context) {
		CallBehaviorAction cba = (CallBehaviorAction) action;
		String behaviorName = cba.getBehavior().getName();
		if (behaviorName.endsWith("_operator"))
			behaviorName = behaviorName.substring(0, behaviorName.length() - "_operator".length());

		StringBuffer result = new StringBuffer();
		List arguments = cba.getArguments();
		if (ARITHMETIC_MAPPING.get(behaviorName) != null) {
			int opIndex = 0;
			if (arguments.size() == 2) {
				result.append(CriteriaMapping.getInstance().map((Action) ActivityUtils.getSource(((InputPin) arguments.get(opIndex++))).getOwner(), context));
				result.append(" ");
			}
			//TODO only for primitive functions
			result.append(ARITHMETIC_MAPPING.get(behaviorName));
			result.append(" ");
			result.append(POJOMapper.getInstance().map((Action) ActivityUtils.getSource(((InputPin) arguments.get(opIndex))).getOwner(), context));
		} else if (FUNCTION_MAPPING.get(behaviorName) != null) {
			result.append(".add(");
			result.append("org.hibernate.criterion.Restrictions.");
			result.append(FUNCTION_MAPPING.get(behaviorName));
			result.append("(");
			int opIndex = 0;
			result.append(CriteriaMapping.getInstance().map((Action) ActivityUtils.getSource(((InputPin) arguments.get(opIndex++))).getOwner(), null));
			if (arguments.size() == 2) {
				result.append(", ");
				result.append(CriteriaMapping.getInstance().map((Action) ActivityUtils.getSource(((InputPin) arguments.get(opIndex++))).getOwner(), null));
			}
			result.append("))");
		} else
			result.append("//XXX: unexpected behavior name: " + behaviorName);
		return result.toString();
	}

}
