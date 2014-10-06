package com.abstratt.mdd.target.pojo;

import java.util.HashMap;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.RegistryFactory;

public class ExternalTypeMappingManager {
	private static ExternalTypeMappingManager instance = new ExternalTypeMappingManager();
	private HashMap<String, IExternalTypeMapper> mappers;

	public static ExternalTypeMappingManager getInstance() {
		return instance;
	}

	private ExternalTypeMappingManager() {
		initMappers();
	}

	public IExternalTypeMapper getMapper(String typeName) {
			return mappers.get(typeName);
	}
	
	private void initMappers() {
		IExtensionPoint point = RegistryFactory.getRegistry().getExtensionPoint(POJOMapper.PLUGIN_ID, "externalTypeMapper");
		IConfigurationElement[] elements = point.getConfigurationElements();
		mappers = new HashMap<String, IExternalTypeMapper>();
		for (int i = 0; i < elements.length; i++) {
			IExternalTypeMapper newMapper;
			try {
				newMapper = (IExternalTypeMapper) elements[i].createExecutableExtension("mapper");
				String typeName = elements[i].getAttribute("typeName");
				mappers.put(typeName, newMapper);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
