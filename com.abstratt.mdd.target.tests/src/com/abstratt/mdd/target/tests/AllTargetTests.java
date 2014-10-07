package com.abstratt.mdd.target.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTargetTests {
    public static Test suite() {
        TestSuite suite = new TestSuite(AllTargetTests.class.getName());
        suite.addTest(com.abstratt.mdd.target.tests.AllTests.suite());
        //suite.addTest(com.abstratt.mdd.target.tests.query.AllTests.suite());
        // suite.addTest(com.abstratt.mdd.target.tests.pojo.AllTests.suite());
        // suite.addTest(com.abstratt.mdd.target.tests.jpa.AllTests.suite());
        //suite.addTest(com.abstratt.mdd.target.tests.sql.AllTests.suite());
        return suite;
    }
}
