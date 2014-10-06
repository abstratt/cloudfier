package com.abstratt.mdd.core.target;

import java.util.Map;

import org.eclipse.uml2.uml.NamedElement;

public interface ITargetPlatform {
    String getId();

    <T extends NamedElement> ITopLevelMapper<T> getMapper(String artifactType);

    String getName();

    Map<String, String> getProperties();
}