package com.abstratt.kirra.server;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

import com.abstratt.pluginutils.LogUtils;

public class Activator implements BundleActivator {

    private static final String JETTY_HTTP_PORT = "org.eclipse.equinox.http.jetty.http.port";
	private static final String CLOUDFIER_API_PORT = "cloudfier.api.port";

	@Override
    public void start(BundleContext context) {
        decidePort();
        
        for (Bundle current : context.getBundles())
            if (current.getBundleId() != context.getBundle().getBundleId() && current.getHeaders().get(Constants.FRAGMENT_HOST) == null)
                try {
                    LogUtils.logInfo(getClass().getPackage().getName(), "Starting bundle : " + current.getHeaders().get(Constants.BUNDLE_SYMBOLICNAME), null);
                    current.start();
                } catch (BundleException e) {
                    LogUtils.logError(getClass().getPackage().getName(), "Error starting " + current.getSymbolicName(), e);
                }
    }

    private void decidePort() {
        String defaultPort = "8081";
        String cloudfierAPIPort = System.getProperty(CLOUDFIER_API_PORT);
        String equinoxHttpPort = System.getProperty(JETTY_HTTP_PORT);
        if (cloudfierAPIPort != null)
            System.setProperty(JETTY_HTTP_PORT, cloudfierAPIPort);
        else
            if (equinoxHttpPort != null)
                System.setProperty(CLOUDFIER_API_PORT, equinoxHttpPort);
            else {
                System.setProperty(CLOUDFIER_API_PORT, defaultPort);
                System.setProperty(JETTY_HTTP_PORT, defaultPort);
            }
        LogUtils.logInfo(getClass().getPackage().getName(), "Instance location: " + Platform.getInstanceLocation().getURL(), null);
        LogUtils.logInfo(getClass().getPackage().getName(), 
                "Internal port: " + System.getProperty(CLOUDFIER_API_PORT), null);
        
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        // nothing to do...
    }

}
