package com.abstratt.mdd.frontend.web.tests;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;

import com.abstratt.mdd.core.tests.harness.AbstractRepositoryTests;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * @deprecated declare a child {@link RestHelper} object instead
 */
@Deprecated
public class AbstractWebFrontEndTest extends AbstractRepositoryTests {

    protected RestHelper restHelper;

    AbstractWebFrontEndTest(String name) {
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

    protected HttpMethod buildUploadRequest(URI location, Map<String, byte[]> toUpload) throws HttpException, IOException {
        return restHelper.buildUploadRequest(location, toUpload);
    }

    protected <T extends JsonNode> T executeJsonMethod(int expectedStatus, HttpMethod method) throws IOException, HttpException {
        return restHelper.executeJsonMethod(expectedStatus, method);
    }

    protected byte[] executeMethod(int expectedStatus, HttpMethod method) throws IOException, HttpException {
        return restHelper.executeMethod(expectedStatus, method);
    }

    protected URI getTestProjectURI() {
        return restHelper.getProjectUri();
    }

    protected String getTestWorkspaceName() {
        return getName();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        restHelper = new RestHelper(getName());
    }

    @Override
    protected void tearDown() throws Exception {
        restHelper.dispose();
    }
}
