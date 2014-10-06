package com.abstratt.mdd.target.sql.mappers.select;

import com.abstratt.mdd.core.target.IActionMapper;
import com.abstratt.mdd.core.target.IMappingContext;
import com.abstratt.mdd.core.util.MDDExtensionUtils;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.LiteralString;
import org.eclipse.uml2.uml.ValueSpecification;
import org.eclipse.uml2.uml.ValueSpecificationAction;

public class ValueSpecificationActionMapping implements IActionMapper<ValueSpecificationAction> {

	public String map(ValueSpecificationAction action, IMappingContext context) {
		ValueSpecification valueSpec = action.getValue();
        if (MDDExtensionUtils.isBasicValue(valueSpec)) {
        	Object basicValue = MDDExtensionUtils.getBasicValue(valueSpec);
			return basicValue instanceof String ? ('\'' + basicValue.toString() + '\'') : (basicValue + "");
        }
		final String stringValue = valueSpec.stringValue();
		return valueSpec instanceof LiteralString ? ('\'' + stringValue + '\'') : stringValue;
	}
}
