package com.abstratt.mdd.core.target;

import org.eclipse.uml2.uml.Action;

public interface IMappingContext {
	enum Style {
		EXPRESSION, STATEMENT
	}
	public String map(Action target, Style nextStyle);
	public String map(Action target);
	public Style getCurrentStyle();
	public ILanguageMapper getLanguageMapper();
}
