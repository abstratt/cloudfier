package com.abstratt.mdd.frontend.web.tests;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import junit.framework.Assert;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;

import com.abstratt.mdd.core.tests.harness.AssertHelper;
import com.abstratt.mdd.frontend.web.WebFrontEnd;

public class GeneratorTests extends AbstractWebFrontEndTest {

    public GeneratorTests(String name) {
        super(name);
        // TODO Auto-generated constructor stub
    }

    /**
     * Tests a valid generation.
     */
    public void testCustomPlatform() throws HttpException, IOException {
        Properties properties = new Properties();
        properties.put("mdd.target.engine", "gstring");
        properties.put("mdd.target.bar.template", "bar.gt");
        StringWriter propertiesContents = new StringWriter();
        properties.store(propertiesContents, "");

        String source = "";
        source += "package foo;\n";
        source += "class Bar\n";
        source += "end;\n";
        source += "end.\n";

        String template = "";
        template += "String generate(def clazz) {\n";
        template += "\"hello, ${clazz.name}\"\n";
        template += "}\n";

        Map<String, byte[]> sources = new HashMap<String, byte[]>();
        sources.put("mdd.properties", propertiesContents.toString().getBytes());
        sources.put("foo.tuml", source.getBytes());
        sources.put("bar.gt", template.getBytes());
        buildProject(sources);
        String generated = new String(generate("bar", null));
        Assert.assertTrue(generated, AssertHelper.areEqual("hello, Bar", generated));
    }

    public void testMissingPlatform() throws HttpException, IOException {
        String source = "";
        source += "package foo;\n";
        source += "class Class\n";
        source += "end;\n";
        source += "end.\n";
        buildProject(source);
        GetMethod generateRequest = new GetMethod(getTestGeneratorURI().resolve("non-existing").toASCIIString());
        executeMethod(404, generateRequest);
    }

    public void testMissingWorkspace() throws HttpException, IOException {
        GetMethod generateRequest = new GetMethod(getTestGeneratorURI().resolve("pojo").toASCIIString());
        executeMethod(404, generateRequest);
    }

    /**
     * Tests a valid generation.
     */
    // public void testGeneratePOJO() throws HttpException, IOException {
    // String source = "";
    // source += "package foo;\n";
    // source += "class Class\n";
    // source += "end;\n";
    // source += "end.\n";
    // buildProject(source);
    // String generated = new String(generate("pojo", null));
    // Assert.assertTrue(generated, AssertHelper.areEqual(
    // "package foo; public class Class {  }", generated));
    // }

    // public void testGenerateCustomMultiple() throws HttpException,
    // IOException {
    // String source = "";
    // source += "package foo;\n";
    // source += "class Class1\n";
    // source += "end;\n";
    // source += "class Class2\n";
    // source += "end;\n";
    // source += "end.\n";
    // buildProject(source);
    // String generated = new String(generate("pojo", "text/plain"));
    // Assert.assertTrue(
    // generated,
    // AssertHelper
    // .areEqual(
    // "package foo; public class Class1 {  } package foo; public class Class2 {  }",
    // generated));
    // }
    //
    // public void testGenerateSpecificPOJO() throws HttpException, IOException
    // {
    // String source = "";
    // source += "package foo;\n";
    // source += "class Class1\n";
    // source += "end;\n";
    // source += "class Class2\n";
    // source += "end;\n";
    // source += "end.\n";
    // buildProject(source);
    // String generated = new String(generate("pojo", "text/plain",
    // "foo::Class2"));
    // Assert.assertTrue(generated, AssertHelper.areEqual(
    // "package foo; public class Class2 {  }", generated));
    // }
    //
    // public void testGenerateZippedPOJOs() throws HttpException, IOException {
    // String source = "";
    // source += "package foo;\n";
    // source += "class Class1\n";
    // source += "end;\n";
    // source += "class Class2\n";
    // source += "end;\n";
    // source += "end.\n";
    // buildProject(source);
    // byte[] generated = generate("pojo", "application/zip");
    // File output = File.createTempFile("test", null);
    // Map<String, String> contents = new LinkedHashMap<String, String>();
    // try {
    // FileUtils.writeByteArrayToFile(output, generated);
    // ZipFile zipFile = new ZipFile(output);
    // try {
    // Enumeration<? extends ZipEntry> e = zipFile.entries();
    // while (e.hasMoreElements()) {
    // ZipEntry entry = (ZipEntry) e.nextElement();
    // InputStream entryContents = zipFile.getInputStream(entry);
    // try {
    // contents.put(entry.getName(),
    // IOUtils.toString(entryContents));
    // } finally {
    // IOUtils.closeQuietly(entryContents);
    // }
    // }
    // } finally {
    // zipFile.close();
    // }
    // } finally {
    // output.delete();
    // }
    // Assert.assertEquals(2, contents.size());
    // String class1 = contents.get("foo/Class1.java");
    // Assert.assertNotNull(class1);
    // Assert.assertTrue(class1, AssertHelper.areEqual(
    // "package foo; public class Class1 {  }", class1));
    //
    // String class2 = contents.get("foo/Class2.java");
    // Assert.assertNotNull(class2);
    // Assert.assertTrue(class2, AssertHelper.areEqual(
    // "package foo; public class Class2 {  }", class2));
    // }

    private byte[] generate(String platform, String expectedType, String... classes) throws HttpException, IOException {
        GetMethod generateRequest = new GetMethod(getTestGeneratorURI().resolve(platform).toASCIIString());
        List<NameValuePair> values = new ArrayList<NameValuePair>();
        for (String toGenerate : classes)
            values.add(new NameValuePair("class", toGenerate));
        generateRequest.setQueryString(values.toArray(new NameValuePair[0]));
        if (expectedType != null)
            generateRequest.setRequestHeader("Content-Type", expectedType);
        return executeMethod(200, generateRequest);
    }

    private URI getTestGeneratorURI() {
        return URI.create("http://localhost" + WebFrontEnd.GENERATOR_PATH + getName() + "/");
    }

}
