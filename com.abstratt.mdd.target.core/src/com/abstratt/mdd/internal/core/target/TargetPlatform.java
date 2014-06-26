package com.abstratt.mdd.internal.core.target;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

import com.abstratt.mdd.core.target.ITargetPlatform;
import com.abstratt.mdd.core.target.ITopLevelMapper;

public class TargetPlatform implements ITargetPlatform {

    private String id;
    private ITopLevelMapper mapper;
    private String name;

    public TargetPlatform(String id, String name, ITopLevelMapper mapper) {
        super();
        this.id = id;
        this.name = name;
        this.mapper = mapper;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public ITopLevelMapper getMapper(URI baseURI) {
        return mapper;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, String> getProperties() {
        return Collections.emptyMap();
    }
}
