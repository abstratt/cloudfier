package com.abstratt.kirra.mdd.runtime;

import com.abstratt.kirra.KirraApplication;
import com.abstratt.kirra.SchemaManagement;
import com.abstratt.kirra.mdd.core.KirraHelper;
import com.abstratt.resman.FeatureProvider;
import com.abstratt.resman.Resource;

public class KirraSchemaProvider implements FeatureProvider {
	
	@Override
	public Class<?>[] getRequiredFeatureTypes() {
		return new Class<?>[] { KirraHelper.Metadata.class, KirraApplication.class };
	}
	@Override
	public Class<?>[] getProvidedFeatureTypes() {
		return new Class<?>[] { SchemaManagement.class };
	}
	@Override
	public void initFeatures(Resource<?> resource) {
		SchemaManagement schema = new SchemaManagementSnapshot();
		resource.setFeature(SchemaManagement.class, schema);
	}
}
