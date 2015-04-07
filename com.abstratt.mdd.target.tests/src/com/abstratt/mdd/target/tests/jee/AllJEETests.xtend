package com.abstratt.mdd.target.tests.jee

import junit.framework.Test
import junit.framework.TestSuite

class AllJEETests {
    def static Test suite() {
        val suite = new TestSuite(AllJEETests.getName())
        suite.addTestSuite(QueryActionGeneratorTests)

        return suite
    }     
}