package com.abstratt.mdd.core.target;

import org.eclipse.emf.query.conditions.eobjects.EObjectCondition;
import org.eclipse.uml2.uml.Action;

public interface IConditionedActionMapper<A extends Action> extends IActionMapper<A> {
    public EObjectCondition getCondition();
}
