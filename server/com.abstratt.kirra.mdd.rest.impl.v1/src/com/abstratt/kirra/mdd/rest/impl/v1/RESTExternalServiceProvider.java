package com.abstratt.kirra.mdd.rest.impl.v1;

import com.abstratt.kirra.ExternalService;
import com.abstratt.resman.FeatureProvider;
import com.abstratt.resman.Resource;

public class RESTExternalServiceProvider implements FeatureProvider {

    @Override
    public Class<?>[] getProvidedFeatureTypes() {
        return new Class<?>[] { ExternalService.class };
    }

    @Override
    public Class<?>[] getRequiredFeatureTypes() {
        return new Class<?>[0];
    }

    @Override
    public void initFeatures(Resource<?> resource) {
        resource.setFeature(ExternalService.class, new KirraRESTExternalService());
    }
}
