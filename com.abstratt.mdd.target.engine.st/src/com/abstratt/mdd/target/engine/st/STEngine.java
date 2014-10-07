package com.abstratt.mdd.target.engine.st;

import java.net.URI;

import com.abstratt.mdd.core.target.ITopLevelMapper;
import com.abstratt.mdd.core.target.spi.CustomTargetPlatform;
import com.abstratt.mdd.core.target.spi.ITransformationEngine;

public class STEngine implements ITransformationEngine {

	@Override
	public ITopLevelMapper createLanguageMapper(
			CustomTargetPlatform platform, URI baseURI) {
		
		return new STLanguageMapper(platform.getProperties(), baseURI);
	}	
}
