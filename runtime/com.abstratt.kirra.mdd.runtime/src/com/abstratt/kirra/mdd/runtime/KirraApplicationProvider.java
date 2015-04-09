package com.abstratt.kirra.mdd.runtime;

import com.abstratt.kirra.KirraApplication;
import com.abstratt.mdd.core.IRepository;
import com.abstratt.resman.FeatureProvider;
import com.abstratt.resman.Resource;

public class KirraApplicationProvider implements FeatureProvider {

    @Override
    public Class<?>[] getProvidedFeatureTypes() {
        return new Class<?>[] { KirraApplication.class };
    }

    @Override
    public Class<?>[] getRequiredFeatureTypes() {
        return new Class<?>[] { IRepository.class };
    }

    @Override
    public void initFeatures(Resource<?> resource) {
        IRepository repository = resource.getFeature(IRepository.class);
        String name = repository.getBaseURI().lastSegment();
        KirraApplication application = new KirraApplication(name);
        resource.setFeature(KirraApplication.class, application);
    }
}
