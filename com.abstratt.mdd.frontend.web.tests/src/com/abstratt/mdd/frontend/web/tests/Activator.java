package com.abstratt.mdd.frontend.web.tests;

import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class Activator implements BundleActivator {

    private static final String HTTP_SERVICE_BUNDLE = "org.eclipse.equinox.http.jetty";
    private static final String HTTP_REGISTRY_BUNDLE = "org.eclipse.equinox.http.registry";

    @Override
    public void start(BundleContext context) throws BundleException {
        ensureBundleStarted(context, Activator.HTTP_SERVICE_BUNDLE);
        ensureBundleStarted(context, Activator.HTTP_REGISTRY_BUNDLE);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        // nothing to do
    }

    private void ensureBundleStarted(BundleContext context, String symbolicName) throws BundleException {
        State state = Platform.getPlatformAdmin().getState();
        BundleDescription bundleDesc = state.getBundle(symbolicName, null);
        if (bundleDesc == null)
            throw new IllegalStateException("Could not find bundle " + symbolicName);
        long bundleId = bundleDesc.getBundleId();
        context.getBundle(bundleId).start();
    }
}
