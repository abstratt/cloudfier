package com.abstratt.kirra.mdd.runtime;

import com.abstratt.kirra.ExternalService;
import com.abstratt.kirra.KirraApplication;
import com.abstratt.kirra.Repository;
import com.abstratt.kirra.Schema;
import com.abstratt.kirra.mdd.core.KirraHelper;
import com.abstratt.mdd.core.runtime.Runtime;
import com.abstratt.resman.ActivatableFeatureProvider;
import com.abstratt.resman.ExceptionTranslationFeatureProvider;
import com.abstratt.resman.Resource;

public class KirraRepositoryProvider implements ExceptionTranslationFeatureProvider, ActivatableFeatureProvider {
	
	@Override
	public Throwable translate(Throwable toTranslate) {
		return KirraOnMDDRuntime.translateException(toTranslate);
	}
	
	@Override
	public void activateContext(Resource<?> resource) {
	}
	
	@Override
	public void deactivateContext(Resource<?> resource, boolean operationSucceeded) {
		((KirraOnMDDRuntime) resource.getFeature(Repository.class)).flush();
	}
	
	@Override
	public Class<?>[] getRequiredFeatureTypes() {
		return new Class<?>[] { Runtime.class, ExternalService.class, KirraHelper.Metadata.class, KirraApplication.class, Schema.class };
	}
	@Override
	public Class<?>[] getProvidedFeatureTypes() {
		return new Class<?>[] { Repository.class };
	}
	@Override
	public void initFeatures(Resource<?> resource) {
		Runtime runtime = resource.getFeature(Runtime.class);
		KirraOnMDDRuntime repository = KirraOnMDDRuntime.create();
		runtime.registerExternalDelegate(repository);
		resource.setFeature(Repository.class, repository);
	}
}
