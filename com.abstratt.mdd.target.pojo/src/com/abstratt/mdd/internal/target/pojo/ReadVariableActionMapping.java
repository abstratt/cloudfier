package com.abstratt.mdd.internal.target.pojo;

import org.eclipse.uml2.uml.ReadVariableAction;

import com.abstratt.mdd.core.target.IActionMapper;
import com.abstratt.mdd.core.target.IMappingContext;

public class ReadVariableActionMapping implements IActionMapper<ReadVariableAction> {
	public String map(ReadVariableAction action, IMappingContext context) {
        return action.getVariable().getName();
    }

}
