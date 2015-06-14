package com.abstratt.mdd.frontend.web.tests;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

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
    
    private Bundle getBundle(BundleContext context, String symbolicName) throws BundleException {    
        for (Bundle current : context.getBundles())
            if (symbolicName.equals(current.getSymbolicName()))
                return current;
        return null;
    }

    private void ensureBundleStarted(BundleContext context, String symbolicName) throws BundleException {
        getBundle(context, symbolicName).start();
    }
}
