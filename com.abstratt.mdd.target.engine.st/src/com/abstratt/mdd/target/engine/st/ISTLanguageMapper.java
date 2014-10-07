package com.abstratt.mdd.target.engine.st;

import java.util.Map;

import com.abstratt.mdd.core.target.ILanguageMapper;

/** Not in use any longer. */
@Deprecated
public interface ISTLanguageMapper extends ILanguageMapper {
	void registerHandlers(
			Map<Class<?>, ModelWrapper.PropertyHandler<?>> handlers);
}
