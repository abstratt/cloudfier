package com.abstratt.mdd.internal.target.pojo;

import java.util.Iterator;
import java.util.List;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.CallOperationAction;
import org.eclipse.uml2.uml.InputPin;
import org.eclipse.uml2.uml.Type;

import com.abstratt.mdd.core.target.IMappingContext;
import com.abstratt.mdd.core.util.ActivityUtils;
import com.abstratt.mdd.target.pojo.IExternalTypeMapper;

public class ConsoleMapper implements IExternalTypeMapper {

	public String mapOperationCall(CallOperationAction action, IMappingContext context) {
		String javaOpName = action.getOperation().getName().equals("write") ? "print" : "println";
		StringBuffer toPrint= new StringBuffer("System.out.").append(javaOpName).append("(");
		List<InputPin> argumentInputPins = action.getArguments();
		for (Iterator<InputPin> i = argumentInputPins.iterator(); i.hasNext();) {
			InputPin argument = i.next();
			Action sourceAction = ActivityUtils.getSourceAction(argument);
			toPrint.append(context.map(sourceAction, null));
		}
		toPrint.append(")");
		return toPrint.toString();
	}

	public String mapTypeReference(Type externalType) {
		return "//FIXME unexpected reference to the Console class";
	}
}
