package com.abstratt.mdd.core.tests.runtime;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllRuntimeTests {
	
    public def static Test suite() {
        val TestSuite suite = new TestSuite(AllRuntimeTests.name);
        suite.addTest(PrimitiveActionTests.suite());
        suite.addTest(ExpressionTests.suite());
        suite.addTest(RuntimeStringTests.suite());
        suite.addTest(RuntimeDateAndTimeTests.suite());
        suite.addTest(RuntimeObjectTests.suite());
        suite.addTest(RuntimeEnumerationTests.suite());
        suite.addTest(RuntimeControlTests.suite());
        suite.addTest(RuntimeCollectionTests.suite());
        suite.addTest(RuntimeAssociationTests.suite());
        suite.addTest(RuntimeStateMachineTests.suite());
        suite.addTest(RuntimeReceptionTests.suite());
        suite.addTest(RuntimePortTests.suite());
        suite.addTest(RuntimeUserTests.suite());
        suite.addTest(RuntimeExternalTests.suite());
        suite.addTest(SSMTests.suite());
        return suite;
    }
}
