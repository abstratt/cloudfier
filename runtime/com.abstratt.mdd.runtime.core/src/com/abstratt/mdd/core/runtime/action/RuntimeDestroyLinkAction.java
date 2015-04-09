package com.abstratt.mdd.core.runtime.action;

import java.util.List;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.DestroyLinkAction;
import org.eclipse.uml2.uml.InputPin;
import org.eclipse.uml2.uml.LinkEndData;
import org.eclipse.uml2.uml.Property;

import com.abstratt.mdd.core.runtime.CompositeRuntimeAction;
import com.abstratt.mdd.core.runtime.ExecutionContext;
import com.abstratt.mdd.core.runtime.RuntimeAction;
import com.abstratt.mdd.core.runtime.RuntimeObject;

public class RuntimeDestroyLinkAction extends RuntimeAction {

    public RuntimeDestroyLinkAction(Action instance, CompositeRuntimeAction parent) {
        super(instance, parent);
    }

    @Override
    public void executeBehavior(ExecutionContext context) {
        DestroyLinkAction instance = (DestroyLinkAction) getInstance();
        List<LinkEndData> endData = instance.getEndData();
        InputPin targetEndPin = endData.get(0).getValue();
        Property targetEnd = endData.get(1).getEnd();
        InputPin otherEndPin = endData.get(1).getValue();
        RuntimeObject targetObject = (RuntimeObject) getRuntimeObjectNode(targetEndPin).getValue();
        RuntimeObject otherObject = (RuntimeObject) getRuntimeObjectNode(otherEndPin).getValue();
        targetObject.unlink(targetEnd, otherObject);
    }
}
