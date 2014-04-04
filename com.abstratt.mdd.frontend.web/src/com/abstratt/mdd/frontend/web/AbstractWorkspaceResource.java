package com.abstratt.mdd.frontend.web;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

public abstract class AbstractWorkspaceResource extends ServerResource {

	protected static Log log = LogFactory
			.getLog(AbstractWorkspaceResource.class);

	protected static final String SECRET_PARAMETER = "secret";
	protected static final String SECRET_FILE = "__secret__";
	
	public File getWorkspaceDir(boolean required) {
		String workspace = getWorkspace();

		File workspaceDir = BuildDirectoryUtils.getDeployDirectory(workspace);
		ResourceUtils.ensure(!required || workspaceDir.isDirectory(), "Workspace not found: " + getWorkspace(),
				Status.CLIENT_ERROR_NOT_FOUND);
		return workspaceDir;
	}

	public String getWorkspace() {
		return (String) getRequestAttributes().get("workspace");
	}

	protected File[] listWorkspaceFiles() {
		File workspaceDir = getWorkspaceDir(false);
		if (!workspaceDir.isDirectory())
			return new File[0];
		File[] workspaceFiles = workspaceDir.listFiles();
		if (workspaceFiles == null)
			return new File[0];
		return workspaceFiles;
	}

	protected String getSecret() {
		return StringUtils.trimToEmpty(getQuery().getValuesMap().get(
				SECRET_PARAMETER));
	}

	protected void checkSecret() {
		File secretFile = new File(getWorkspaceDir(false), SECRET_FILE);
		if (!secretFile.isFile())
			return;
		String secretParameter = getSecret();
	    ResourceUtils.ensure(secretParameter != null, "Secret required but not provided", Status.CLIENT_ERROR_UNAUTHORIZED);
		String secret;
		try {
			secret = FileUtils.readFileToString(secretFile);
		} catch (IOException e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Error reading secret file");
		}
	    ResourceUtils.ensure(secret.equals(secretParameter), "Secret provided does not match", Status.CLIENT_ERROR_UNAUTHORIZED);
	}
}
