package com.abstratt.mdd.target.tests.mean;

import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.tests.harness.AssertHelper;
import com.abstratt.mdd.target.mean.ModelGenerator;
import com.abstratt.mdd.target.tests.AbstractGeneratorTest;
import java.io.IOException;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.xtend2.lib.StringConcatenation;

@SuppressWarnings("all")
public class ModelGeneratorTests extends AbstractGeneratorTest {
  public static Test suite() {
    return new TestSuite(ModelGeneratorTests.class);
  }
  
  public ModelGeneratorTests(final String name) {
    super(name);
  }
  
  public ModelGenerator getGenerator() {
    IRepository _repository = this.getRepository();
    return new ModelGenerator(_repository);
  }
  
  public void testSimpleModel() throws CoreException, IOException {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("model simple;");
    _builder.newLine();
    _builder.append("  ");
    _builder.append("class Class1");
    _builder.newLine();
    _builder.append("      ");
    _builder.append("attribute attr1 : String;");
    _builder.newLine();
    _builder.append("      ");
    _builder.append("attribute attr2 : Integer;");
    _builder.newLine();
    _builder.append("      ");
    _builder.append("attribute attr3 : Date[0,1];            ");
    _builder.newLine();
    _builder.append("  ");
    _builder.append("end;");
    _builder.newLine();
    _builder.append("end.");
    _builder.newLine();
    String source = _builder.toString();
    this.parseAndCheck(source);
    ModelGenerator _generator = this.getGenerator();
    org.eclipse.uml2.uml.Class _class = this.getClass("simple::Class1");
    CharSequence _generateSchema = _generator.generateSchema(_class);
    final String mapped = _generateSchema.toString();
    StringConcatenation _builder_1 = new StringConcatenation();
    _builder_1.append("var class1Schema = new Schema({ ");
    _builder_1.newLine();
    _builder_1.append("    ");
    _builder_1.append("attr1: { type: String, required: true }, ");
    _builder_1.newLine();
    _builder_1.append("    ");
    _builder_1.append("attr2: { type: Number, required: true },");
    _builder_1.newLine();
    _builder_1.append("    ");
    _builder_1.append("attr3: { type: Date }");
    _builder_1.newLine();
    _builder_1.append("}); ");
    _builder_1.newLine();
    _builder_1.append("var Class1 = mongoose.model(\'Class1\', class1Schema);      ");
    _builder_1.newLine();
    AssertHelper.assertStringsEqual(_builder_1.toString(), mapped);
  }
  
  public void testAction() throws CoreException, IOException {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("model simple;");
    _builder.newLine();
    _builder.append("class Class1");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("attribute attr1 : Integer;");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("operation incAttr1(value : Integer);");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("begin");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("self.attr1 := self.attr1 + value;");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("end;            ");
    _builder.newLine();
    _builder.append("end;");
    _builder.newLine();
    _builder.append("end.");
    _builder.newLine();
    final String source = _builder.toString();
    this.parseAndCheck(source);
    ModelGenerator _generator = this.getGenerator();
    Operation _operation = this.getOperation("simple::Class1::incAttr1");
    CharSequence _generateActionOperation = _generator.generateActionOperation(_operation);
    final String mapped = _generateActionOperation.toString();
    StringConcatenation _builder_1 = new StringConcatenation();
    _builder_1.append("class1Schema.methods.incAttr1 = function (value) {");
    _builder_1.newLine();
    _builder_1.append("    ");
    _builder_1.append("this.attr1 = this.attr1 + value; ");
    _builder_1.newLine();
    _builder_1.append("};");
    _builder_1.newLine();
    AssertHelper.assertStringsEqual(_builder_1.toString(), mapped);
  }
  
  public void testExtent() throws CoreException, IOException {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("model crm;");
    _builder.newLine();
    _builder.append("class Customer");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("attribute name : String;");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("static query allCustomers() : Customer[*];");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("begin");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("return Customer extent;");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("end;            ");
    _builder.newLine();
    _builder.append("end;");
    _builder.newLine();
    _builder.append("end.");
    _builder.newLine();
    final String source = _builder.toString();
    this.parseAndCheck(source);
    ModelGenerator _generator = this.getGenerator();
    Operation _operation = this.getOperation("crm::Customer::allCustomers");
    CharSequence _generateQueryOperation = _generator.generateQueryOperation(_operation);
    final String mapped = _generateQueryOperation.toString();
    StringConcatenation _builder_1 = new StringConcatenation();
    _builder_1.append("customerSchema.statics.allCustomers = function () {");
    _builder_1.newLine();
    _builder_1.append("    ");
    _builder_1.append("return this.model(\'Customer\').find().exec(); ");
    _builder_1.newLine();
    _builder_1.append("};");
    _builder_1.newLine();
    String _string = mapped.toString();
    AssertHelper.assertStringsEqual(_builder_1.toString(), _string);
  }
  
