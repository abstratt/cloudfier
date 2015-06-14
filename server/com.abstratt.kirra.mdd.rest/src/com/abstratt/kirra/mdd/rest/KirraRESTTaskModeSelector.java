package com.abstratt.kirra.mdd.rest;

import com.abstratt.kirra.mdd.core.KirraHelper;
import com.abstratt.resman.FeatureProvider;
import com.abstratt.resman.Resource;
import com.abstratt.resman.TaskModeSelector;

/**
 * This class is both a feature provider and the feature itself.
 */
public class KirraRESTTaskModeSelector implements TaskModeSelector, FeatureProvider {

    private static ThreadLocal<Mode> currentMode = new ThreadLocal<TaskModeSelector.Mode>();

    @Override
    public Mode getMode() {
        return currentMode.get();
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
