package com.abstratt.kirra.tests.mdd.runtime;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllKirraMDDRuntimeTests {
    public static Test suite() {
        TestSuite suite = new TestSuite(AllKirraMDDRuntimeTests.class.getName());
        suite.addTest(new TestSuite(TupleParserTests.class));
        suite.addTest(new TestSuite(RepositoryServiceTests.class));
        suite.addTest(new TestSuite(KirraMDDRuntimeResourceTests.class));
        suite.addTest(new TestSuite(KirraMDDRuntimeSchemaTests.class));
        suite.addTest(new TestSuite(KirraMDDRuntimeActorTests.class));
        suite.addTest(new TestSuite(KirraMDDRuntimeDataTests.class));
        suite.addTest(new TestSuite(KirraMDDRuntimeActionTests.class));
        suite.addTest(new TestSuite(KirraMDDRuntimeRelationshipTests.class));
        suite.addTest(new TestSuite(KirraMDDRuntimeValidationTests.class));
        suite.addTest(new TestSuite(KirraMDDRuntimeExternalServiceTests.class));
//        suite.addTest(new TestSuite(KirraUIHelperTests.class));
        suite.addTest(new TestSuite(KirraMDDRuntimeRest2Tests.class));
        suite.addTest(new TestSuite(KirraMDDRuntimeRestTests.class));
        suite.addTest(new TestSuite(KirraDataPopulatorTests.class));
        return suite;
    }
}
