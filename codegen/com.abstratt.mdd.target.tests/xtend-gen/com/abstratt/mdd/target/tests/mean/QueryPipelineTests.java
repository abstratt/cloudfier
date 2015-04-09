package com.abstratt.mdd.target.tests.mean;

import com.abstratt.mdd.core.util.ActivityUtils;
import com.abstratt.mdd.target.mean.ExtentStage;
import com.abstratt.mdd.target.mean.FilterStage;
import com.abstratt.mdd.target.mean.MappingStage;
import com.abstratt.mdd.target.mean.QueryPipeline;
import com.abstratt.mdd.target.mean.QueryStage;
import com.abstratt.mdd.target.tests.AbstractGeneratorTest;
import java.io.IOException;
import java.util.List;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.StructuredActivityNode;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.xbase.lib.IterableExtensions;

@SuppressWarnings("all")
public class QueryPipelineTests extends AbstractGeneratorTest {
  public static Test suite() {
    return new TestSuite(QueryPipelineTests.class);
  }
  
  public QueryPipelineTests(final String name) {
    super(name);
  }
  
  public void testExtent() throws CoreException, IOException {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("model crm;");
    _builder.newLine();
    _builder.append("class Customer");
    _builder.newLine();
    _builder.append("      ");
    _builder.append("query findAll() : Customer[*];");
    _builder.newLine();
    _builder.append("      ");
    _builder.append("begin");
    _builder.newLine();
    _builder.append("          ");
    _builder.append("return Customer extent;");
    _builder.newLine();
    _builder.append("      ");
    _builder.append("end;");
    _builder.newLine();
    _builder.append("end;");
    _builder.newLine();
    _builder.append("end.");
    _builder.newLine();
    String source = _builder.toString();
    this.parseAndCheck(source);
    final Operation op = this.getOperation("crm::Customer::findAll");
    Activity _activity = ActivityUtils.getActivity(op);
    StructuredActivityNode _rootAction = ActivityUtils.getRootAction(_activity);
    List<Action> _findStatements = ActivityUtils.findStatements(_rootAction);
    Action _head = IterableExtensions.<Action>head(_findStatements);
    final Action root = ActivityUtils.getSourceAction(_head);
    final QueryPipeline pipeline = QueryPipeline.build(root);
    long _stageCount = pipeline.getStageCount();
    TestCase.assertEquals(1, _stageCount);
    QueryStage _stage = pipeline.getStage(0);
    TestCase.assertTrue((_stage instanceof ExtentStage));
  }
  
  public void testSelect() throws CoreException, IOException {
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
    final Operation op = this.getOperation("crm::Customer::mvpCustomers");
    Activity _activity = ActivityUtils.getActivity(op);
    StructuredActivityNode _rootAction = ActivityUtils.getRootAction(_activity);
    List<Action> _findStatements = ActivityUtils.findStatements(_rootAction);
    Action _head = IterableExtensions.<Action>head(_findStatements);
    final Action root = ActivityUtils.getSourceAction(_head);
    final QueryPipeline pipeline = QueryPipeline.build(root);
    long _stageCount = pipeline.getStageCount();
    TestCase.assertEquals(2, _stageCount);
    QueryStage _stage = pipeline.getStage(0);
    TestCase.assertTrue((_stage instanceof ExtentStage));
    QueryStage _stage_1 = pipeline.getStage(1);
    TestCase.assertTrue((_stage_1 instanceof FilterStage));
    QueryStage _stage_2 = pipeline.getStage(1);
    final FilterStage filter = ((FilterStage) _stage_2);
    TestCase.assertNotNull(filter.condition);
  }
  
  public void testMap() throws CoreException, IOException {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("model crm;");
    _builder.newLine();
    _builder.append("class Account");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("attribute holder : Customer;");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("static query holders() : Customer[*];");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("begin");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("return Account extent.collect((a : Account) : Customer { a.holder });");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("end;            ");
    _builder.newLine();
    _builder.append("end;");
    _builder.newLine();
    _builder.append("class Customer");
    _builder.newLine();
    _builder.append("end;");
    _builder.newLine();
    _builder.append("end.");
    _builder.newLine();
    final String source = _builder.toString();
    this.parseAndCheck(source);
    final Operation op = this.getOperation("crm::Account::holders");
    Activity _activity = ActivityUtils.getActivity(op);
    StructuredActivityNode _rootAction = ActivityUtils.getRootAction(_activity);
    List<Action> _findStatements = ActivityUtils.findStatements(_rootAction);
    Action _head = IterableExtensions.<Action>head(_findStatements);
    final Action root = ActivityUtils.getSourceAction(_head);
    final QueryPipeline pipeline = QueryPipeline.build(root);
    long _stageCount = pipeline.getStageCount();
    TestCase.assertEquals(2, _stageCount);
    QueryStage _stage = pipeline.getStage(0);
    TestCase.assertTrue((_stage instanceof ExtentStage));
    QueryStage _stage_1 = pipeline.getStage(1);
    TestCase.assertTrue((_stage_1 instanceof MappingStage));
    QueryStage _stage_2 = pipeline.getStage(1);
    final MappingStage mapping = ((MappingStage) _stage_2);
    TestCase.assertNotNull(mapping.mapping);
  }
}
