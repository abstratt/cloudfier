package com.abstratt.simpleblobstore;

import com.abstratt.blobstore.IBlobStoreCatalog;
import com.abstratt.kirra.KirraApplication;
import com.abstratt.resman.ActivatableFeatureProvider;
import com.abstratt.resman.Resource;
import com.abstratt.resman.TaskModeSelector;

public class SimpleBlobStoreProvider implements ActivatableFeatureProvider {
    @Override
    public void activateContext(Resource<?> resource) {
        SimpleBlobStoreCatalog blobStoreCatalog = (SimpleBlobStoreCatalog) resource.getFeature(IBlobStoreCatalog.class);
        TaskModeSelector taskModeSelector = resource.getFeature(TaskModeSelector.class);
        String environment = taskModeSelector.getEnvironment().toLowerCase();
        blobStoreCatalog.setEnvironment(environment);
        blobStoreCatalog.init();
    }

    @Override
    public void deactivateContext(Resource<?> resource, boolean operationSucceeded) {
        SimpleBlobStoreCatalog blobStore = (SimpleBlobStoreCatalog) resource.getFeature(IBlobStoreCatalog.class);
        blobStore.setEnvironment(null);
    }

    @Override
    public Class<?>[] getProvidedFeatureTypes() {
        return new Class[] { IBlobStoreCatalog.class };
    }

    @Override
    public Class<?>[] getRequiredFeatureTypes() {
        return new Class[] { KirraApplication.class, TaskModeSelector.class };
    }

    @Override
    public void initFeatures(Resource<?> resource) {
        String applicationName = resource.getFeature(KirraApplication.class).getName();
        resource.setFeature(IBlobStoreCatalog.class, new SimpleBlobStoreCatalog(applicationName));
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
}
