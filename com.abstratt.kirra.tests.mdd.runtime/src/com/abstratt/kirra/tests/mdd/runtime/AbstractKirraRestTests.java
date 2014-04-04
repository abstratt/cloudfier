package com.abstratt.kirra.tests.mdd.runtime;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.codehaus.jackson.JsonNode;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import com.abstratt.kirra.mdd.rest.Paths;
import com.abstratt.mdd.core.tests.harness.AbstractRepositoryTests;
import com.abstratt.mdd.frontend.web.BuildDirectoryUtils;
import com.abstratt.mdd.frontend.web.tests.RestHelper;

public class AbstractKirraRestTests extends AbstractRepositoryTests {

	protected RestHelper restHelper;
	
	public AbstractKirraRestTests(String name) {
		super(name);
	}
	
	@Override
	protected IFileStore computeBaseDir() {
		String deployDir = BuildDirectoryUtils.getBaseDeployDirectory().getAbsolutePath();
		return EFS.getLocalFileSystem().getStore(new Path(deployDir ));
	}

	protected void login(String username, String password) throws HttpException, IOException {
		URI sessionURI = getWorkspaceURI();
		PostMethod loginMethod = new PostMethod(sessionURI.resolve(Paths.LOGIN).toString());
		loginMethod.setRequestEntity(new StringRequestEntity("login=" + username + "&password=" + password,"application/x-www-form-urlencoded", "UTF-8"));
		restHelper.executeMethod(200, loginMethod);
	}
	
	protected void logout() throws HttpException, IOException {
		URI sessionURI = getWorkspaceURI();
		PostMethod logoutMethod = new PostMethod(sessionURI.resolve(Paths.LOGOUT).toString());
		restHelper.executeMethod(200, logoutMethod);
	}

	protected void signUp(String username, String password) throws HttpException, IOException {
		signUp(username, password, 200);
	}
	
	protected void signUp(String username, String password, int expected) throws HttpException, IOException {
		URI sessionURI = getWorkspaceURI();
		PostMethod signUpMethod = new PostMethod(sessionURI.resolve(Paths.SIGNUP).toString());
		signUpMethod.setRequestEntity(new StringRequestEntity("login=" + username + "&password=" + password,"application/x-www-form-urlencoded", "UTF-8"));
		restHelper.executeMethod(expected, signUpMethod);
	}

	
	protected String getDefaultProperties() {
		return "mdd.modelWeaver=kirraWeaver\nmdd.extendBaseObject=true\nmdd.enableLibraries=true\nmdd.enableExtensions=true\nmdd.application.allowAnonymous=true";
	}

	protected URI getWorkspaceURI() throws IOException, HttpException {
		return restHelper.getApiUri();
	}
	
	protected void buildProjectAndLoadRepository(Map<String, byte[]> sources, boolean login) throws IOException, CoreException {
		sources = new HashMap<String, byte[]>(sources);
		sources.put("mdd.properties", getDefaultProperties().getBytes());
		restHelper.buildProject(sources);
		login("guest", "");
		restHelper.initDB();
		if (!login)
			logout();
	}
	
	protected HttpMethod buildMultipartRequest(URI location,
			Map<String, byte[]> toUpload) throws HttpException, IOException {
		return restHelper.buildMultipartRequest(location, toUpload);
	}

	protected HttpMethod buildUploadRequest(URI location,
			Map<String, byte[]> toUpload) throws HttpException, IOException {
		return restHelper.buildUploadRequest(location, toUpload);
	}

	protected HttpMethod buildGetRequest(URI location) throws HttpException,
			IOException {
		return restHelper.buildGetRequest(location);
	}
	
	protected byte[] executeMethod(int expectedStatus, HttpMethod method)
			throws IOException, HttpException {
		return restHelper.executeMethod(expectedStatus, method);
	}
	
	protected <T extends JsonNode> T executeJsonMethod(int expectedStatus, HttpMethod method)
			throws IOException, HttpException {
		return restHelper.executeJsonMethod(expectedStatus, method);
	}

	protected void buildProject(String source) throws IOException {
		restHelper.buildProject(source);
	}

	protected void buildProject(Map<String, byte[]> sources) throws IOException {
		restHelper.buildProject(sources);
	}

	protected URI getTestProjectURI() {
		return restHelper.getProjectUri();
	}

	protected String getTestWorkspaceName() {
		return getName();
	}

	@Override
	protected void tearDown() throws Exception {
		restHelper.dispose();
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		restHelper = new RestHelper(getName());
	}
}
