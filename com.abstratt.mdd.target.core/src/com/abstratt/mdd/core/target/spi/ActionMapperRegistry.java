package com.abstratt.mdd.core.target.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.core.runtime.Status;

import com.abstratt.mdd.core.target.IActionMapper;
import com.abstratt.mdd.core.target.TargetCore;
import com.abstratt.pluginutils.LogUtils;

public class ActionMapperRegistry {
	private static final String ATTRIBUTE_ACTION = "action";
	private static final String ATTRIBUTE_MAPPER = "mapper";

	private Map<String, List<IActionMapper>> actionMappers;

	private ActionMapperRegistry(String extensionPointId) {
		actionMappers = buildActionMapperRegistry(extensionPointId);
	}

	private Map<String, List<IActionMapper>> buildActionMapperRegistry(String extensionPointId) {
		IExtensionPoint extensionPoint = RegistryFactory.getRegistry().getExtensionPoint(extensionPointId);
		IExtension[] extensions = extensionPoint.getExtensions();
		Map<String, List<IActionMapper>> result = new HashMap<String, List<IActionMapper>>();
		for (IExtension extension : extensions)
			for (IConfigurationElement configElement : extension.getConfigurationElements()) {
				String action = configElement.getAttribute(ATTRIBUTE_ACTION);
				IActionMapper mapper = null;
				try {
						mapper = (IActionMapper) configElement.createExecutableExtension(ATTRIBUTE_MAPPER);
				} catch (CoreException e) {
					IStatus extensionError = new Status(IStatus.ERROR, TargetCore.PLUGIN_ID, "Error loading extension for " + extensionPointId, e);
					LogUtils.log(extensionError);
					continue;
				}
				List<IActionMapper> sameActionMappers = result.get(action);
				if (sameActionMappers == null) 
					result.put(action, sameActionMappers = new ArrayList<IActionMapper>());
				sameActionMappers.add(mapper);
			}
		return result;
	}

	public List<IActionMapper> getActionMappers(String action) {
		List<IActionMapper> found = actionMappers.get(action);
		return found == null ? Collections.<IActionMapper>emptyList() : Collections.unmodifiableList(found);
	}
}
