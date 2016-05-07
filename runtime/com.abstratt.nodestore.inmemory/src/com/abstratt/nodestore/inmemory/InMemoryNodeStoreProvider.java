package com.abstratt.nodestore.inmemory;

import java.net.URI;

import com.abstratt.kirra.KirraApplication;
import com.abstratt.kirra.SchemaManagement;
import com.abstratt.nodestore.INodeStoreCatalog;
import com.abstratt.resman.ActivatableFeatureProvider;
import com.abstratt.resman.Resource;
import com.abstratt.resman.TaskModeSelector;
import com.abstratt.resman.TaskModeSelector.Mode;

public class InMemoryNodeStoreProvider implements ActivatableFeatureProvider {
    @Override
    public void activateContext(Resource<?> resource) {
        InMemoryNodeStoreCatalog contextCatalog = (InMemoryNodeStoreCatalog) resource.getFeature(INodeStoreCatalog.class);
        boolean readOnly = resource.getFeature(TaskModeSelector.class).getMode() == Mode.ReadOnly;
        contextCatalog.setReadOnly(readOnly);
    }

    @Override
    public void deactivateContext(Resource<?> resource, boolean operationSucceeded) {
        InMemoryNodeStoreCatalog contextCatalog = (InMemoryNodeStoreCatalog) resource.getFeature(INodeStoreCatalog.class);
    }

    @Override
    public Class<?>[] getProvidedFeatureTypes() {
        return new Class[] { INodeStoreCatalog.class };
    }

    @Override
    public Class<?>[] getRequiredFeatureTypes() {
        return new Class[] { SchemaManagement.class, KirraApplication.class, TaskModeSelector.class };
    }

    @Override
    public void initFeatures(Resource<?> resource) {
        SchemaManagement schema = resource.getFeature(SchemaManagement.class);
        String applicationName = resource.getFeature(KirraApplication.class).getName();
        resource.setFeature(INodeStoreCatalog.class, new InMemoryNodeStoreCatalog(applicationName, schema));
    }
    
    @Override
    public boolean isEnabled() {
    	String kind = InMemoryNodeStore.class.getSimpleName();
		return kind.equals(System.getProperty("nodestore.kind", kind));
    }
}
