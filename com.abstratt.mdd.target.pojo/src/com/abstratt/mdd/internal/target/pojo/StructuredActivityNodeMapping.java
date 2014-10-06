package com.abstratt.mdd.internal.target.pojo;

import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.target.IActionMapper;
import com.abstratt.mdd.core.target.IMappingContext;
import com.abstratt.mdd.core.target.IMappingContext.Style;
import com.abstratt.mdd.core.util.ActivityUtils;
import com.abstratt.mdd.core.util.MDDUtil;
import com.abstratt.mdd.target.pojo.POJOMapper;
import java.util.Collection;
import java.util.List;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.StructuredActivityNode;
import org.eclipse.uml2.uml.Variable;

public class StructuredActivityNodeMapping implements IActionMapper<StructuredActivityNode> {

    protected static final String LINE_SEPARATOR = System.getProperty("line.separator");

    public String map(StructuredActivityNode structuredActionNode, IMappingContext context) {
        Collection<Action> subActions = MDDUtil.filterByClass(structuredActionNode.getNodes(), IRepository.PACKAGE.getAction());
        StringBuffer result = new StringBuffer();
        if (context.getCurrentStyle() == Style.STATEMENT) {
            result.append("{");
            result.append(LINE_SEPARATOR);
            List<Variable> variables = structuredActionNode.getVariables();
            for (Variable current : variables) {
                result.append(((POJOMapper) context.getLanguageMapper()).mapTypeReference(current.getType()));
                result.append(" ");
                result.append(current.getName());
                result.append(';');
                result.append(LINE_SEPARATOR);
			}
        }
        for (Action subAction : subActions) {
            if (!ActivityUtils.isTerminal(subAction))
                continue;
            result.append(context.map(subAction));
            if (context.getCurrentStyle() == Style.STATEMENT && !(subActions instanceof StructuredActivityNode)) {
                result.append(";");
                result.append(LINE_SEPARATOR);
            }
        }
        if (context.getCurrentStyle() == Style.STATEMENT) {
            result.append("}");
            result.append(LINE_SEPARATOR);
        }
        return result.toString();
    }

}
