package com.abstratt.mdd.target.jee

import org.eclipse.uml2.uml.Action
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.InputPin
import org.eclipse.uml2.uml.OutputPin
import org.eclipse.uml2.uml.ReadVariableAction
import org.eclipse.uml2.uml.ValueSpecificationAction

import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import org.eclipse.uml2.uml.Activity

class DataFlowAnalyzer {

	def OutputPin findSource(InputPin pin) {
		return findSource(pin.source as OutputPin)
	}
	
	def InputPin findTarget(OutputPin pin) {
		return findTarget(pin.target as InputPin)
	}
	
	def OutputPin findSource(OutputPin pin) {
		if (pin == null) {
			return null
		}
		val source = findSource(pin.owningAction, pin)
		return source
	}

	def InputPin findTarget(InputPin pin) {
		if (pin == null) {
			return null
		}
		val target = findTarget(pin.owningAction, pin)
		return target
	}

	private def OutputPin internalFindSource(InputPin pin) {
		if (pin == null) {
			return null
		}
		return findSource(pin.sourceAction, pin.source as OutputPin)
	}
	
	private def InputPin internalFindTarget(OutputPin pin) {
		if (pin == null) {
			return null
		}
		return findTarget(pin.targetAction, pin.target as InputPin)
	}
	

	private def dispatch OutputPin findSource(Action action, OutputPin pin) {
		return if(pin.owningAction == action) pin else null
	}
	
	private def InputPin findTarget(Action action, InputPin pin) {
		return if(pin.owningAction == action) pin else null
	}
	
	private def dispatch OutputPin findSource(ReadVariableAction action, OutputPin pin) {
		val parameter = action.variable.parameter
		if (parameter == null) {
			return null
		}
		if (!action.actionActivity.closure) {
			return null
		}
	    val isOperationParameter = (parameter.namespace as Activity).specification != null
	    if (isOperationParameter) {
	    	return null
	    }
		val closureSite = action.actionActivity.closureContext.findFirstMatchingAction( a |
			a instanceof ValueSpecificationAction && (a as ValueSpecificationAction).value.behaviorReference
		) as ValueSpecificationAction
		val closureConsumer = closureSite.result.target as InputPin
		if (!(closureConsumer.owningAction instanceof CallOperationAction)) {
			// we can only trace the source in operation calls on collections
			return null
		}
		val asCallOp = closureConsumer.owningAction as CallOperationAction
		return if(asCallOp.target.multivalued) asCallOp.target.internalFindSource else pin
	}

	private def dispatch OutputPin findSource(CallOperationAction action, OutputPin pin) {
		if (!action.results.contains(pin)) {
			return null
		}
		// we only care about collection operations
		if (!action.target.multivalued) {
			return null
		}
		val operationName = action.operation.name
		switch (operationName) {
			case 'select': return action.target.internalFindSource
			case 'any': return action.target.internalFindSource
			case 'one': return action.target.internalFindSource
		}
		return pin
	}
}