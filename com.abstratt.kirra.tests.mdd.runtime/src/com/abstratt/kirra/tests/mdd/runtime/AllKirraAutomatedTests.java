package com.abstratt.kirra.tests.mdd.runtime;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.abstratt.mdd.core.tests.runtime.AllRuntimeTests;
import com.abstratt.mdd.frontend.web.tests.AllWebFrontEndTests;
import com.abstratt.mdd.target.tests.AllTargetTests;
import com.abstratt.nodestore.tests.AllNodeStoreTests;

public class AllKirraAutomatedTests {
    public static Test suite() {
        TestSuite suite = new TestSuite(AllKirraAutomatedTests.class.getName());
        suite.addTest(AllTargetTests.suite());
        suite.addTest(AllRuntimeTests.suite());
        suite.addTest(AllNodeStoreTests.suite());
        suite.addTest(AllKirraMDDRuntimeTests.suite());
        suite.addTest(AllWebFrontEndTests.suite());
        return suite;
    }
}
