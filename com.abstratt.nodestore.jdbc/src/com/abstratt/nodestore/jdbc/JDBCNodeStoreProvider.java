package com.abstratt.nodestore.jdbc;

import com.abstratt.kirra.KirraApplication;
import com.abstratt.kirra.SchemaManagement;
import com.abstratt.nodestore.INodeStoreCatalog;
import com.abstratt.resman.ActivatableFeatureProvider;
import com.abstratt.resman.Resource;
import com.abstratt.resman.TaskModeSelector;
import com.abstratt.resman.TaskModeSelector.Mode;

public class JDBCNodeStoreProvider implements ActivatableFeatureProvider {
    @Override
    public void activateContext(Resource<?> resource) {
        JDBCNodeStoreCatalog contextCatalog = (JDBCNodeStoreCatalog) resource.getFeature(INodeStoreCatalog.class);
        boolean readOnly = resource.getFeature(TaskModeSelector.class).getMode() == Mode.ReadOnly;
        contextCatalog.setReadOnly(readOnly);
    }

    @Override
    public void deactivateContext(Resource<?> resource, boolean operationSucceeded) {
        JDBCNodeStoreCatalog contextCatalog = (JDBCNodeStoreCatalog) resource.getFeature(INodeStoreCatalog.class);
        if (contextCatalog != null)
            contextCatalog.clearCache();
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
        resource.setFeature(INodeStoreCatalog.class, new JDBCNodeStoreCatalog(applicationName, schema));
    }
}
