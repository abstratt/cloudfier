package com.abstratt.nodestore.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllNodeStoreTests {
	public static Test suite() {
		TestSuite suite = new TestSuite(AllNodeStoreTests.class.getName());
		suite.addTest(JdbcNodeStoreTests.suite());
		suite.addTest(SQLGeneratorTests.suite());
		return suite;
	}

}
