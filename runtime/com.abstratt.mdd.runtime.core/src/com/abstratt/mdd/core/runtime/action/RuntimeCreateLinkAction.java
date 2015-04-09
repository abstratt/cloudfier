package com.abstratt.mdd.core.runtime.action;

import java.util.List;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.CreateLinkAction;
import org.eclipse.uml2.uml.LinkEndData;
import org.eclipse.uml2.uml.Property;

import com.abstratt.mdd.core.runtime.CompositeRuntimeAction;
import com.abstratt.mdd.core.runtime.ExecutionContext;
import com.abstratt.mdd.core.runtime.RuntimeAction;
import com.abstratt.mdd.core.runtime.RuntimeObject;

public class RuntimeCreateLinkAction extends RuntimeAction {

    public RuntimeCreateLinkAction(Action instance, CompositeRuntimeAction parent) {
        super(instance, parent);
    }

    @Override
    public void executeBehavior(ExecutionContext context) {
        CreateLinkAction instance = (CreateLinkAction) getInstance();
        List<LinkEndData> endData = instance.getEndData();
        RuntimeObject targetObject = (RuntimeObject) getRuntimeObjectNode(endData.get(0).getValue()).getValue();
        Property targetEnd = endData.get(0).getEnd();
        RuntimeObject otherObject = (RuntimeObject) getRuntimeObjectNode(endData.get(1).getValue()).getValue();
        Property otherEnd = targetEnd.getOtherEnd();
        targetObject.link(otherEnd, otherObject);
    }
}
