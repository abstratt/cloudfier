package com.abstratt.mdd.target.tests.mean;

import com.abstratt.mdd.core.tests.harness.AssertHelper;
import com.abstratt.mdd.core.util.ActivityUtils;
import com.abstratt.mdd.target.mean.JSGenerator;
import com.abstratt.mdd.target.tests.AbstractGeneratorTest;
import java.io.IOException;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.xtend2.lib.StringConcatenation;

@SuppressWarnings("all")
public class JSGeneratorTests extends AbstractGeneratorTest {
  private JSGenerator generator = new JSGenerator();
  
  public static Test suite() {
    return new TestSuite(JSGeneratorTests.class);
  }
  
  public JSGeneratorTests(final String name) {
    super(name);
  }
  
  public void testDateDifferenceInDays() throws CoreException, IOException {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("model simple;");
    _builder.newLine();
    _builder.append("  ");
    _builder.append("class Class1");
    _builder.newLine();
    _builder.append("      ");
    _builder.append("attribute date : Date;");
    _builder.newLine();
    _builder.append("      ");
    _builder.append("query difference() : Integer;");
    _builder.newLine();
    _builder.append("      ");
    _builder.append("begin");
    _builder.newLine();
    _builder.append("          ");
    _builder.append("return self.date.differenceInDays(Date#now());");
    _builder.newLine();
    _builder.append("      ");
    _builder.append("end; ");
    _builder.newLine();
    _builder.append("  ");
    _builder.append("end;");
    _builder.newLine();
    _builder.append("end.");
    _builder.newLine();
    String source = _builder.toString();
    this.parseAndCheck(source);
    final Operation operation = this.getOperation("simple::Class1::difference");
    Activity _activity = ActivityUtils.getActivity(operation);
    CharSequence _generateActivity = this.generator.generateActivity(_activity);
    final String mapped = _generateActivity.toString();
    StringConcatenation _builder_1 = new StringConcatenation();
    _builder_1.append("{");
    _builder_1.newLine();
    _builder_1.append("    ");
    _builder_1.append("return (new Date() - this.date) / (1000*60*60*24);    ");
    _builder_1.newLine();
    _builder_1.append("}");
    _builder_1.newLine();
    AssertHelper.assertStringsEqual(_builder_1.toString(), mapped);
  }
  
  public void testDateTranspose() throws CoreException, IOException {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("model simple;");
    _builder.newLine();
    _builder.append("  ");
    _builder.append("class Class1");
    _builder.newLine();
    _builder.append("      ");
    _builder.append("attribute date : Date;");
    _builder.newLine();
    _builder.append("      ");
    _builder.append("query nextWeek() : Date;");
    _builder.newLine();
    _builder.append("      ");
    _builder.append("begin");
    _builder.newLine();
    _builder.append("          ");
    _builder.append("return self.date.transpose(Duration#days(7));");
    _builder.newLine();
    _builder.append("      ");
    _builder.append("end; ");
    _builder.newLine();
    _builder.append("  ");
    _builder.append("end;");
    _builder.newLine();
    _builder.append("end.");
    _builder.newLine();
    String source = _builder.toString();
    this.parseAndCheck(source);
    final Operation operation = this.getOperation("simple::Class1::nextWeek");
    Activity _activity = ActivityUtils.getActivity(operation);
    CharSequence _generateActivity = this.generator.generateActivity(_activity);
    final String mapped = _generateActivity.toString();
    StringConcatenation _builder_1 = new StringConcatenation();
    _builder_1.append("{");
    _builder_1.newLine();
    _builder_1.append("    ");
    _builder_1.append("return new Date(this.date + 7 * 1000 * 60 * 60 * 24 /* days */);    ");
    _builder_1.newLine();
    _builder_1.append("}");
    _builder_1.newLine();
    AssertHelper.assertStringsEqual(_builder_1.toString(), mapped);
  }
}
