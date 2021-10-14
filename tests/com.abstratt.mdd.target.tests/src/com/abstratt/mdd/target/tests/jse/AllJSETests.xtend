package com.abstratt.mdd.target.tests.jse

import junit.framework.Test
import junit.framework.TestSuite

class AllJSETests {
    def static Test suite() {
        val suite = new TestSuite(AllJSETests.getName())
        suite.addTestSuite(PlainEntityBehaviorGenerationTests)
		suite.addTestSuite(PlainEntityGenerationTests)
        return suite
    }     
}