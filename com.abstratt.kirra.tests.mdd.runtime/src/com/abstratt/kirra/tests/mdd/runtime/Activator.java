package com.abstratt.kirra.tests.mdd.runtime;

import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class Activator implements BundleActivator {

	private static final String HTTP_SERVICE_BUNDLE = "org.eclipse.equinox.http.jetty";
	private static final String HTTP_REGISTRY_BUNDLE = "org.eclipse.equinox.http.registry";
	static BundleContext bundleContext;

	@Override
	public void start(BundleContext context) throws BundleException {
		 ensureBundleStarted(context, HTTP_SERVICE_BUNDLE);
		 ensureBundleStarted(context, HTTP_REGISTRY_BUNDLE);
		 bundleContext = context;
	}

	private void ensureBundleStarted(BundleContext context, String symbolicName)
			throws BundleException {
		State state = Platform.getPlatformAdmin().getState();
		BundleDescription bundleDesc = state.getBundle(symbolicName,
				null);
		if (bundleDesc == null)
			throw new IllegalStateException(
					"Could not find bundle "
							+ symbolicName);
		long bundleId = bundleDesc.getBundleId();
		context.getBundle(bundleId).start();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// nothing to do
	}
}
