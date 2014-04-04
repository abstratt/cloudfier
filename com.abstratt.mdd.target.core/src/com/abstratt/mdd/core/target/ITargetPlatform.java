package com.abstratt.mdd.core.target;

import java.net.URI;
import java.util.Map;

import org.eclipse.uml2.uml.NamedElement;

public interface ITargetPlatform {
	String getId();
	String getName();
	<T extends NamedElement> ITopLevelMapper<T> getMapper(URI baseURI);
    Map<String, String> getProperties();
}