package com.abstratt.mdd.core.target.spi;

import java.util.Stack;

import org.eclipse.uml2.uml.Action;

import com.abstratt.mdd.core.target.IActionMapper;
import com.abstratt.mdd.core.target.IMappingContext;
import com.abstratt.mdd.core.target.ITopLevelMapper;

public class MappingContext implements IMappingContext {

    private ITopLevelMapper<?> languageMapper;

    private Stack<Style> styles;

    private MapperFinder mapperFinder;

    public MappingContext(ITopLevelMapper<?> mapper, Style defaultStyle, MapperFinder finder) {
        this.languageMapper = mapper;
        styles = new Stack<Style>();
        mapperFinder = finder;
        styles.push(defaultStyle);
    }

    @Override
    public Style getCurrentStyle() {
        return styles.peek();
    }

    @Override
    public ITopLevelMapper<?> getLanguageMapper() {
        return this.languageMapper;
    }

    @Override
    public String map(Action target) {
        return map(target, null);
    }

    @Override
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
}
