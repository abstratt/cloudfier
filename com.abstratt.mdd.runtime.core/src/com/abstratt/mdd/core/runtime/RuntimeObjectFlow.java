package com.abstratt.mdd.core.runtime;

import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.MultiplicityElement;

import com.abstratt.mdd.core.runtime.types.BasicType;
import com.abstratt.mdd.core.runtime.types.CollectionType;

public class RuntimeObjectFlow {
    private RuntimeObjectNode source;
    private RuntimeObjectNode target;

    public RuntimeObjectFlow(RuntimeObjectNode source, RuntimeObjectNode target) {
        super();
        this.source = source;
        this.source.addOutgoing(this);
        this.target = target;
        this.target.addIncoming(this);
    }

    public RuntimeObjectNode getSource() {
        return source;
    }

    public RuntimeObjectNode getTarget() {
        return target;
    }

    public void tryToTransfer() {
        BasicType consumed;
        try {
            consumed = source.consumeValue();
        } catch (NoDataAvailableException e) {
            // no data available
            return;
        }
        if (consumed == null && target.getInstance() instanceof MultiplicityElement) {
            MultiplicityElement multiplicity = (MultiplicityElement) target.getInstance();
            boolean required = multiplicity.getLower() > 0;
            boolean multivalued = multiplicity.isMultivalued();
            if (multivalued)
                consumed = CollectionType.createCollectionFor(multiplicity);
            else if (required)
                consumed = RuntimeUtils.getDefaultValue((Classifier) target.getInstance().getType());
        }
        target.basicAddValue(consumed);
    }
}
