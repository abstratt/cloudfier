package com.abstratt.kirra.tests.performance;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllKirraPerformanceTests {
	public static Test suite() {
		TestSuite suite = new TestSuite(AllKirraPerformanceTests.class.getName());
		suite.addTest(new TestSuite(InstancePerformanceTests.class));
		return suite;
	}
}
