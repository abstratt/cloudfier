package com.abstratt.kirra.mdd.rest;

import com.abstratt.mdd.core.runtime.ActorSelector;
import com.abstratt.resman.ActivatableFeatureProvider;
import com.abstratt.resman.Resource;

public class KirraRESTActorSelectorProvider implements ActivatableFeatureProvider {

    @Override
    public void activateContext(Resource<?> resource) {
        KirraRESTActorSelector actorSelector = getCurrentActorSelector(resource);
        actorSelector.clearCache();
    }

    @Override
    public void deactivateContext(Resource<?> resource, boolean operationSucceeded) {
        KirraRESTActorSelector actorSelector = getCurrentActorSelector(resource);
        actorSelector.clearCache();
    }

    public KirraRESTActorSelector getCurrentActorSelector(Resource<?> resource) {
        return (KirraRESTActorSelector) resource.getFeature(ActorSelector.class);
    }

    @Override
    public Class<?>[] getProvidedFeatureTypes() {
        return new Class[] { ActorSelector.class };
    }

    @Override
    public Class<?>[] getRequiredFeatureTypes() {
        return new Class[0];
    }

    @Override
    public void initFeatures(Resource<?> resource) {
        resource.setFeature(ActorSelector.class, new KirraRESTActorSelector());
    }
}
