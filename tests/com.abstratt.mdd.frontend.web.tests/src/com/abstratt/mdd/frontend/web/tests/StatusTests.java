package com.abstratt.mdd.frontend.web.tests;

import java.io.IOException;
import java.net.URI;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;

import com.abstratt.mdd.frontend.web.WebFrontEnd;

public class StatusTests extends AbstractWebFrontEndTest {
    private static final URI STATUS_URI = URI.create("http://localhost" + WebFrontEnd.STATUS_PATH);

    public StatusTests(String name) {
        super(name);
        // TODO Auto-generated constructor stub
    }

    public void testStatus() throws HttpException, IOException {
        HttpMethod getRequest = buildGetRequest(StatusTests.STATUS_URI);
        executeMethod(200, getRequest);

    }

}
