package com.abstratt.mdd.frontend.web.tests;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;

import com.abstratt.mdd.frontend.web.WebFrontEnd;

public class ValidatorTests extends AbstractWebFrontEndTest {
	public ValidatorTests(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	private static final URI VALIDATOR_URI = URI.create("http://localhost" + WebFrontEnd.VALIDATOR_PATH + "?mdd.enableExtensions=true&mdd.enableLibraries=true");

	public void testValidSingleUnit() throws HttpException, IOException {
		String source = "";
		source += "package foo;\n";
		source += "class Class1\n";
		source += "end;\n";
		source += "end.\n";
		HttpMethod method = buildUploadRequest(VALIDATOR_URI, Collections.singletonMap("foo.tuml", source.getBytes("UTF-8")));
		HttpClient client = new HttpClient();
		int result = client.executeMethod(method);
		String responseText = method.getResponseBodyAsString();
		assertEquals(responseText, 200, result);
		assertTrue(responseText, responseText.startsWith("<results status=\"success\""));
	}
	
	/** @see 0000131: unit contents must be encoded/decoded when validating/publishing */
	public void testEncoding() throws HttpException, IOException {
		String source = "";
		source += "package foo;\n";
		source += "import mdd_types;\n";
		source += "class Class1\n";
		source += "attribute attr1 : Integer;\n";
		source += "operation op1();\n";
		source += "begin\n";
        source += "self.attr1 := 1 + self.attr1;\n"; 		
		source += "end;\n";
		source += "end;\n";
		source += "end.\n";
		HttpMethod method = buildUploadRequest(VALIDATOR_URI, Collections.singletonMap("foo.tuml", source.getBytes("UTF-8")));
		HttpClient client = new HttpClient();
		int result = client.executeMethod(method);
		String responseText = method.getResponseBodyAsString();
		assertEquals(responseText, 200, result);
		assertTrue(responseText, responseText.startsWith("<results status=\"success\""));
	}

	public void testValidMultipleUnits() throws HttpException, IOException {
		String source1 = "";
		source1 += "package foo;\n";
		source1 += "class Class1 specializes Class2\n";
		source1 += "end;\n";
		source1 += "class Class3\n";
		source1 += "end;\n";
		source1 += "end.\n";

		String source2 = "";
		source2 += "package foo;\n";
		source2 += "class Class2 specializes Class3\n";
		source2 += "end;\n";
		source2 += "end.\n";

		Map<String, byte[]> sources = new HashMap<String, byte[]>();
		sources.put("class1.tuml", source1.getBytes("UTF-8"));
		sources.put("class2.tuml", source2.getBytes("UTF-8"));

		HttpMethod method = buildUploadRequest(VALIDATOR_URI, sources);
		HttpClient client = new HttpClient();
		int result = client.executeMethod(method);
		String responseText = method.getResponseBodyAsString();
		assertEquals(responseText, 200, result);
		assertTrue(responseText, responseText.startsWith("<results status=\"success\""));
	}

	public void testInvalidSingleUnit() throws HttpException, IOException {
		String source = "foobar";
		HttpMethod method = buildUploadRequest(VALIDATOR_URI, Collections.singletonMap("foo.tuml", source.getBytes("UTF-8")));
		HttpClient client = new HttpClient();
		int result = client.executeMethod(method);
		String responseText = method.getResponseBodyAsString();
		assertEquals(responseText, 200, result);
		assertTrue(responseText, responseText.startsWith("<results status=\"failure\""));
	}
}
