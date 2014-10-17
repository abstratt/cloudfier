package com.abstratt.mdd.target.mean.tests

import junit.framework.Test
import junit.framework.TestSuite

public class AllTests {
    def static Test suite() {
        val suite = new TestSuite(AllTests.getName())
        suite.addTest(ModelGeneratorTests.suite())
        suite.addTest(JSGeneratorTests.suite())
        return suite
    } 
}
