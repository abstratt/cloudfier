package com.abstratt.nodestore.inmemory;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class InMemoryNodeStoreActivator implements BundleActivator {

	public static final String BUNDLE_NAME = InMemoryNodeStoreActivator.class.getPackage().getName();

	@Override
	public void start(BundleContext context) throws Exception {
		if (InMemoryNodeStoreCatalog.REPOSITORY_ROOT == null)
			throw new IllegalStateException("Could not determine a the node store catalog root (property:" + InMemoryNodeStoreCatalog.NODESTORE_FILE_BASE_KEY + ")");
		
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		
	}

}
