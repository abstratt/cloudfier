package com.abstratt.mdd.core.target;

import org.eclipse.uml2.uml.Action;

/**
 * An action-specific mapping protocol.
 */
public interface IActionMapper<A extends Action> {
    public String map(A action, IMappingContext context);
}
