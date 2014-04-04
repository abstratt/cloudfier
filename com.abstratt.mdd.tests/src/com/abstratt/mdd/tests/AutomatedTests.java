package com.abstratt.mdd.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.abstratt.mdd.core.tests.AllCoreTests;
import com.abstratt.mdd.core.tests.runtime.AllRuntimeTests;
import com.abstratt.mdd.core.tests.textuml.AllTextUMLTests;
import com.abstratt.mdd.target.tests.AllTargetTests;

public class AutomatedTests {
	public static Test suite() {
		TestSuite suite = new TestSuite(AutomatedTests.class.getName());
		suite.addTest(AllCoreTests.suite());
		suite.addTest(AllRuntimeTests.suite());
		suite.addTest(AllTextUMLTests.suite());
		suite.addTest(AllTargetTests.suite());
		return suite;
	}
}
