package com.abstratt.mdd.frontend.web.tests;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonParser.Feature;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.abstratt.mdd.frontend.web.BuildDirectoryUtils;
import com.abstratt.mdd.frontend.web.WebFrontEnd;

public class BuilderTests extends AbstractWebFrontEndTest {
	public BuilderTests(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	private static final URI BUILDER_URI = URI.create("http://localhost" + WebFrontEnd.BUILDER_PATH);
	
    private static JsonFactory jsonFactory = new JsonFactory();
    static {
        jsonFactory.configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        jsonFactory.configure(Feature.ALLOW_SINGLE_QUOTES, true);
        jsonFactory.configure(Feature.ALLOW_COMMENTS, true);
        jsonFactory.configure(Feature.CANONICALIZE_FIELD_NAMES, false);
    }
    
    @Override
    protected void tearDown() throws Exception {
    	super.tearDown();
    	File buildDirectory = BuildDirectoryUtils.getDeployDirectory(new Path(getName()));
    	FileUtils.deleteQuietly(buildDirectory.getParentFile());
    }

    // BuilderResource now relies on Orion metadata, not testable in isolation
	public void testBuilder() throws HttpException, IOException, CoreException {
		IPath projectPath = new Path(getName() + "/s1/s2/s3/");
		
		URI baseRequestURI = BUILDER_URI.resolve("file/" + projectPath);
		
		String validSource = "";
		validSource += "package foo;\n";
		validSource += "class Class1\n";
		validSource += "end;\n";
		validSource += "end.\n";
		IFileStore projectArea = BuildDirectoryUtils.getSourcePath(projectPath);
		FileUtils.write(projectArea.getChild("valid.tuml").toLocalFile(EFS.NONE, null), validSource);
		FileUtils.write(projectArea.getChild("invalid.tuml").toLocalFile(EFS.NONE, null), "foobar");
		
		File buildDirectory = BuildDirectoryUtils.getDeployDirectory(projectPath);

		assertFalse(buildDirectory.toString(), buildDirectory.exists());
		
		HttpClient client = new HttpClient();
		HttpMethod buildMethod = new GetMethod();
		
		buildMethod.setURI(new org.apache.commons.httpclient.URI(baseRequestURI.resolve("valid.tuml").toString(), false));
		int result = client.executeMethod(buildMethod);
		String responseText = buildMethod.getResponseBodyAsString();
		assertEquals(responseText, 200, result);
		assertEquals(0, ((ArrayNode) parse(new StringReader(responseText)).path("problems")).size());

		assertTrue(buildDirectory.toString(), buildDirectory.exists());
		
		buildMethod.setURI(new org.apache.commons.httpclient.URI(baseRequestURI.resolve("invalid.tuml").toString(), false));
		result = client.executeMethod(buildMethod);
		responseText = buildMethod.getResponseBodyAsString();
		
		assertEquals(responseText, 200, result);
		
		JsonNode parsedResponse = parse(new StringReader(responseText));
		assertNotNull(parsedResponse.get("problems"));
		assertTrue(parsedResponse.get("problems") instanceof ArrayNode);
		ArrayNode problems = (ArrayNode) parsedResponse.get("problems");
		assertEquals(1, problems.size());
		assertEquals(1, problems.get(0).get("line").asLong());
		assertEquals(1, problems.get(0).get("character").asLong());
		assertEquals(1, problems.get(0).get("end").asLong());
		assertEquals("error", problems.get(0).get("severity").asText());
		assertTrue(problems.get(0).get("reason").asText().contains("foobar"));
	}
	
    public static <T extends JsonNode> T parse(Reader contents) throws IOException, JsonParseException, JsonProcessingException {
        JsonParser parser = jsonFactory.createJsonParser(contents);
        parser.setCodec(new ObjectMapper());
        return (T) parser.readValueAsTree();
    }
}
