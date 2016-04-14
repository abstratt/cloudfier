package com.abstratt.kirra.auth;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		context.registerService(AuthenticationService.class, new DatabaseAuthenticationService(),
				new Hashtable<String, String>());
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}
	
}
