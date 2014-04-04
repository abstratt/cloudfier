package com.abstratt.mdd.runtime.core;

import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.runtime.ActorSelector;
import com.abstratt.mdd.core.runtime.Runtime;
import com.abstratt.nodestore.INodeStoreCatalog;
import com.abstratt.resman.ActivatableFeatureProvider;
import com.abstratt.resman.Resource;

public class RuntimeProvider implements ActivatableFeatureProvider {

	@Override
	public void activateContext(Resource<?> resource) {
		resource.getFeature(Runtime.class).enter();
	}
	
	public void deactivateContext(com.abstratt.resman.Resource<?> resource, boolean operationSucceeded) {
		resource.getFeature(Runtime.class).leave(operationSucceeded);
	}
	
	@Override
	public void initFeatures(Resource<?> resource) {
		IRepository repository = resource.getFeature(IRepository.class);
		INodeStoreCatalog nodeStoreCatalog = resource.getFeature(INodeStoreCatalog.class);
		ActorSelector actorSelector = resource.getFeature(ActorSelector.class);
		Runtime newRuntime = new Runtime(repository, nodeStoreCatalog, actorSelector);
		resource.setFeature(Runtime.class, newRuntime);
	}
	@Override
	public Class<?>[] getRequiredFeatureTypes() {
		return new Class<?>[] { IRepository.class, INodeStoreCatalog.class, ActorSelector.class };
	}
	@Override
	public Class<?>[] getProvidedFeatureTypes() {
		return new Class<?>[] { Runtime.class };
	}
}
