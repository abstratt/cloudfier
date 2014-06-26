package com.abstratt.nodestore.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

public class JdbcNodeStoreTests extends AbstractNodeStoreTests {

    public static Test suite() {
        return new TestSuite(JdbcNodeStoreTests.class);
    }

    public JdbcNodeStoreTests(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected String getFactoryName() {
        return "jdbc";
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
}
