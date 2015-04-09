package com.abstratt.mdd.target.tests.mean

import junit.framework.Test
import junit.framework.TestSuite

public class AllMeanTests {
    def static Test suite() {
        val suite = new TestSuite(AllMeanTests.getName())
        suite.addTest(ModelGeneratorTests.suite())
        suite.addTest(JSGeneratorTests.suite())
        suite.addTest(PipelineTests.suite())
        return suite
    } 
}
