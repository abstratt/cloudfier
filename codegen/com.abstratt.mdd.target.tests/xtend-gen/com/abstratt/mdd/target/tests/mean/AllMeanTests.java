package com.abstratt.mdd.target.tests.mean;

import com.abstratt.mdd.target.tests.mean.JSGeneratorTests;
import com.abstratt.mdd.target.tests.mean.ModelGeneratorTests;
import com.abstratt.mdd.target.tests.mean.PipelineTests;
import junit.framework.Test;
import junit.framework.TestSuite;

@SuppressWarnings("all")
public class AllMeanTests {
  public static Test suite() {
    String _name = AllMeanTests.class.getName();
    final TestSuite suite = new TestSuite(_name);
    Test _suite = ModelGeneratorTests.suite();
    suite.addTest(_suite);
    Test _suite_1 = JSGeneratorTests.suite();
    suite.addTest(_suite_1);
    Test _suite_2 = PipelineTests.suite();
    suite.addTest(_suite_2);
    return suite;
  }
}
