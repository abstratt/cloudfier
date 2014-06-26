package com.abstratt.mdd.target.tests.jpa;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
    public static Test suite() {
        TestSuite suite = new TestSuite(AllTests.class.getName());
        suite.addTest(JPABasicTests.suite());
        suite.addTest(JPAQueryTests.suite());
        return suite;
    }
}
