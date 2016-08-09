package com.abstratt.kirra.mdd.rest;

import org.apache.commons.lang.StringUtils;

import com.abstratt.resman.FeatureProvider;
import com.abstratt.resman.Resource;
import com.abstratt.resman.TaskModeSelector;

/**
 * This class is both a feature provider and the feature itself.
 */
public class KirraRESTTaskModeSelector implements TaskModeSelector, FeatureProvider {

    private static ThreadLocal<Mode> currentMode = new ThreadLocal<TaskModeSelector.Mode>();
    private static ThreadLocal<String> currentEnvironment = new ThreadLocal<String>();

    @Override
    public Mode getMode() {
        return currentMode.get();
    }
    
    @Override
    public String getEnvironment() {
    	return StringUtils.defaultString(currentEnvironment.get(), "default");
    }

    public static void setTaskEnvironment(String newEnvironment) {
        if (newEnvironment != null)
            currentEnvironment.set(newEnvironment);
        else
            currentEnvironment.remove();
    }
    
    public static void setTaskMode(Mode newMode) {
        if (newMode != null)
            currentMode.set(newMode);
        else
            currentMode.remove();
    }

    public static Mode getTaskMode() {
        return currentMode.get();
    }
    
    public static String getTaskEnvironment() {
        return currentEnvironment.get();
    }
    
    @Override
    public Class<?>[] getProvidedFeatureTypes() {
        return new Class<?>[] {TaskModeSelector.class};
    }
    
    @Override
    public Class<?>[] getRequiredFeatureTypes() {
        return new Class<?>[0];
    }
    @Override
    public void initFeatures(Resource<?> resource) {
        resource.setFeature(TaskModeSelector.class, this);
    }
}
