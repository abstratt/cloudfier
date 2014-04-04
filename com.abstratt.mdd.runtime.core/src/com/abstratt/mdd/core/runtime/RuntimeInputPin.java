package com.abstratt.mdd.core.runtime;

import org.eclipse.uml2.uml.InputPin;
import org.eclipse.uml2.uml.ValuePin;

/**
 * Relevant sections of the specification:
 * <p>
 * &quot;<strong>11.3.19 InputPin (from BasicActions)</strong> - An action cannot start execution if an input pin has fewer values than the 
 * lower multiplicity. The upper multiplicity determines how many values are 
 * consumed by a single execution of the action.&quot;
 * </p>
 */
public class RuntimeInputPin extends RuntimePin {

	public RuntimeInputPin(RuntimeAction action, InputPin instance) {
		super(action, instance);
		if (instance instanceof ValuePin)
			addValue(RuntimeUtils.extractValueFromSpecification(((ValuePin) instance).getValue()));
	}

	@Override
	public boolean isInput() {
		return true;
	}

}
