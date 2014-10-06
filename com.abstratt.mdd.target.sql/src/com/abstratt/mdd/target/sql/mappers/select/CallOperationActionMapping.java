package com.abstratt.mdd.target.sql.mappers.select;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.uml2.uml.CallOperationAction;
import org.eclipse.uml2.uml.InputPin;
import org.eclipse.uml2.uml.ObjectNode;

import com.abstratt.mdd.core.target.IActionMapper;
import com.abstratt.mdd.core.target.IMappingContext;
import com.abstratt.mdd.core.util.ActivityUtils;

public class CallOperationActionMapping implements IActionMapper<CallOperationAction> {

	private static final Map<String, String> OPERATOR_MAPPING = asMap(new String[][] {{"greaterOrEquals", ">="}, {"lowerThan", "<"}, {"equals", "="}, {"lowerOrEquals", "<="}, {"greaterThan", ">"}});

	private static Map<String, String> asMap(String[][] pairs) {
		Map<String, String> result = new HashMap<String, String>(pairs.length);
		for (int i = 0; i < pairs.length; i++)
			result.put(pairs[i][0], pairs[i][1]);
		return result;
	}

	private String getSqlOperator(String function) {
		String sqlOp = OPERATOR_MAPPING.get(function);
		if (sqlOp == null)
			sqlOp = function;
		return sqlOp;
	}

	public String map(CallOperationAction coa, IMappingContext context) {
		String sqlOperator = getSqlOperator(coa.getOperation().getName());
		List<InputPin> arguments = coa.getArguments();
		StringBuffer result = new StringBuffer();
		if (arguments.size() == 1) {
			result.append(mapSourceAction(coa.getTarget(), context));
			result.append(" ");
			result.append(sqlOperator);
			result.append(" ");
			result.append(mapSourceAction(arguments.get(0), context));
			return result.toString();
		} else {
			result.append(sqlOperator);
			result.append("(");
			result.append(mapSourceAction(coa.getTarget(), context));
			result.append(")");
		}
		return result.toString();
	}

	private String mapSourceAction(ObjectNode targetPin, IMappingContext context) {
		return context.map(ActivityUtils.getSourceAction(targetPin));
	}

}
