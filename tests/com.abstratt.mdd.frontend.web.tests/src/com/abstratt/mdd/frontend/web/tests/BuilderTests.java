package com.abstratt.mdd.frontend.web.tests;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;

import junit.framework.TestCase;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.abstratt.mdd.frontend.web.BuildDirectoryUtils;
import com.abstratt.mdd.frontend.web.WebFrontEnd;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class BuilderTests extends AbstractWebFrontEndTest {
    public static <T extends JsonNode> T parse(Reader contents) throws IOException, JsonParseException, JsonProcessingException {
        JsonParser parser = BuilderTests.jsonFactory.createJsonParser(contents);
        parser.setCodec(new ObjectMapper());
        return (T) parser.readValueAsTree();
    }

    private static final URI BUILDER_URI = URI.create("http://localhost" + WebFrontEnd.BUILDER_PATH);

    private static JsonFactory jsonFactory = new JsonFactory();
    static {
        BuilderTests.jsonFactory.configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        BuilderTests.jsonFactory.configure(Feature.ALLOW_SINGLE_QUOTES, true);
        BuilderTests.jsonFactory.configure(Feature.ALLOW_COMMENTS, true);
        BuilderTests.jsonFactory.configure(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES, false);
    }

    public BuilderTests(String name) {
        super(name);
        // TODO Auto-generated constructor stub
    }

    // BuilderResource now relies on Orion metadata, not testable in isolation
    public void testBuilder() throws HttpException, IOException, CoreException {
        IPath projectPath = new Path(getName() + "/s1/s2/s3/");

        URI baseRequestURI = BuilderTests.BUILDER_URI.resolve("file/" + projectPath);

        String validSource = "";
        validSource += "package foo;\n";
        validSource += "class Class1\n";
        validSource += "end;\n";
        validSource += "end.\n";
        IFileStore projectArea = BuildDirectoryUtils.getSourcePath(projectPath);
        FileUtils.writeStringToFile(projectArea.getChild("valid.tuml").toLocalFile(EFS.NONE, null), validSource);
        FileUtils.writeStringToFile(projectArea.getChild("invalid.tuml").toLocalFile(EFS.NONE, null), "foobar");

        File buildDirectory = BuildDirectoryUtils.getDeployDirectory(projectPath);

        TestCase.assertFalse(buildDirectory.toString(), buildDirectory.exists());

        HttpClient client = new HttpClient();
        HttpMethod buildMethod = new GetMethod();

        buildMethod.setURI(new org.apache.commons.httpclient.URI(baseRequestURI.resolve("valid.tuml").toString(), false));
        int result = client.executeMethod(buildMethod);
        String responseText = buildMethod.getResponseBodyAsString();
        TestCase.assertEquals(responseText, 200, result);
        TestCase.assertEquals(0, ((ArrayNode) BuilderTests.parse(new StringReader(responseText)).path("problems")).size());

        TestCase.assertTrue(buildDirectory.toString(), buildDirectory.exists());

        buildMethod.setURI(new org.apache.commons.httpclient.URI(baseRequestURI.resolve("invalid.tuml").toString(), false));
        result = client.executeMethod(buildMethod);
        responseText = buildMethod.getResponseBodyAsString();

        TestCase.assertEquals(responseText, 200, result);

        JsonNode parsedResponse = BuilderTests.parse(new StringReader(responseText));
        TestCase.assertNotNull(parsedResponse.get("problems"));
        TestCase.assertTrue(parsedResponse.get("problems") instanceof ArrayNode);
        ArrayNode problems = (ArrayNode) parsedResponse.get("problems");
        TestCase.assertEquals(1, problems.size());
        TestCase.assertEquals(1, problems.get(0).get("line").asLong());
        TestCase.assertEquals(1, problems.get(0).get("character").asLong());
        TestCase.assertEquals(1, problems.get(0).get("end").asLong());
        TestCase.assertEquals("error", problems.get(0).get("severity").asText());
        TestCase.assertTrue(problems.get(0).get("reason").asText().contains("foobar"));
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        File buildDirectory = BuildDirectoryUtils.getDeployDirectory(new Path(getName()));
        FileUtils.deleteQuietly(buildDirectory.getParentFile());
    }
}
