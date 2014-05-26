package com.abstratt.mdd.target.tests;

import static com.abstratt.mdd.core.tests.harness.AssertHelper.trim;
import junit.framework.TestCase;

public class AssertHelperTests extends TestCase {
	public void testSingleChar() {
		assertEquals("", trim(""));
		assertEquals("a", trim("a"));
		assertEquals("a", trim("a "));
		assertEquals("a", trim(" a "));
	}
	
	public void testOneWord() {
		assertEquals("foo", trim("foo"));
		assertEquals("foo", trim("foo "));
		assertEquals("foo", trim(" foo"));
		assertEquals("foo", trim(" foo "));
	}
	
	public void testOneWordWithMultipleSpaces() {
		assertEquals("foo", trim("foo  "));
		assertEquals("foo", trim("  foo"));
		assertEquals("foo", trim("  foo  "));
	}

	public void testTwoWord() {
		assertEquals("foo bar", trim("foo bar"));
		assertEquals("foo bar", trim("foo bar "));
		assertEquals("foo bar", trim(" foo bar"));
		assertEquals("foo bar", trim(" foo bar "));
	}
	
	public void testTwoWordWithMultipleSpaces() {
		assertEquals("foo bar", trim("foo  bar  "));
		assertEquals("foo bar", trim("  foo  bar"));
		assertEquals("foo bar", trim("  foo  bar  "));
	}
	
	public void testComplexExpressions() {
		assertEquals("(4+5.47)", trim("( 4 + 5.47 )"));
		assertEquals("4+(5.47)*2", trim("4 +(5.47) * 2"));
		assertEquals("int sum=0;for(Integer e:listOfInts){sum=e+1;}", trim("int sum = 0;\nfor ( Integer e : listOfInts ) {\n\tsum = e + 1 ;\n}"));
	}


}
