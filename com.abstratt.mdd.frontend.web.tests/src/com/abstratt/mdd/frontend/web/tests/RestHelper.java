package com.abstratt.mdd.frontend.web.tests;

import java.io.ByteArrayInputStream;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.codehaus.jackson.JsonNode;

import com.abstratt.mdd.frontend.web.JsonHelper;
import com.abstratt.mdd.frontend.web.Paths;
import com.abstratt.mdd.frontend.web.WebFrontEnd;
import org.junit.Assert;

public class RestHelper {

	private HttpClient httpClient;
	private String workspaceName;

	public RestHelper(String name) {
		httpClient = new HttpClient();
		workspaceName = name;
	}

	public HttpMethod buildMultipartRequest(URI location,
			Map<String, byte[]> toUpload) throws HttpException, IOException {
		PostMethod filePost = new PostMethod(location.toASCIIString());
		List<Part> parts = new ArrayList<Part>(toUpload.size());
		for (Entry<String, byte[]> entry : toUpload.entrySet())
			parts.add(new FilePart(entry.getKey(), new ByteArrayPartSource(
					entry.getKey(), entry.getValue())));
		filePost.setRequestEntity(new MultipartRequestEntity(parts
				.toArray(new Part[0]), filePost.getParams()));
		return filePost;
	}

	public HttpMethod buildUploadRequest(URI location,
			Map<String, byte[]> toUpload) throws HttpException, IOException {
		return buildMultipartRequest(location, toUpload);
	}

	public HttpMethod buildGetRequest(URI location) throws HttpException,
			IOException {
		return new GetMethod(location.toASCIIString());
	}
	
	public byte[] executeMethod(int expectedStatus, HttpMethod method)
			throws IOException, HttpException {
		return executeMethod(httpClient, expectedStatus, method);
	}

	public byte[] executeMethod(HttpClient httpClient, int expectedStatus, HttpMethod method)
			throws IOException, HttpException {
		try {
			int response = httpClient.executeMethod(method);
			byte[] body = method.getResponseBody();
			Assert.assertEquals("Method: " + method.getName() + " - URI: " + method.getURI() + "\n" + new String(body), expectedStatus, response);
			return body;
		} finally {
			method.releaseConnection();
		}
	}

	
	public <T extends JsonNode> T executeJsonMethod(int expectedStatus, HttpMethod method)
			throws IOException, HttpException {
		return (T) JsonHelper.parse(new InputStreamReader(new ByteArrayInputStream(executeMethod(expectedStatus, method))));
	}

	public void buildProject(String source) throws IOException {
		buildProject(Collections.singletonMap("foo.tuml", source.getBytes()));
	}

	public void buildProject(Map<String, byte[]> sources) throws IOException {
		HttpMethod uploadRequest = buildMultipartRequest(getProjectUri(),
				sources);
		try {
			Assert.assertEquals(200, httpClient.executeMethod(uploadRequest));
			String responseText = uploadRequest.getResponseBodyAsString();
			Assert.assertTrue(responseText, responseText.startsWith("<results status=\"success\""));
		} finally {
			uploadRequest.releaseConnection();
		}
	}

	public URI getProjectUri() {
		return URI.create("http://localhost" + WebFrontEnd.PUBLISHER_PATH + getWorkspaceName() + '/');
	}
	
	public URI getApiUri() throws IOException, HttpException {
		return URI.create("http://localhost" + WebFrontEnd.APP_API_PATH + getWorkspaceName() + "/");
	}
	
	public URI getDeployerUri() throws IOException, HttpException {
		return URI.create("http://localhost" + WebFrontEnd.DEPLOYER_PATH + "?path=/test/" + getWorkspaceName() + "/mdd.properties");
	}

	
	public String getWorkspaceName() {
		return workspaceName;
	}

	public void dispose() throws Exception {
		// cleans up repository
		DeleteMethod delete = new DeleteMethod(getProjectUri()
				.toASCIIString());
		try {
			httpClient.executeMethod(delete);
		} finally {
			delete.releaseConnection();
		}
	}

	public void initDB() throws IOException {
		PostMethod init = new PostMethod(getApiUri().resolve(Paths.DATA).toString());
		try {
			int result = httpClient.executeMethod(init);
			Assert.assertTrue(result == HttpURLConnection.HTTP_OK || result == HttpURLConnection.HTTP_NOT_FOUND);
		} finally {
			init.releaseConnection();
		}
	}

}
