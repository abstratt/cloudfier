package com.abstratt.mdd.core.target;

import java.util.Collection;
import java.util.Properties;

import com.abstratt.mdd.internal.core.target.TargetPlatformRegistry;

public class TargetCore {

    public static ITargetPlatform getBuiltInPlatform(String platformId) {
        return TargetPlatformRegistry.getInstance().getBuiltInPlatform(platformId);
    }

    public static ITargetPlatform getPlatform(Properties properties, String platformId) {
        return TargetPlatformRegistry.getInstance().getPlatform(properties, platformId);
    }

    public static Collection<String> getPlatformIds(Properties properties) {
        return TargetPlatformRegistry.getInstance().getPlatformIds(properties);
    }

    public static final String PLUGIN_ID = "com.abstratt.mdd.target.core";
}
