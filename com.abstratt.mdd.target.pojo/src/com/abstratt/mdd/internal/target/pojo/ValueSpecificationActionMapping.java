package com.abstratt.mdd.internal.target.pojo;

import org.eclipse.uml2.uml.ValueSpecificationAction;

import com.abstratt.mdd.core.target.IActionMapper;
import com.abstratt.mdd.core.target.IMappingContext;
import com.abstratt.mdd.target.pojo.POJOMappingUtils;

public class ValueSpecificationActionMapping implements IActionMapper<ValueSpecificationAction> {

	public String map(ValueSpecificationAction vsam, IMappingContext context) {
        return POJOMappingUtils.mapValue(vsam.getValue());
	}

}
