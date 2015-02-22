package com.abstratt.mdd.target.tests;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.AddVariableValueAction;
import org.eclipse.uml2.uml.CallOperationAction;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.Variable;

import com.abstratt.mdd.core.target.spi.ActionMappingUtils;
import com.abstratt.mdd.core.tests.harness.AbstractRepositoryBuildingTests;
import com.abstratt.mdd.core.util.ActivityUtils;

public class BasicTargetTests extends AbstractRepositoryBuildingTests {

    public static Test suite() {
        return new TestSuite(BasicTargetTests.class);
    }

    public BasicTargetTests(String name) {
        super(name);
    }

    public void testCollision() throws CoreException, IOException {
        String source = "";
        source += "model simple;\n";
        source += "  import mdd_types;\n";
        source += "  class Class1\n";
        source += "      static operation query1() : Class2[*];\n";
        source += "      begin\n";
        source += "          return (((Class1 extent.collect((a : Class1) : Class2 { null }) as Class2).collect((a : Class2) : Class1 { null }) as Class1).collect((a : Class1) : Class2 { null }) as Class2);\n";
        source += "      end;\n";
        source += "  end;\n";
        source += "  class Class2\n";
        source += "  end;\n";
        source += "end.";
        parseAndCheck(source);

        Operation queryOperation = getRepository().findNamedElement("simple::Class1::query1", UMLPackage.Literals.OPERATION, null);
        TestCase.assertNotNull(queryOperation);

        List<Action> statements = ActivityUtils.findStatements(ActivityUtils.getRootAction(queryOperation));
        TestCase.assertEquals(statements.size(), 1);

        TestCase.assertTrue(statements.get(0).toString(), statements.get(0) instanceof AddVariableValueAction);
        CallOperationAction collect1 = (CallOperationAction) ActivityUtils.getSourceAction(((AddVariableValueAction) statements.get(0))
                .getValue());
        CallOperationAction collect2 = (CallOperationAction) ActivityUtils.getSourceAction(collect1.getTarget());
        CallOperationAction collect3 = (CallOperationAction) ActivityUtils.getSourceAction(collect2.getTarget());

        HashMap<Variable, String> allocation = new HashMap<Variable, String>();
        TestCase.assertEquals("a", ActionMappingUtils.getParameterVariables(allocation, collect3).get(0));
        TestCase.assertEquals("mapped", ActionMappingUtils.getResultVariable(allocation, collect3));
        TestCase.assertEquals("mapped", ActionMappingUtils.getParameterVariables(allocation, collect2).get(0));
        TestCase.assertEquals("mapped1", ActionMappingUtils.getResultVariable(allocation, collect2));
        TestCase.assertEquals("mapped1", ActionMappingUtils.getParameterVariables(allocation, collect1).get(0));
        TestCase.assertEquals("mapped2", ActionMappingUtils.getResultVariable(allocation, collect1));
    }

    /**
     * In:
     *
     * return Class1 extent.select((a : Class1) : Boolean { return true
     * }).select((b : Class1) : Boolean { return true }).collect((c : Class1) :
     * Class2 { return c.attr1 }).select((d : Class1) : Boolean { return true
     * }).select((e : Class1) : Boolean { return true })
     *
     * a = b = c, d = e
     *
     * @throws CoreException
     * @throws IOException
     */
    public void testSuggestedVariableNames() throws CoreException, IOException {
        String source = "";
        source += "model simple;\n";
        source += "  import mdd_types;\n";
        source += "  class Class1\n";
        source += "      reference attr1 : Class2;\n";
        source += "      static operation query1() : Integer;\n";
        source += "      begin\n";
        source += "          return Class1 extent.select((a : Class1) : Boolean { true }).select((b : Class1) : Boolean { true }).collect((c : Class1) : Class2 { c->attr1 }).select((d : Class2) : Boolean { true }).select((e : Class2) : Boolean { true }).reduce((f : Class2, count : Integer) : Integer { 1 }, 0);\n";
        source += "      end;\n";
        source += "  end;\n";
        source += "  class Class2\n";
        source += "  end;\n";
        source += "end.";
        parseAndCheck(source);

        Operation queryOperation = getRepository().findNamedElement("simple::Class1::query1", UMLPackage.Literals.OPERATION, null);
        TestCase.assertNotNull(queryOperation);

        List<Action> statements = ActivityUtils.findStatements(ActivityUtils.getRootAction(queryOperation));
        TestCase.assertEquals(statements.size(), 1);

        TestCase.assertTrue(statements.get(0).toString(), statements.get(0) instanceof AddVariableValueAction);
        CallOperationAction reduce = (CallOperationAction) ActivityUtils.getSourceAction(((AddVariableValueAction) statements.get(0))
                .getValue());
        CallOperationAction select1 = (CallOperationAction) ActivityUtils.getSourceAction(reduce.getTarget());
        CallOperationAction select2 = (CallOperationAction) ActivityUtils.getSourceAction(select1.getTarget());
        CallOperationAction collect = (CallOperationAction) ActivityUtils.getSourceAction(select2.getTarget());
        CallOperationAction select3 = (CallOperationAction) ActivityUtils.getSourceAction(collect.getTarget());
        CallOperationAction select4 = (CallOperationAction) ActivityUtils.getSourceAction(select3.getTarget());

        TestCase.assertEquals("a", ActionMappingUtils.getParameterVariables(new HashMap<Variable, String>(), select4).get(0));
        TestCase.assertEquals("a", ActionMappingUtils.getResultVariable(new HashMap<Variable, String>(), select4));
        TestCase.assertEquals("a", ActionMappingUtils.getParameterVariables(new HashMap<Variable, String>(), select3).get(0));
        TestCase.assertEquals("a", ActionMappingUtils.getResultVariable(new HashMap<Variable, String>(), select3));
        TestCase.assertEquals("a", ActionMappingUtils.getParameterVariables(new HashMap<Variable, String>(), collect).get(0));
        TestCase.assertEquals("mapped", ActionMappingUtils.getResultVariable(new HashMap<Variable, String>(), collect));
        TestCase.assertEquals("mapped", ActionMappingUtils.getParameterVariables(new HashMap<Variable, String>(), select2).get(0));
        TestCase.assertEquals("mapped", ActionMappingUtils.getResultVariable(new HashMap<Variable, String>(), select2));
        TestCase.assertEquals("mapped", ActionMappingUtils.getParameterVariables(new HashMap<Variable, String>(), select1).get(0));
        TestCase.assertEquals("mapped", ActionMappingUtils.getResultVariable(new HashMap<Variable, String>(), select1));
        TestCase.assertEquals("mapped", ActionMappingUtils.getParameterVariables(new HashMap<Variable, String>(), reduce).get(0));
        TestCase.assertEquals("count", ActionMappingUtils.getParameterVariables(new HashMap<Variable, String>(), reduce).get(1));
        TestCase.assertEquals("count", ActionMappingUtils.getResultVariable(new HashMap<Variable, String>(), reduce));
    }

}
