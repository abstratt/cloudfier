package com.abstratt.mdd.frontend.web.tests;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class PublisherTests extends AbstractWebFrontEndTest {
	public PublisherTests(String name) {
		super(name);
	}

	public void testPublishValidSingleUnit() throws HttpException, IOException {
		String source = "";
		source += "package foo;\n";
		source += "class Class1\n";
		source += "end;\n";
		source += "end.\n";
		HttpMethod uploadRequest = buildUploadRequest(getTestProjectURI(), Collections.singletonMap("foo.tuml", source.getBytes()));
		HttpClient client = new HttpClient();
		int result = client.executeMethod(uploadRequest);
		String uploadResponseText = uploadRequest.getResponseBodyAsString();
		assertEquals(uploadResponseText, 200, result);
		assertTrue(uploadResponseText, uploadResponseText.startsWith("<results status=\"success\""));
		
		HttpMethod getRequest = buildGetRequest(getTestProjectURI());
		result = client.executeMethod(getRequest);
		String getResponseText = getRequest.getResponseBodyAsString();
		assertEquals(getResponseText, 200, result);
		assertTrue(getResponseText, getResponseText.startsWith("<workspace packageCount='1'"));
	}
	
	public void testGetWorkspaceClasses() throws HttpException, IOException, SAXException, ParserConfigurationException {
		String source = "";
		source += "package foo;\n";
		source += "class Class1\n";
		source += "end;\n";
		source += "class Class2\n";
		source += "end;\n";
		source += "end.\n";
		HttpMethod uploadRequest = buildUploadRequest(getTestProjectURI(), Collections.singletonMap("foo.tuml", source.getBytes()));
		HttpClient client = new HttpClient();
		int result = client.executeMethod(uploadRequest);
		String uploadResponseText = uploadRequest.getResponseBodyAsString();
		assertEquals(uploadResponseText, 200, result);
		
		HttpMethod getRequest = buildGetRequest(URI.create(getTestProjectURI().toASCIIString() + "?classes=true"));
		result = client.executeMethod(getRequest);
		String getResponseText = getRequest.getResponseBodyAsString();
		assertEquals(getResponseText, 200, result);

		Document response = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(getResponseText)));
		NodeList classElements = response.getDocumentElement().getElementsByTagName("class");
		assertEquals(2, classElements.getLength());
		Collection<String> expected = new ArrayList<String>(Arrays.asList("foo::Class1", "foo::Class2"));
		for (int i = 0; i < classElements.getLength(); i++) {
			Element classElement = (Element) classElements.item(i);
			assertEquals("foo.tuml", classElement.getAttribute("unit.name"));
			String className = classElement.getAttribute("name");
			assertTrue(className, expected.remove(className));
		}
	}
	
//	public void testGetWorkspaceGenerators() throws HttpException, IOException, SAXException, ParserConfigurationException {
//		Properties properties = new Properties();
//		properties.put("mdd.target.engine","gstring");
//		properties.put("mdd.target.foo.someprop1","gstring");
//		properties.put("mdd.target.bar.someprop2","gstring");
//		StringWriter propertiesContents = new StringWriter();
//		properties.store(propertiesContents, "");
//		 
//		HttpMethod uploadRequest = buildUploadRequest(getTestProjectURI(), Collections.singletonMap("mdd.properties", propertiesContents.toString().getBytes()));
//		HttpClient client = new HttpClient();
//		int result = client.executeMethod(uploadRequest);
//		String uploadResponseText = uploadRequest.getResponseBodyAsString();
//		assertEquals(uploadResponseText, 200, result);
//		
//		HttpMethod getRequest = buildGetRequest(URI.create(getTestProjectURI().toASCIIString() + "?classes=true"));
//		result = client.executeMethod(getRequest);
//		String getResponseText = getRequest.getResponseBodyAsString();
//		assertEquals(getResponseText, 200, result);
//
//		Document response = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(getResponseText)));
//		NodeList generatorElements = response.getDocumentElement().getElementsByTagName("generator");
//		Collection<String> expected = TargetCore.getPlatformIds(properties);
//		assertTrue(expected.contains("foo"));
//		assertTrue(expected.contains("bar"));
//		assertTrue(generatorElements.getLength() >= 2);
//		assertEquals(expected.size(), generatorElements.getLength());
//		for (int i = 0; i < generatorElements.getLength(); i++) {
//			Element generatorElement = (Element) generatorElements.item(i);
//			String platformId = generatorElement.getAttribute("platform");
//			assertTrue(platformId, expected.remove(platformId));
//		}
//	}

	
	public void testDeleteRepository() throws HttpException, IOException {
		String source = "";
		source += "package foo;\n";
		source += "class Class1\n";
		source += "end;\n";
		source += "end.\n";
		executeMethod(200, buildUploadRequest(getTestProjectURI(), Collections.singletonMap("foo.tuml", source.getBytes())));
		executeMethod(200, buildGetRequest(getTestProjectURI()));
		executeMethod(204, new DeleteMethod(getTestProjectURI().toASCIIString()));
		executeMethod(404, buildGetRequest(getTestProjectURI()));
	}

	public void testValidMultipleUnits() throws HttpException, IOException {
		String source1 = "";
		source1 += "package foo1;\n";
		source1 += "class Class1 specializes foo2::Class2\n";
		source1 += "end;\n";
		source1 += "class Class3\n";
		source1 += "end;\n";
		source1 += "end.\n";

		String source2 = "";
		source2 += "package foo2;\n";
		source2 += "class Class2 specializes foo1::Class3\n";
		source2 += "end;\n";
		source2 += "end.\n";

		Map<String, byte[]> sources = new HashMap<String, byte[]>();
		sources.put("class1.tuml", source1.getBytes());
		sources.put("class2.tuml", source2.getBytes());

		HttpMethod uploadRequest = buildUploadRequest(getTestProjectURI(), sources);
		HttpClient client = new HttpClient();
		int result = client.executeMethod(uploadRequest);
		String uploadResponseText = uploadRequest.getResponseBodyAsString();
		assertEquals(uploadResponseText, 200, result);
		assertTrue(uploadResponseText, uploadResponseText.startsWith("<results status=\"success\""));
		
		HttpMethod getRequest = buildGetRequest(getTestProjectURI());
		result = client.executeMethod(getRequest);
		String getResponseText = getRequest.getResponseBodyAsString();
		assertEquals(getResponseText, 200, result);
		assertTrue(getResponseText, getResponseText.startsWith("<workspace packageCount='2'"));
	}

	public void testPublishInvalidSingleUnit() throws HttpException, IOException {
		String source = "";
		HttpMethod uploadRequest = buildUploadRequest(getTestProjectURI(), Collections.singletonMap("foo.tuml", source.getBytes()));
		HttpClient client = new HttpClient();
		int result = client.executeMethod(uploadRequest);
		String responseText = uploadRequest.getResponseBodyAsString();
		assertEquals(responseText, 200, result);
		assertTrue(responseText, responseText.startsWith("<results status=\"failure\""));
		
		HttpMethod getRequest = buildGetRequest(getTestProjectURI());
		result = client.executeMethod(getRequest);
		String getResponseText = getRequest.getResponseBodyAsString();
		assertEquals(getResponseText, 200, result);
		assertTrue(getResponseText, getResponseText.startsWith("<workspace packageCount='0'"));
	}
}
