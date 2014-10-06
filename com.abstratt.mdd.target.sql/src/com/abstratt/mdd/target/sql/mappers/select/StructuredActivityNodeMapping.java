package com.abstratt.mdd.target.sql.mappers.select;

import java.util.Collection;
import java.util.List;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.OutputPin;
import org.eclipse.uml2.uml.StructuredActivityNode;
import org.eclipse.uml2.uml.UMLPackage;

import com.abstratt.mdd.core.target.IActionMapper;
import com.abstratt.mdd.core.target.IMappingContext;
import com.abstratt.mdd.core.util.ActivityUtils;
import com.abstratt.mdd.core.util.MDDUtil;

public class StructuredActivityNodeMapping implements IActionMapper {

    protected static final String LINE_SEPARATOR = System.getProperty("line.separator");

    public String map(Action action, IMappingContext context) {
        StructuredActivityNode structuredActionNode = (StructuredActivityNode) action;
        Collection<Action> subActions = MDDUtil.filterByClass(structuredActionNode.getNodes(), UMLPackage.Literals.ACTION);
        StringBuffer result = new StringBuffer();
        for (Action subAction : subActions) {
            if (!ActivityUtils.isTerminal(subAction))
                continue;
            result.append(context.map(subAction));
        }
        return result.toString();
    }

}
