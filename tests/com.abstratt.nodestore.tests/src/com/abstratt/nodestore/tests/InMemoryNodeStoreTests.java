package com.abstratt.nodestore.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

public class InMemoryNodeStoreTests extends AbstractNodeStoreTests {

    public static Test suite() {
        return new TestSuite(InMemoryNodeStoreTests.class);
    }

    public InMemoryNodeStoreTests(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected String getFactoryName() {
        return "inmemory";
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
}
