package com.abstratt.mdd.target.query;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.AddVariableValueAction;
import org.eclipse.uml2.uml.CallOperationAction;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.ReadExtentAction;
import org.eclipse.uml2.uml.StructuredActivityNode;
import org.eclipse.uml2.uml.Variable;

import com.abstratt.mdd.core.util.ActivityUtils;
import com.abstratt.mdd.core.util.MDDUtil;

public class QueryCore {

    /**
     * Returns a query that represents the flow starting from the given action.
     *
     * @param nextAction
     *            the action to start from
     * @return the query built
     */
    public Query buildQuery(Action nextAction) {
        Query query = QueryFactory.eINSTANCE.createQuery();
        do {
            if (ActivityUtils.isCast(nextAction)) {
                // this action is a no-op in SQL
                nextAction = ActivityUtils.getSourceAction(nextAction);
            } else if (nextAction instanceof ReadExtentAction) {
                ReadExtentAction readExtent = (ReadExtentAction) nextAction;
                query.setSourceType(readExtent.getClassifier());
                // no input pin
                break;
            } else if (nextAction instanceof CallOperationAction) {
                CallOperationAction callOperation = (CallOperationAction) nextAction;
                Operation operation = callOperation.getOperation();
                if (operation.getQualifiedName().endsWith("::Collection::select")) {
                    Activity filterClosure = ActivityUtils.getSourceClosure(callOperation.getArguments().get(0));
                    query.getFilters().add(filterClosure);
                } else if (operation.getQualifiedName().endsWith("::Collection::collect")) {
                    Activity mappingClosure = ActivityUtils.getSourceClosure(callOperation.getArguments().get(0));
                    List<Join> found = buildMappings(mappingClosure);
                    for (Join current : found)
                        query.getJoins().add(current);
                } else if (operation.getQualifiedName().endsWith("::Collection::any")) {
                    Activity filterClosure = ActivityUtils.getSourceClosure(callOperation.getArguments().get(0));
                    query.getFilters().add(filterClosure);
                } else
                    throw new UnsupportedOperationException("Unexpected operation invocation: " + operation.getQualifiedName());
                nextAction = ActivityUtils.getSourceAction(callOperation.getTarget());
            } else
                throw new UnsupportedOperationException("Unexpected: " + nextAction);
        } while (nextAction != null);
        return query;
    }

    /**
     * Returns an existing mapping that has the given classifier as the source
     */
    public Join findMapping(List<Join> mappings, Classifier source) {
        for (Join mapping : mappings)
            if (mapping.getSource().getType() == source)
                return mapping;
        return null;
    }

    public Query transformActivityToQuery(Activity behavior) {
        StructuredActivityNode body = ActivityUtils.getBodyNode(behavior);
        Variable returnValue = body.getVariable("", null);
        if (returnValue == null)
            //
            throw new UnsupportedOperationException();
        StructuredActivityNode block = (StructuredActivityNode) body.getNodes().get(0);
        AddVariableValueAction returnStatement = findReturnAction(block, returnValue);
        return buildQuery(ActivityUtils.getSourceAction(returnStatement));
    }

    private List<Join> buildMappings(final Activity mappingClosure) {
        final List<Join> mappings = new ArrayList<Join>();
        StructuredActivityNode rootNode = ActivityUtils.getBodyNode(mappingClosure);
        MDDUtil.visitTree(rootNode, new MDDUtil.IActivityNodeTreeVisitor() {
        });
        return mappings;
    }

    private AddVariableValueAction findReturnAction(StructuredActivityNode block, Variable returnValue) {
        AddVariableValueAction returnStatement = null;
        for (Action subAction : ActivityUtils.findStatements(block)) {
            if (ActivityUtils.isTerminal(subAction)) {
                if (returnStatement != null)
                    throw new UnsupportedOperationException("No support for multiple terminal actions");
                if (!(subAction instanceof AddVariableValueAction))
                    throw new UnsupportedOperationException("Unexpected terminal action: " + subAction
                            + ", expected AddVariableValueAction on method return value");
                returnStatement = (AddVariableValueAction) subAction;
                if (returnStatement.getVariable() != returnValue)
                    throw new UnsupportedOperationException("No support for modifying local variables");
            }
        }
        return returnStatement;
    }
}
