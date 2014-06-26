package com.abstratt.mdd.core.runtime;

import org.eclipse.uml2.uml.OutputPin;

import com.abstratt.mdd.core.runtime.types.BasicType;

/**
 * Relevant sections of the specification:
 * <p>
 * &quot;<strong>11.3.27 OutputPin (from BasicActions)</strong> - An action
 * cannot terminate itself if an output pin has fewer values than the lower
 * multiplicity. An action may not put more values in an output pin in a single
 * execution than the upper multiplicity of the pin.&quot;
 * </p>
 */
public class RuntimeOutputPin extends RuntimePin {

    public RuntimeOutputPin(RuntimeAction action, OutputPin instance) {
        super(action, instance);
    }

    @Override
    public void addValue(BasicType newValue) {
        if (isFull())
            throw new IllegalStateException("full");
        assert getAction().peekState() == RuntimeActionState.EXECUTING;
        super.addValue(newValue);
    }

    @Override
    public boolean isInput() {
        return false;
    }
}
