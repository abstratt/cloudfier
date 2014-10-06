package com.abstratt.kirra.mdd.schema;

import com.abstratt.kirra.KirraApplication;
import com.abstratt.kirra.SchemaManagement;
import com.abstratt.kirra.SchemaManagementSnapshot;
import com.abstratt.kirra.mdd.core.KirraHelper;
import com.abstratt.resman.FeatureProvider;
import com.abstratt.resman.Resource;

public class KirraSchemaProvider implements FeatureProvider {

    @Override
    public Class<?>[] getProvidedFeatureTypes() {
        return new Class<?>[] { SchemaManagement.class };
    }

    @Override
    public Class<?>[] getRequiredFeatureTypes() {
        return new Class<?>[] { KirraHelper.Metadata.class, KirraApplication.class };
    }

    @Override
    public void initFeatures(Resource<?> resource) {
        SchemaManagement schema = new SchemaManagementSnapshot(new KirraMDDSchemaBuilder());
        resource.setFeature(SchemaManagement.class, schema);
    }
}
