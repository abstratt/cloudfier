package com.abstratt.mdd.core.target;

import org.eclipse.uml2.uml.Action;

public interface IMappingContext {
    enum Style {
        EXPRESSION, STATEMENT
    }

    public Style getCurrentStyle();

    public ILanguageMapper getLanguageMapper();

    public String map(Action target);

    public String map(Action target, Style nextStyle);
}
