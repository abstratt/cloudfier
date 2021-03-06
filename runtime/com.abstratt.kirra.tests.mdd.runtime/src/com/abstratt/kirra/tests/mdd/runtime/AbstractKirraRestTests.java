package com.abstratt.kirra.tests.mdd.runtime;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import com.abstratt.mdd.core.tests.harness.AbstractRepositoryTests;
import com.abstratt.mdd.frontend.web.BuildDirectoryUtils;
import com.abstratt.mdd.frontend.web.tests.RestHelper;
import com.fasterxml.jackson.databind.JsonNode;

public abstract class AbstractKirraRestTests extends AbstractRepositoryTests {

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
        restHelper.initDB();
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
        return "mdd.modelWeaver=kirraWeaver\nmdd.extendBaseObject=true\nmdd.enableLibraries=true\nmdd.enableExtensions=true\nmdd.enableMedia=true\nmdd.application.allowAnonymous=false\nmdd.application.loginRequired=true";
    }

    protected URI getTestProjectURI() {
        return restHelper.getProjectUri();
    }

    protected String getTestWorkspaceName() {
        return getName();
    }

    protected URI getWorkspaceBaseURI() throws IOException, HttpException {
        return restHelper.getApiUri();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        restHelper = new RestHelper(getName());
    }

    protected abstract void login(String username, String password) throws HttpException, IOException;

    protected abstract void logout() throws HttpException, IOException;
    

    @Override
    protected void tearDown() throws Exception {
        restHelper.dispose();
    }
}