  public void testSelectByBooleanProperty() throws CoreException, IOException {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("model crm;");
    _builder.newLine();
    _builder.append("class Customer");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("attribute name : String;");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("attribute mvp : Boolean;");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("static query mvpCustomers() : Customer[*];");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("begin");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("return Customer extent.select((c : Customer) : Boolean { c.mvp = true});");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("end;            ");
    _builder.newLine();
    _builder.append("end;");
    _builder.newLine();
    _builder.append("end.");
    _builder.newLine();
    final String source = _builder.toString();
    this.parseAndCheck(source);
    ModelGenerator _generator = this.getGenerator();
    Operation _operation = this.getOperation("crm::Customer::mvpCustomers");
    CharSequence _generateQueryOperation = _generator.generateQueryOperation(_operation);
    final String mapped = _generateQueryOperation.toString();
    StringConcatenation _builder_1 = new StringConcatenation();
    _builder_1.append("customerSchema.statics.mvpCustomers = function () {");
    _builder_1.newLine();
    _builder_1.append("    ");
    _builder_1.append("return this.model(\'Customer\').find().where(\'mvp\').equals(true).exec(); ");
    _builder_1.newLine();
    _builder_1.append("};");
    _builder_1.newLine();
    AssertHelper.assertStringsEqual(_builder_1.toString(), mapped);
  }
  
  public void testSelectByPropertyGreaterThan() throws CoreException, IOException {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("model banking;");
    _builder.newLine();
    _builder.append("class Account");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("attribute number : String;");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("attribute balance : Double;");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("static query bestAccounts(threshold : Double) : Account[*];");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("begin");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("return Account extent.select((a : Account) : Boolean { a.balance > threshold });");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("end;            ");
    _builder.newLine();
    _builder.append("end;");
    _builder.newLine();
    _builder.append("end.");
    _builder.newLine();
    final String source = _builder.toString();
    this.parseAndCheck(source);
    ModelGenerator _generator = this.getGenerator();
    Operation _operation = this.getOperation("banking::Account::bestAccounts");
    CharSequence _generateQueryOperation = _generator.generateQueryOperation(_operation);
    final String mapped = _generateQueryOperation.toString();
    StringConcatenation _builder_1 = new StringConcatenation();
    _builder_1.append("accountSchema.statics.bestAccounts = function (threshold) {");
    _builder_1.newLine();
    _builder_1.append("    ");
    _builder_1.append("return this.model(\'Account\').find().where(\'balance\').gt(threshold).exec(); ");
    _builder_1.newLine();
    _builder_1.append("};");
    _builder_1.newLine();
    AssertHelper.assertStringsEqual(_builder_1.toString(), mapped);
  }
  
  public void testSum() throws CoreException, IOException {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("model banking;");
    _builder.newLine();
    _builder.append("class Account");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("attribute number : String;");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("attribute balance : Double;");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("static query totalBalance() : Double;");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("begin");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("return Account extent.sum((a : Account) : Double { a.balance  });");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("end;            ");
    _builder.newLine();
    _builder.append("end;");
    _builder.newLine();
    _builder.append("end.");
    _builder.newLine();
    final String source = _builder.toString();
    this.parseAndCheck(source);
    ModelGenerator _generator = this.getGenerator();
    Operation _operation = this.getOperation("banking::Account::totalBalance");
    CharSequence _generateQueryOperation = _generator.generateQueryOperation(_operation);
    final String mapped = _generateQueryOperation.toString();
    StringConcatenation _builder_1 = new StringConcatenation();
    _builder_1.append("accountSchema.statics.totalBalance = function () {");
    _builder_1.newLine();
    _builder_1.append("    ");
    _builder_1.append("return this.model(\'Account\').aggregate()");
    _builder_1.newLine();
    _builder_1.append("        ");
    _builder_1.append(".group({ _id: null, result: { $sum: \'$balance\' } })");
    _builder_1.newLine();
    _builder_1.append("        ");
    _builder_1.append(".select(\'-id result\')");
    _builder_1.newLine();
    _builder_1.append("        ");
    _builder_1.append(".exec();");
    _builder_1.newLine();
    _builder_1.append("};");
    _builder_1.newLine();
    AssertHelper.assertStringsEqual(_builder_1.toString(), mapped);
  }
}
