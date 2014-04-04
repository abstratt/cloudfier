package com.abstratt.mdd.core.target.spi;

import java.util.Stack;

import org.eclipse.uml2.uml.Action;

import com.abstratt.mdd.core.target.IActionMapper;
import com.abstratt.mdd.core.target.ILanguageMapper;
import com.abstratt.mdd.core.target.IMappingContext;

public class MappingContext implements IMappingContext {

	private ILanguageMapper languageMapper;

	private Stack<Style> styles;
	
	private MapperFinder mapperFinder;

	public MappingContext(ILanguageMapper mapper, Style defaultStyle, MapperFinder finder) {
		this.languageMapper = mapper;
		styles = new Stack<Style>();
		mapperFinder = finder;
		styles.push(defaultStyle);
	}

	public Style getCurrentStyle() {
		return styles.peek();
	}

	public String map(Action target) {
		return map(target, null);
	}

	public String map(Action target, Style nextStyle) {
		if (nextStyle == null)
			nextStyle = styles.peek();
		styles.push(nextStyle);
		try {
			IActionMapper<Action> mapper = (IActionMapper<Action>) mapperFinder.getMapping(target);
			return mapper != null ? mapper.map(target, this) : null;
		} finally {
			styles.pop();
		}
	}

	public ILanguageMapper getLanguageMapper() {
		return this.languageMapper;
	}
}
