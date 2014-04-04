package com.abstratt.mdd.core.target.spi;

import java.net.URI;

import com.abstratt.mdd.core.target.ITopLevelMapper;

/**
 * A transformation engine.
 */
public interface ITransformationEngine {
	public ITopLevelMapper createLanguageMapper(CustomTargetPlatform customTargetPlatform, URI baseURI);
}
