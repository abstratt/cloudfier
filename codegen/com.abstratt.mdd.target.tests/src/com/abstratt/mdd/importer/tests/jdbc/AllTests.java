package com.abstratt.mdd.importer.tests.jdbc;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
    public static Test suite() {
        TestSuite suite = new TestSuite(AllTests.class.getName());
        suite.addTest(new TestSuite(JDBCImporterTests.class));
        return suite;
    }
}
