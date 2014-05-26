package com.abstratt.mdd.core.tests.runtime;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.abstratt.mdd.core.runtime.types.BooleanType;
import com.abstratt.mdd.core.runtime.types.IntegerType;
import com.abstratt.mdd.core.runtime.types.RealType;
import com.abstratt.mdd.core.runtime.types.StringType;

public class RuntimeStringTests extends AbstractRuntimeTests {

	public static Test suite() {
		return new TestSuite(RuntimeStringTests.class);
	}
	
	public RuntimeStringTests(String name) {
		super(name);
	}

	public void setUp() throws Exception {
		super.setUp();
	}
	public void testComparisons() {
		assertEquals(BooleanType.TRUE, runOperation(new StringType("foobar"), "equals", new StringType("foobar")));
		assertEquals(BooleanType.FALSE, runOperation(new StringType("foobar"), "equals", new StringType("foobara")));
		assertEquals(BooleanType.TRUE, runOperation(new StringType("foobar"), "notEquals", new StringType("foobara")));
		assertEquals(BooleanType.FALSE, runOperation(new StringType("foobar"), "notEquals", new StringType("foobar")));
		assertEquals(BooleanType.TRUE, runOperation(new StringType("foobar"), "greaterThan", new StringType("barfoo")));
		assertEquals(BooleanType.FALSE, runOperation(new StringType("barfoo"), "greaterThan", new StringType("foobar")));
		assertEquals(BooleanType.TRUE, runOperation(new StringType("barfoo"), "lowerThan", new StringType("foobar")));
		assertEquals(BooleanType.FALSE, runOperation(new StringType("foobar"), "lowerThan", new StringType("barfoo")));
	}
	
	@Override
	protected void originalRunTest() throws Throwable {
		// invoke explicitly - can't rely on #compilationCompleted as we don't compile anything here
		setupRuntime();
		super.originalRunTest();
	}
	
	public void testConcat() {
		assertEquals(new StringType("foo10"), runOperation(new StringType("foo"), "add", runOperation(new IntegerType(10),"toString"))); 
	}
	
	public void testSize() {
		assertEquals(new IntegerType(6), runOperation(new StringType("foobar"), "size"));
	}
	
	public void testSubstring() {
		assertEquals(new StringType("oba"), runOperation(new StringType("foobar"), "substring", new IntegerType(2), new IntegerType(5)));
	}


	public void testToString()  {
		assertEquals(new StringType("foo"), runOperation(new StringType("foo"),"toString"));
		assertEquals(new StringType("17"), runOperation(new IntegerType(17),"toString"));
		assertEquals(new StringType("false"), runOperation(BooleanType.fromValue(false),"toString"));
		assertEquals(new StringType("17.5"), runOperation(new RealType(17.5),"toString"));
	}

}
