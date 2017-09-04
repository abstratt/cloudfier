package com.abstratt.simpleblobstore;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class SimpleBlobStoreActivator implements BundleActivator {

	public static final String BUNDLE_NAME = SimpleBlobStoreActivator.class.getPackage().getName();

	@Override
	public void start(BundleContext context) throws Exception {
		if (SimpleBlobStoreCatalog.REPOSITORY_ROOT == null)
			throw new IllegalStateException("Could not determine a the blob store catalog root (property:" + SimpleBlobStoreCatalog.BLOBSTORE_FILE_BASE_KEY + ")");
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		
	}

}
