package com.abstratt.mdd.frontend.web.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllWebFrontEndTests {
    public static Test suite() {
        TestSuite suite = new TestSuite(AllWebFrontEndTests.class.getName());
        suite.addTest(new TestSuite(StatusTests.class));
        suite.addTest(new TestSuite(ValidatorTests.class));
        // BuilderResource now relies on Orion metadata, not testable in
        // isolation
        // suite.addTest(new TestSuite(BuilderTests.class));
        suite.addTest(new TestSuite(PublisherTests.class));
        // suite.addTest(new TestSuite(GeneratorTests.class));
        return suite;
    }
}
