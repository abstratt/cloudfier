package com.abstratt.kirra.internal.auth.ldap;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.abstratt.kirra.auth.AuthenticationService;
import com.abstratt.kirra.auth.EmailService;


public class Activator implements BundleActivator {
	
	public final static String ID = "com.abstratt.kirra.auth.ldap";  

	@Override
	public void start(BundleContext context) throws Exception {
		context.registerService(AuthenticationService.class, new StormpathAuthenticationService(), new Hashtable<String, String>());
		context.registerService(EmailService.class, new MandrillEmailService(), new Hashtable<String, String>());
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}
}
