package com.abstratt.mdd.target.tests.jee;

import com.abstratt.mdd.target.tests.jee.QueryActionGeneratorTests;
import junit.framework.Test;
import junit.framework.TestSuite;

@SuppressWarnings("all")
public class AllJEETests {
  public static Test suite() {
    String _name = AllJEETests.class.getName();
    final TestSuite suite = new TestSuite(_name);
    suite.addTestSuite(QueryActionGeneratorTests.class);
    return suite;
  }
}
