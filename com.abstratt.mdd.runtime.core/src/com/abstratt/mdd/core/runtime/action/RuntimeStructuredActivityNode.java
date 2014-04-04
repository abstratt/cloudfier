package com.abstratt.mdd.core.runtime.action;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.ExceptionHandler;
import org.eclipse.uml2.uml.ExecutableNode;
import org.eclipse.uml2.uml.InputPin;
import org.eclipse.uml2.uml.OutputPin;
import org.eclipse.uml2.uml.StructuredActivityNode;
import org.eclipse.uml2.uml.UMLPackage.Literals;
import org.eclipse.uml2.uml.Variable;

import com.abstratt.mdd.core.runtime.CompositeRuntimeAction;
import com.abstratt.mdd.core.runtime.ExecutionContext;
import com.abstratt.mdd.core.runtime.RuntimeAction;
import com.abstratt.mdd.core.runtime.RuntimeObjectNode;
import com.abstratt.mdd.core.runtime.RuntimeRaisedException;
import com.abstratt.mdd.core.util.ActivityUtils;
import com.abstratt.mdd.core.util.MDDUtil;

public class RuntimeStructuredActivityNode extends CompositeRuntimeAction {
	public RuntimeStructuredActivityNode(Action actionNode, CompositeRuntimeAction parent) {
		super(actionNode, parent);
	}

	public void executeBehavior(ExecutionContext context) {
		StructuredActivityNode instance = (StructuredActivityNode) this.getInstance();
		if (instance.getNodes().size() == 2) {
			List<InputPin> inputs = MDDUtil.filterByClass(instance.getNodes(), Literals.INPUT_PIN);
			List<OutputPin> outputs = MDDUtil.filterByClass(instance.getNodes(), Literals.OUTPUT_PIN);
			if (inputs.size() == 1 && outputs.size() == 1) {
				// fUML Beta A.4.13 - A type cast is mapped to a structured activity node that simply copies its input to its output
				RuntimeObjectNode source = this.getRuntimeObjectNode(inputs.get(0));
				RuntimeObjectNode destination = this.getRuntimeObjectNode(outputs.get(0));
				destination.setValue(source.getValue());
				return;
			}
		}
		Set<ExecutableNode> handlerBodies = new HashSet<ExecutableNode>();
		for (ExceptionHandler exceptionHandler : instance.getHandlers())
		    handlerBodies.add(exceptionHandler.getHandlerBody());
		context.newScope(instance);
		try {
			List<Variable> variables = instance.getVariables();
			for (Variable current : variables)
				context.declareVariable(current);
            // if the SAN has inputs, copy values to local vars (exception handler variables)
			for (InputPin inputPin : ActivityUtils.getActionInputs(instance)) {
                Variable inputVar = instance.getVariable(inputPin.getName(), inputPin.getType());
                if (inputVar != null)
                    context.setVariableValue(inputVar, getRuntimeObjectNode(inputPin).consumeValue());
            }
			List<ActivityNode> children = MDDUtil.filterByClass(instance.getNodes(), Literals.ACTION);
			int toExecute = children.size();
			do {
				for (ActivityNode each : children) {
					Action subAction = (Action) each;
					RuntimeAction runtimeAction = this.getRuntimeAction(subAction);
					// skip handler bodies from normal subaction execution
					if (!handlerBodies.contains(runtimeAction.getInstance()))
					    toExecute -= this.executeContainedAction(runtimeAction, context);
				}
			} while (toExecute > 0);
		} catch (RuntimeRaisedException rre) {
		    if (rre.getExceptionObject() != null) {
		        ExceptionHandler handler = ActivityUtils.findHandler(getInstance(), rre.getExceptionType(), false);
		        if (handler != null) {
		            Action handlerAction = (Action) handler.getHandlerBody();
		            RuntimeAction runtimeAction = this.getRuntimeAction(handlerAction);
		            runtimeAction.getInputs().get(0).setValue(rre.getExceptionObject());
		            this.executeContainedAction(runtimeAction, context);
		            return;
		        }
		    }
		    throw rre;
		} finally {
			context.dropScope();
		}
	}
}