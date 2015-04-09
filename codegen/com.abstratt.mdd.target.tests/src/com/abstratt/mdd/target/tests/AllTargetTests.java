package com.abstratt.mdd.target.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTargetTests {
    public static Test suite() {
        TestSuite suite = new TestSuite(AllTargetTests.class.getName());
        suite.addTest(com.abstratt.mdd.target.tests.AllTests.suite());
        suite.addTest(com.abstratt.mdd.target.tests.jee.AllJEETests.suite());
        // see issue abstratt/cloudfier/#33
        //suite.addTest(com.abstratt.mdd.target.tests.mean.AllMeanTests.suite());
        return suite;
    }
}
