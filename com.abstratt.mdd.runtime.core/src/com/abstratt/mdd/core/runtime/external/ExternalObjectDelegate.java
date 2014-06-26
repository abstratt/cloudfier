package com.abstratt.mdd.core.runtime.external;

import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.Signal;

import com.abstratt.mdd.core.runtime.types.BasicType;

public interface ExternalObjectDelegate {
    BasicType getData(Classifier classifier, Operation operation, Object... arguments);

    void receiveSignal(Classifier classifier, Signal signal, Object... arguments);
}
