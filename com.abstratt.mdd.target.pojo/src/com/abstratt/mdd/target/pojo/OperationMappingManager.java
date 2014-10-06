package com.abstratt.mdd.target.pojo;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.RegistryFactory;

public class OperationMappingManager {
	private static OperationMappingManager instance = new OperationMappingManager();
	private HashMap<String, List<IOperationMapper>> mappers;

	public static OperationMappingManager getInstance() {
		return instance;
	}

	private OperationMappingManager() {
		initMappers();
	}

	public List<IOperationMapper> getMappers(String annotation) {
			final List<IOperationMapper> mappersForAnnotation = mappers.get(annotation);
			final List<IOperationMapper> emptyList = Collections.emptyList();
			return mappersForAnnotation ==  null ? emptyList : mappersForAnnotation;
	}
	
	private void initMappers() {
		IExtensionPoint point = RegistryFactory.getRegistry().getExtensionPoint(POJOMapper.PLUGIN_ID, "operationMapper");
		IConfigurationElement[] elements = point.getConfigurationElements();
		mappers = new HashMap<String, List<IOperationMapper>>();
		for (int i = 0; i < elements.length; i++) {
			IOperationMapper mapper;
			try {
				mapper = (IOperationMapper) elements[i].createExecutableExtension("class");
				String annotation = elements[i].getAttribute("annotation");
				List<IOperationMapper> mappersForAnnotation = mappers.get(annotation);
				if (mappersForAnnotation == null)
					mappers.put(annotation, mappersForAnnotation = new LinkedList<IOperationMapper>());
				mappersForAnnotation.add(mapper);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
