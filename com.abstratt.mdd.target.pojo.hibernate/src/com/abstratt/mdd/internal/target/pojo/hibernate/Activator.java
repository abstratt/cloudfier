package com.abstratt.mdd.internal.target.pojo.hibernate;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	private static Activator instance;
	private Bundle bundle;

	public static Activator getInstance() {
		return instance;
	}

	public Bundle getBundle() {
		return bundle;
	}

	public void start(BundleContext context) throws Exception {
		bundle = context.getBundle();
		instance = this;
	}

	public void stop(BundleContext context) throws Exception {
		instance = null;
		bundle = null;
	}
}
