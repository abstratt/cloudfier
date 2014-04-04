package com.abstratt.kirra.mdd.rest;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.util.tracker.ServiceTracker;

import com.abstratt.kirra.auth.AuthenticationService;
import com.abstratt.kirra.auth.EmailService;
import com.abstratt.kirra.auth.TransientAuthenticationService;

public class Activator implements BundleActivator {

	private static Activator instance;
	private BundleContext context;
	private ServiceTracker<AuthenticationService, AuthenticationService> authenticationTracker;
	private AuthenticationService transientAuthentication = new TransientAuthenticationService();
	private ServiceTracker<EmailService, EmailService> emailTracker;
	private String applicationVersion;
	
	@Override
	public void start(BundleContext context) throws Exception {
		this.context = context;
		authenticationTracker = new ServiceTracker<AuthenticationService, AuthenticationService>(context, AuthenticationService.class, null);
		authenticationTracker.open();
		emailTracker = new ServiceTracker<EmailService, EmailService>(context, EmailService.class, null);
		emailTracker.open();
		this.applicationVersion = context.getBundle().getHeaders().get(Constants.BUNDLE_VERSION);
		instance = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		instance = null;
		this.context = null;
	}
	
	public BundleContext getContext() {
		return context;
	}
	
	public static Activator getInstance() {
		return instance;
	}
	
	public String getPlatformVersion() {
		return applicationVersion;
	}
	
	public AuthenticationService getAuthenticationService() {
		AuthenticationService boundService = Boolean.getBoolean("mdd.offlineAuthentication") ? transientAuthentication : authenticationTracker.getService();
		return boundService != null ? boundService : transientAuthentication;
	}
	
	public EmailService getEmailService() {
		EmailService boundService = emailTracker.getService();
		return boundService;
	}
}
