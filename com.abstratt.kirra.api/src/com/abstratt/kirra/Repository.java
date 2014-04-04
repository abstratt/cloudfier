package com.abstratt.kirra;

import java.net.URI;
import java.util.Properties;

/**
 * This API allows manipulation of both entity data and metadata.
 */
public interface Repository extends SchemaManagement, InstanceManagement {
	// REPOSITORY CONFIGURATION
	/**
	 * Determines the data repository this instance should connect to.
	 */
	public void setRepositoryURI(URI uri) throws KirraException;
	public URI getRepositoryURI();
	public void setValidating(boolean isValidating);
	public boolean isValidating();
	public void setFiltering(boolean filtering);
	public boolean isFiltering();
	public boolean isOpen();
	public void dispose();
	public void initialize();
	public Properties getProperties();
	public String getBuild();
}