package com.abstratt.kirra.server;

import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import com.abstratt.pluginutils.LogUtils;

public class Activator implements BundleActivator {

	public void start(BundleContext context) {
		BundleDescription[] allBundles = Platform.getPlatformAdmin().getState().getBundles();
		for (BundleDescription current : allBundles)
			if (current.getHost() == null && current.getBundleId() != context.getBundle().getBundleId())
				try {
					context.getBundle(current.getBundleId()).start();
				} catch (BundleException e) {
					LogUtils.logError(getClass().getPackage().getName(), "Error starting " + current.getSymbolicName(), e);
				}
	}

	public void stop(BundleContext context) throws Exception {
		// nothing to do...
	}

}
