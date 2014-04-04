package com.abstratt.mdd.frontend.web.tests;

import java.io.IOException;
import java.net.URI;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;

import com.abstratt.mdd.frontend.web.WebFrontEnd;

public class StatusTests extends AbstractWebFrontEndTest {
	public StatusTests(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	private static final URI STATUS_URI = URI.create("http://localhost" + WebFrontEnd.STATUS_PATH);
	
	public void testStatus() throws HttpException, IOException {
		HttpMethod getRequest = buildGetRequest(STATUS_URI);
		executeMethod(200, getRequest);

	}

}
