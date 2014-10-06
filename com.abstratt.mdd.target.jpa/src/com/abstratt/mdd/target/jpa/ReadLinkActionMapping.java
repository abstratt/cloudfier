package com.abstratt.mdd.target.jpa;

import org.eclipse.uml2.uml.Association;
import org.eclipse.uml2.uml.LinkEndData;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.ReadLinkAction;

import com.abstratt.mdd.core.target.IActionMapper;
import com.abstratt.mdd.core.target.IMappingContext;
import com.abstratt.mdd.core.target.spi.ActionMappingUtils;

public class ReadLinkActionMapping implements IActionMapper<ReadLinkAction> {

	public String map(ReadLinkAction action, IMappingContext context) {
		Association association = action.association();
		// XXX we only support binary associations at this time
		assert association.isBinary();
		LinkEndData fedEndData = action.getEndData().get(0);
		Property fedEnd = fedEndData.getEnd();
		Property openEnd = fedEnd.getOtherEnd();
		StringBuffer result = new StringBuffer();
        result.append(ActionMappingUtils.mapSourceAction(fedEndData.getValue(), context));
        result.append(".");
        result.append(openEnd.getName());
        return result.toString();
	}

}
