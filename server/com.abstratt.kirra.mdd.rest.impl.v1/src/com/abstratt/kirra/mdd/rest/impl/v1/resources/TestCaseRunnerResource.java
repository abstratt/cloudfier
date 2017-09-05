package com.abstratt.kirra.mdd.rest.impl.v1.resources;

import java.net.URLDecoder;

import org.restlet.representation.Representation;
import org.restlet.resource.Post;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.UnsupportedEncodingException;

public class TestCaseRunnerResource extends AbstractTestRunnerResource {

    @Post
    public Representation runTest(Representation noneExpected) throws UnsupportedEncodingException {
        final String testClassName = URLDecoder.decode((String) getRequestAttributes().get("testClassName"), UTF_8.name());
        final String testCaseName = URLDecoder.decode((String) getRequestAttributes().get("testCaseName"), UTF_8.name());
        TestResult testResult = runTestCaseAndRollback(new TestCase(testClassName, testCaseName));
        return jsonToStringRepresentation(testResult);
    }
}
