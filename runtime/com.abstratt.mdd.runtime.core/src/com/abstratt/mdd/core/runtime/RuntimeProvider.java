package com.abstratt.mdd.core.runtime;

import com.abstratt.blobstore.IBlobStoreCatalog;
import com.abstratt.mdd.core.IRepository;
import com.abstratt.nodestore.INodeStoreCatalog;
import com.abstratt.resman.ActivatableFeatureProvider;
import com.abstratt.resman.Resource;
import com.abstratt.resman.TaskModeSelector;
import com.abstratt.resman.TaskModeSelector.Mode;

public class RuntimeProvider implements ActivatableFeatureProvider {

    @Override
    public void activateContext(Resource<?> resource) {
        boolean readOnly = resource.getFeature(TaskModeSelector.class).getMode() == Mode.ReadOnly;
        resource.getFeature(Runtime.class).enter(readOnly);
    }

    @Override
    public void deactivateContext(com.abstratt.resman.Resource<?> resource, boolean operationSucceeded) {
        resource.getFeature(Runtime.class).leave(operationSucceeded);
    }

    @Override
    public Class<?>[] getProvidedFeatureTypes() {
        return new Class<?>[] { Runtime.class };
    }

    @Override
    public Class<?>[] getRequiredFeatureTypes() {
        return new Class<?>[] { IRepository.class, IBlobStoreCatalog.class, INodeStoreCatalog.class, ActorSelector.class, TaskModeSelector.class };
    }

    @Override
    public void initFeatures(Resource<?> resource) {
        IRepository repository = resource.getFeature(IRepository.class);
        INodeStoreCatalog nodeStoreCatalog = resource.getFeature(INodeStoreCatalog.class);
        IBlobStoreCatalog blobStoreCatalog = resource.getFeature(IBlobStoreCatalog.class);
        ActorSelector actorSelector = resource.getFeature(ActorSelector.class);
        Runtime newRuntime = new Runtime(repository, nodeStoreCatalog, blobStoreCatalog, actorSelector);
        resource.setFeature(Runtime.class, newRuntime);
    }
}
