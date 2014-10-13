package com.abstratt.mdd.core.target;

import java.util.Collection;
import java.util.Map;

import org.eclipse.uml2.uml.NamedElement;

public interface ITargetPlatform {
    String getId();

    <T extends NamedElement> ITopLevelMapper<T> getMapper(String artifactType);
    
    Collection<String> getArtifactTypes();

    Map<String, String> getProperties();
}