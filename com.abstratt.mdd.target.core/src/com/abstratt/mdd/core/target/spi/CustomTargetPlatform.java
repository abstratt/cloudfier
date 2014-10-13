package com.abstratt.mdd.core.target.spi;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import com.abstratt.mdd.core.target.ITargetPlatform;
import com.abstratt.mdd.core.target.ITopLevelMapper;

public class CustomTargetPlatform implements ITargetPlatform {
    private ITransformationEngine engine;
    private String id;
    private Map<String, String> properties;

    public CustomTargetPlatform(String id, ITransformationEngine engine, Map<String, String> properties) {
        this.id = id;
        this.engine = engine;
        this.properties = properties;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public ITopLevelMapper<?> getMapper(String artifactType) {
        return engine.createLanguageMapper(this);
    }
    
    @Override
    public Collection<String> getArtifactTypes() {
        return Arrays.asList(id);
    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }
}