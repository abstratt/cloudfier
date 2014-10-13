package com.abstratt.mdd.internal.core.target;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.eclipse.uml2.uml.NamedElement;

import com.abstratt.mdd.core.target.ITargetPlatform;
import com.abstratt.mdd.core.target.ITopLevelMapper;

public class TargetPlatform implements ITargetPlatform {

    private String id;
    private Map<String, ITopLevelMapper<? extends NamedElement>> mappers;

    public TargetPlatform(String id, Map<String, ITopLevelMapper<? extends NamedElement>> mappers) {
        super();
        this.id = id;
        this.mappers = mappers;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public ITopLevelMapper<? extends NamedElement> getMapper(String mapperId) {
        return mappers.get(mapperId);
    }
    
    @Override
    public Collection<String> getArtifactTypes() {
        return mappers.keySet();
    }

    @Override
    public Map<String, String> getProperties() {
        return Collections.emptyMap();
    }
}
