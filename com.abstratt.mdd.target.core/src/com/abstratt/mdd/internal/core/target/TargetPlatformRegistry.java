package com.abstratt.mdd.internal.core.target;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.uml2.uml.NamedElement;

import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.target.ITargetPlatform;
import com.abstratt.mdd.core.target.ITopLevelMapper;
import com.abstratt.mdd.core.target.TargetCore;
import com.abstratt.mdd.core.target.spi.CustomTargetPlatform;
import com.abstratt.mdd.core.target.spi.ITransformationEngine;
import com.abstratt.pluginutils.LogUtils;

public class TargetPlatformRegistry {
    public static TargetPlatformRegistry getInstance() {
        return TargetPlatformRegistry.instance;
    }

    private static final String ATTRIBUTE_ENGINE_ID = "id";
    private static final String ATTRIBUTE_ENGINE_CLASS = "class";
    private static final String MAPPER_CLASS = "class";
    private static final String MAPPER = "mapper";
    private static final String ARTIFACT_TYPE = "artifactType";

    private static TargetPlatformRegistry instance = new TargetPlatformRegistry();

    private static final String TARGET_PLATFORM = TargetCore.PLUGIN_ID + ".targetPlatform";

    private static final String TRANSFORMATION_ENGINE = TargetCore.PLUGIN_ID + ".transformationEngine";

    private static Pattern PLATFORM_PROPERTY = Pattern.compile("(mdd.target\\.([^\\.]*))\\.([^\\.]*)");
    private Map<String, ITargetPlatform> builtInPlatforms;

    private Map<String, ITransformationEngine> transformationEngines;

    private TargetPlatformRegistry() {
        try {
            builtInPlatforms = buildTargetPlatformRegistry(RegistryFactory.getRegistry());
        } catch (RuntimeException e) {
            LogUtils.logError(TargetCore.PLUGIN_ID, "Exception on initialization", e);
        }
        try {
            transformationEngines = buildTransformationEngineRegistry(RegistryFactory.getRegistry());
        } catch (RuntimeException e) {
            LogUtils.logError(TargetCore.PLUGIN_ID, "Exception on initialization", e);
        }

    }

    public ITargetPlatform getBuiltInPlatform(String platformId) {
        return builtInPlatforms.get(platformId);
    }

    public ITargetPlatform getPlatform(Properties properties, String platformId) {
        String engineId = properties.getProperty(IRepository.TARGET_ENGINE);
        if (transformationEngines.containsKey(engineId) && !builtInPlatforms.containsKey(platformId)) {
            ITransformationEngine engine = transformationEngines.get(engineId);
            ITargetPlatform customPlatform = getCustomPlatform(engine, properties, platformId);
            if (customPlatform != null)
                return customPlatform;
        }
        return getBuiltInPlatform(platformId);
    }

    public Collection<String> getPlatformIds(Properties properties) {
        String engineId = properties.getProperty(IRepository.TARGET_ENGINE);
        boolean knownEngine = transformationEngines.containsKey(engineId);
        Set<String> found = new TreeSet<String>();
        for (String property : properties.stringPropertyNames()) {
            Matcher matcher = TargetPlatformRegistry.PLATFORM_PROPERTY.matcher(property);
            if (matcher.matches()) {
                String platform = matcher.group(2);
                String platformProperty = matcher.group(3);
                boolean isBuiltIn = builtInPlatforms.containsKey(platform);
                boolean builtInEnabled = isBuiltIn && "enabled".equals(platformProperty)
                        && Boolean.parseBoolean(properties.getProperty(property));
                if (builtInEnabled || !isBuiltIn && knownEngine)
                    found.add(platform);
            }
        }
        return found;
    }

    private Map<String, ITargetPlatform> buildTargetPlatformRegistry(IExtensionRegistry registry) {
        IExtensionPoint extensionPoint = registry.getExtensionPoint(TargetPlatformRegistry.TARGET_PLATFORM);
        IExtension[] extensions = extensionPoint.getExtensions();
        Map<String, ITargetPlatform> result = new HashMap<String, ITargetPlatform>();
        for (IExtension extension : extensions) {
            IConfigurationElement[] configElements = extension.getConfigurationElements();
            Map<String, ITopLevelMapper<? extends NamedElement>> mappers = new LinkedHashMap<String, ITopLevelMapper<? extends NamedElement>>();
            for (IConfigurationElement configElement : configElements) {
                if (configElement.getName().equals(MAPPER)) {
                    try {
                        if (configElement.getAttribute(MAPPER_CLASS) != null) {
                            ITopLevelMapper<?> mapper = (ITopLevelMapper<?>) configElement.createExecutableExtension(MAPPER_CLASS);
                            mappers.put(configElement.getAttribute(ARTIFACT_TYPE), mapper);
                        }
                    } catch (CoreException e) {
                        LogUtils.log(e.getStatus());
                        break;
                    }
                }
            }
            String id = extension.getSimpleIdentifier();
            ITargetPlatform targetPlatform = new TargetPlatform(id, mappers);
            result.put(targetPlatform.getId(), targetPlatform);
        }
        return result;
    }

    private Map<String, ITransformationEngine> buildTransformationEngineRegistry(IExtensionRegistry registry) {
        IExtensionPoint extensionPoint = registry.getExtensionPoint(TargetPlatformRegistry.TRANSFORMATION_ENGINE);
        IExtension[] extensions = extensionPoint.getExtensions();
        Map<String, ITransformationEngine> result = new HashMap<String, ITransformationEngine>();
        for (IExtension extension : extensions) {
            IConfigurationElement[] configElements = extension.getConfigurationElements();
            for (IConfigurationElement configElement : configElements) {
                String id = configElement.getAttribute(TargetPlatformRegistry.ATTRIBUTE_ENGINE_ID);
                ITransformationEngine engine = null;
                try {
                    if (configElement.getAttribute(ATTRIBUTE_ENGINE_CLASS) != null)
                        engine = (ITransformationEngine) configElement
                                .createExecutableExtension(ATTRIBUTE_ENGINE_CLASS);
                } catch (CoreException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    break;
                }
                result.put(id, engine);
            }
        }
        return result;
    }

    private ITargetPlatform getCustomPlatform(ITransformationEngine engine, Properties properties, String platformId) {
        Map<String, String> platformProperties = new HashMap<String, String>();
        for (String property : properties.stringPropertyNames()) {
            Matcher matcher = TargetPlatformRegistry.PLATFORM_PROPERTY.matcher(property);
            if (matcher.matches() && platformId.equals(matcher.group(2)))
                platformProperties.put(matcher.group(3), properties.getProperty(property));
        }
        if (properties.containsKey(IRepository.IMPORTED_PROJECTS))
            platformProperties.put(IRepository.IMPORTED_PROJECTS, properties.getProperty(IRepository.IMPORTED_PROJECTS));
        return platformProperties.isEmpty() ? null : new CustomTargetPlatform(platformId, engine, platformProperties);
    }
}
