package com.abstratt.mdd.core.target.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.CallOperationAction;
import org.eclipse.uml2.uml.ObjectNode;
import org.eclipse.uml2.uml.Parameter;
import org.eclipse.uml2.uml.Variable;

import com.abstratt.mdd.core.target.IMappingContext;
import com.abstratt.mdd.core.util.ActivityUtils;

public class ActionMappingUtils {
    public static String generateName(Map<Variable, String> allocation, Variable variable, String suggestion) {
        String found = allocation.get(variable);
        if (found != null)
            return found;
        Collection<String> existing = allocation.values();
        String initial = suggestion == null ? variable.getName() : suggestion;
        String selected = initial;
        for (int i = 1; existing.contains(selected); i++)
            selected = initial + i;
        return ActionMappingUtils.recordName(allocation, variable, selected);
    }

    /**
     * Determines the name to use in the target language for the variable
     * corresponding to the (first) parameter taking into account the current
     * allocation.
     * 
     * @param allocation
     * @param collectionCallOperationAction
     * @return
     */
    public static List<String> getParameterVariables(Map<Variable, String> allocation, CallOperationAction collectionCallOperationAction) {
        Assert.isLegal(collectionCallOperationAction.getTarget().isMultivalued(), "Not a collection operation");
        Action targetAction = ActivityUtils.getSourceAction(collectionCallOperationAction.getTarget());
        // we require the closure to be the first parameter
        Activity closure = ActivityUtils.getSourceClosure(collectionCallOperationAction.getArguments().get(0));
        List<Parameter> closureParameters = ActivityUtils.getClosureInputParameters(closure);
        List<String> variableNames = new ArrayList<String>();
        for (int i = 0; i < closureParameters.size(); i++) {
            Variable inputParameterVariable = ActivityUtils.getBodyNode(closure).getVariable(closureParameters.get(i).getName(), null);
            // if applied to the result of another collection operation, use the
            // name produced by that operation
            if (i == 0 && targetAction instanceof CallOperationAction && ((CallOperationAction) targetAction).getTarget().isMultivalued()) {
                variableNames.add(ActionMappingUtils.recordName(allocation, inputParameterVariable,
                        ActionMappingUtils.getResultVariable(allocation, (CallOperationAction) targetAction)));
            } else {
                // Define the variable name based on the closure parameter name
                // Assume the first parameter is the single closure
                variableNames.add(ActionMappingUtils.generateName(allocation, inputParameterVariable, null));
            }
        }
        return variableNames;
    }

    public static String getResultVariable(Map<Variable, String> allocation, CallOperationAction collectionCallOperationAction) {
        // this possibly needs to also address other collection operations that
        // need to store temporary variables
        Activity closure = ActivityUtils.getSourceClosure(collectionCallOperationAction.getArguments().get(0));
        Variable resultVariable = ActivityUtils.getBodyNode(closure).getVariable("", null);
        String operationName = collectionCallOperationAction.getOperation().getName();
        if ("collect".equals(operationName))
            return ActionMappingUtils.generateName(allocation, resultVariable, "mapped");
        if ("reduce".equals(operationName))
            return ActionMappingUtils.recordName(allocation, resultVariable,
                    ActionMappingUtils.getParameterVariables(allocation, collectionCallOperationAction).get(1));
        return ActionMappingUtils.recordName(allocation, resultVariable,
                ActionMappingUtils.getParameterVariables(allocation, collectionCallOperationAction).get(0));
    }

    public static String mapSourceAction(ObjectNode targetPin, IMappingContext context) {
        return context.map(ActivityUtils.getSourceAction(targetPin));
    }

    private static String recordName(Map<Variable, String> allocation, Variable variable, String selected) {
        allocation.put(variable, selected);
        return selected;
    }
}
