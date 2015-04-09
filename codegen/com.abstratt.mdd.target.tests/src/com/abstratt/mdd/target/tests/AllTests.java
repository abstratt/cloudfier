package com.abstratt.mdd.target.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
    public static Test suite() {
        TestSuite suite = new TestSuite(AllTests.class.getName());
//        suite.addTest(CustomPlatformTests.suite());
//        suite.addTest(STLanguageMapperTests.suite());
        suite.addTest(GStringLanguageMapperTests.suite());
        suite.addTest(BasicTargetTests.suite());
        return suite;
    }
}
