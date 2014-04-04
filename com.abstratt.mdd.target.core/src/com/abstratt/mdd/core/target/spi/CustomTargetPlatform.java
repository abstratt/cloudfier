package com.abstratt.mdd.core.target.spi;

import java.net.URI;
import java.util.Map;

import com.abstratt.mdd.core.target.ITargetPlatform;
import com.abstratt.mdd.core.target.ITopLevelMapper;


public class CustomTargetPlatform implements ITargetPlatform {
	private ITransformationEngine engine;
	private String id;
	private Map<String, String> properties;

	public CustomTargetPlatform(String id,  ITransformationEngine engine, Map<String, String> properties) {
		this.id = id;
		this.engine = engine;
		this.properties = properties;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public Map<String, String> getProperties() {
		return properties;
	}

	@Override
	public ITopLevelMapper getMapper(URI baseURI) {
		return engine.createLanguageMapper(this, baseURI);
	}

	@Override
	public String getName() {
		return properties.get("name");
	}
}