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

import com.abstratt.mdd.core.tests.harness.AbstractRepositoryTests;
import com.abstratt.mdd.frontend.web.BuildDirectoryUtils;
import com.abstratt.mdd.frontend.web.tests.RestHelper;

public class AbstractKirraRestTests extends AbstractRepositoryTests {

    protected RestHelper restHelper;

    public AbstractKirraRestTests(String name) {
        super(name);
    }

    protected HttpMethod buildGetRequest(URI location) throws HttpException, IOException {
        return restHelper.buildGetRequest(location);
    }

    protected HttpMethod buildMultipartRequest(URI location, Map<String, byte[]> toUpload) throws HttpException, IOException {
        return restHelper.buildMultipartRequest(location, toUpload);
    }

    protected void buildProject(Map<String, byte[]> sources) throws IOException {
        restHelper.buildProject(sources);
    }

    protected void buildProject(String source) throws IOException {
        restHelper.buildProject(source);
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

    protected HttpMethod buildUploadRequest(URI location, Map<String, byte[]> toUpload) throws HttpException, IOException {
        return restHelper.buildUploadRequest(location, toUpload);
    }

    @Override
    protected IFileStore computeBaseDir() {
        String deployDir = BuildDirectoryUtils.getBaseDeployDirectory().getAbsolutePath();
        return EFS.getLocalFileSystem().getStore(new Path(deployDir));
    }

    protected <T extends JsonNode> T executeJsonMethod(int expectedStatus, HttpMethod method) throws IOException, HttpException {
        return restHelper.executeJsonMethod(expectedStatus, method);
    }

    protected byte[] executeMethod(int expectedStatus, HttpMethod method) throws IOException, HttpException {
        return restHelper.executeMethod(expectedStatus, method);
    }

    protected String getDefaultProperties() {
        return "mdd.modelWeaver=kirraWeaver\nmdd.extendBaseObject=true\nmdd.enableLibraries=true\nmdd.enableExtensions=true\nmdd.application.allowAnonymous=true";
    }

    protected URI getTestProjectURI() {
        return restHelper.getProjectUri();
    }

    protected String getTestWorkspaceName() {
        return getName();
    }

    protected URI getWorkspaceURI() throws IOException, HttpException {
        return restHelper.getApiUri();
    }

    protected void login(String username, String password) throws HttpException, IOException {
        URI sessionURI = getWorkspaceURI();
        PostMethod loginMethod = new PostMethod(sessionURI.resolve(com.abstratt.mdd.frontend.web.Paths.LOGIN).toString());
        loginMethod.setRequestEntity(new StringRequestEntity("login=" + username + "&password=" + password,
                "application/x-www-form-urlencoded", "UTF-8"));
        restHelper.executeMethod(200, loginMethod);
    }

    protected void logout() throws HttpException, IOException {
        URI sessionURI = getWorkspaceURI();
        PostMethod logoutMethod = new PostMethod(sessionURI.resolve(com.abstratt.mdd.frontend.web.Paths.LOGOUT).toString());
        restHelper.executeMethod(200, logoutMethod);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        restHelper = new RestHelper(getName());
    }

    protected void signUp(String username, String password) throws HttpException, IOException {
        signUp(username, password, 200);
    }

    protected void signUp(String username, String password, int expected) throws HttpException, IOException {
        URI sessionURI = getWorkspaceURI();
        PostMethod signUpMethod = new PostMethod(sessionURI.resolve(com.abstratt.mdd.frontend.web.Paths.SIGNUP).toString());
        signUpMethod.setRequestEntity(new StringRequestEntity("login=" + username + "&password=" + password,
                "application/x-www-form-urlencoded", "UTF-8"));
        restHelper.executeMethod(expected, signUpMethod);
    }

    @Override
    protected void tearDown() throws Exception {
        restHelper.dispose();
    }
}
