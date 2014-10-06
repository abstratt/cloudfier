package com.abstratt.mdd.target.sql.mappers.select;

import org.eclipse.uml2.uml.Action;

import com.abstratt.mdd.core.target.IActionMapper;
import com.abstratt.mdd.core.target.IMappingContext;

public class ReadVariableActionMapping implements IActionMapper {
	public String map(Action action, IMappingContext context) {
		StringBuffer result = new StringBuffer();
		result.append("?");
		return result.toString();
	}
}
