package com.abstratt.kirra.server;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

import com.abstratt.pluginutils.LogUtils;

public class Activator implements BundleActivator {

    @Override
    public void start(BundleContext context) {
        for (Bundle current : context.getBundles())
            if (current.getBundleId() != context.getBundle().getBundleId() && current.getEntries().get(Constants.FRAGMENT_HOST) == null)
                try {
                    current.start();
                } catch (BundleException e) {
                    LogUtils.logError(getClass().getPackage().getName(), "Error starting " + current.getSymbolicName(), e);
                }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        // nothing to do...
    }

}
