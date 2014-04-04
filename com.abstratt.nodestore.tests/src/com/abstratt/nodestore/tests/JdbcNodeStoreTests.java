package com.abstratt.nodestore.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

public class JdbcNodeStoreTests extends AbstractNodeStoreTests {

	public JdbcNodeStoreTests(String name) {
		super(name);
	}
	
	@Override
	protected String getFactoryName() {
		return "jdbc"; 
	}
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public static Test suite() {
		return new TestSuite(JdbcNodeStoreTests.class);
	}
}
