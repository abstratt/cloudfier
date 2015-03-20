package com.abstratt.kirra.mdd.rest.impl.v1.resources;

import org.restlet.representation.Representation;
import org.restlet.resource.Post;

public class TestCaseRunnerResource extends AbstractTestRunnerResource {

    @Post
    public Representation runTest(Representation noneExpected) {
        final String testClassName = (String) getRequestAttributes().get("testClassName");
        final String testCaseName = (String) getRequestAttributes().get("testCaseName");
        TestResult testResult = runTestCaseAndRollback(new TestCase(testClassName, testCaseName));
        return jsonToStringRepresentation(testResult);
    }
}
