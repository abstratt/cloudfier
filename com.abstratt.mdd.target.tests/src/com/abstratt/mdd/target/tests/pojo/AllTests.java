package com.abstratt.mdd.target.tests.pojo;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
    public static Test suite() {
        TestSuite suite = new TestSuite(AllTests.class.getName());
        suite.addTest(POJOStructureTests.suite());
        suite.addTest(POJOBehaviorTests.suite());
        return suite;
    }
}
