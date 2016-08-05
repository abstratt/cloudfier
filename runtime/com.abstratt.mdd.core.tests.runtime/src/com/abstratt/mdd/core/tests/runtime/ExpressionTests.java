package com.abstratt.mdd.core.tests.runtime;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.uml2.uml.UMLPackage;

import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.runtime.RuntimeObject;
import com.abstratt.mdd.core.runtime.types.BasicType;
import com.abstratt.mdd.core.runtime.types.BooleanType;
import com.abstratt.mdd.core.runtime.types.IntegerType;
import com.abstratt.mdd.core.runtime.types.RealType;
import com.abstratt.mdd.core.runtime.types.StringType;

public class ExpressionTests extends AbstractRuntimeTests {

    public static Test suite() {
        return new TestSuite(ExpressionTests.class);
    }

    private static String structure;

    static {
        ExpressionTests.structure = "";
        ExpressionTests.structure += "model tests;\n";
        ExpressionTests.structure += "import base;\n";
        ExpressionTests.structure += "class Simple\n";
        ExpressionTests.structure += "static operation staticGetANumber() : Integer;\n";
        ExpressionTests.structure += "static operation staticGetABoolean() : Boolean;\n";
        ExpressionTests.structure += "static operation staticOperateOnInteger(value :  Integer) : Integer;\n";
        ExpressionTests.structure += "static operation staticOperateOnTwoIntegers(value1 :  Integer, value2 : Integer) : Integer;\n";
        ExpressionTests.structure += "static operation staticOperateOnBoolean(value :  Boolean) : Boolean;\n";
        ExpressionTests.structure += "static operation staticOperateOnObject(value :  Object) : Boolean;\n";
        ExpressionTests.structure += "static operation staticOperateOnTwoBooleans(value1 :  Boolean, value2 : Boolean) : Boolean;\n";
        ExpressionTests.structure += "static operation staticDoubleNumber(number : Integer) : Integer;\n";
        ExpressionTests.structure += "static operation staticCheckObject(value : Object) : Boolean;\n";
        ExpressionTests.structure += "static operation staticCompareTwoNumbers(value1 :  Number, value2 : Number) : Boolean;\n";
        ExpressionTests.structure += "static operation staticCompareTwoObjects(value1 :  Object, value2 : Object) : Boolean;\n";
        ExpressionTests.structure += "end;\n";
        ExpressionTests.structure += "class ClassA end;\n";
        ExpressionTests.structure += "class ClassB end;\n";
        ExpressionTests.structure += "end.";
    }

    public ExpressionTests(String arg0) {
        super(arg0);
    }

    public void testAndExpression() throws CoreException {
        createBinaryExpressionMethod("staticOperateOnTwoBooleans", "and");
        TestCase.assertEquals(BooleanType.FALSE,
                runStaticOperation("tests::Simple", "staticOperateOnTwoBooleans", BooleanType.FALSE, BooleanType.FALSE));
        TestCase.assertEquals(BooleanType.FALSE,
                runStaticOperation("tests::Simple", "staticOperateOnTwoBooleans", BooleanType.TRUE, BooleanType.FALSE));
        TestCase.assertEquals(BooleanType.TRUE,
                runStaticOperation("tests::Simple", "staticOperateOnTwoBooleans", BooleanType.TRUE, BooleanType.TRUE));
    }
    
    public void testOrRelationalExpression() throws CoreException {
        createExpressionMethod("staticCompareTwoNumbers", "value1 > value2 or value1 = value2 ");
        TestCase.assertEquals(BooleanType.FALSE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", IntegerType.fromValue(1), IntegerType.fromValue(2)));
        TestCase.assertEquals(BooleanType.TRUE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", IntegerType.fromValue(1), IntegerType.fromValue(1)));
        TestCase.assertEquals(BooleanType.TRUE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", IntegerType.fromValue(2), IntegerType.fromValue(1)));        
    }
    
    public void testOrAndExpression() throws CoreException {
        createExpressionMethod("staticOperateOnTwoBooleans", "value1 or value1 and value2");
        TestCase.assertEquals(BooleanType.FALSE,
                runStaticOperation("tests::Simple", "staticOperateOnTwoBooleans", BooleanType.FALSE, BooleanType.FALSE));
        TestCase.assertEquals(BooleanType.TRUE,
                runStaticOperation("tests::Simple", "staticOperateOnTwoBooleans", BooleanType.TRUE, BooleanType.FALSE));
        TestCase.assertEquals(BooleanType.FALSE,
                runStaticOperation("tests::Simple", "staticOperateOnTwoBooleans", BooleanType.FALSE, BooleanType.TRUE));        
        TestCase.assertEquals(BooleanType.TRUE,
                runStaticOperation("tests::Simple", "staticOperateOnTwoBooleans", BooleanType.TRUE, BooleanType.TRUE));
    }
    
    public void testNotOrAndExpression() throws CoreException {
        createExpressionMethod("staticOperateOnTwoBooleans", "not value1 or value1 and value2");
        TestCase.assertEquals(BooleanType.TRUE,
                runStaticOperation("tests::Simple", "staticOperateOnTwoBooleans", BooleanType.FALSE, BooleanType.FALSE));
        TestCase.assertEquals(BooleanType.FALSE,
                runStaticOperation("tests::Simple", "staticOperateOnTwoBooleans", BooleanType.TRUE, BooleanType.FALSE));
        TestCase.assertEquals(BooleanType.TRUE,
                runStaticOperation("tests::Simple", "staticOperateOnTwoBooleans", BooleanType.FALSE, BooleanType.TRUE));        
        TestCase.assertEquals(BooleanType.TRUE,
                runStaticOperation("tests::Simple", "staticOperateOnTwoBooleans", BooleanType.TRUE, BooleanType.TRUE));
    }
    
    public void testNotOrAndExpression2() throws CoreException {
        createExpressionMethod("staticOperateOnTwoBooleans", "not value2 or value1 and value2");
        TestCase.assertEquals(BooleanType.TRUE,
                runStaticOperation("tests::Simple", "staticOperateOnTwoBooleans", BooleanType.FALSE, BooleanType.FALSE));
        TestCase.assertEquals(BooleanType.TRUE,
                runStaticOperation("tests::Simple", "staticOperateOnTwoBooleans", BooleanType.TRUE, BooleanType.FALSE));
        TestCase.assertEquals(BooleanType.FALSE,
                runStaticOperation("tests::Simple", "staticOperateOnTwoBooleans", BooleanType.FALSE, BooleanType.TRUE));        
        TestCase.assertEquals(BooleanType.TRUE,
                runStaticOperation("tests::Simple", "staticOperateOnTwoBooleans", BooleanType.TRUE, BooleanType.TRUE));
    }

    public void testEqualsExpression() throws CoreException {
        createNumberComparisonMethod("=");
        TestCase.assertEquals(BooleanType.FALSE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new IntegerType(1), new IntegerType(2)));
        TestCase.assertEquals(BooleanType.TRUE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new IntegerType(1), new IntegerType(1)));
        TestCase.assertEquals(BooleanType.FALSE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(1), new RealType(2)));
        TestCase.assertEquals(BooleanType.TRUE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(1), new RealType(1)));
        TestCase.assertEquals(BooleanType.TRUE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(1), new IntegerType(1)));
    }

    public void testExpression() throws CoreException {
        String behavior = "";
        behavior += "model tests;\n";
        behavior += "operation Simple.staticGetANumber;\n";
        behavior += "begin\n";
        behavior += "var value : Integer;\n";
        behavior += "value := 5;";
        behavior += "return 2 * value;\n";
        behavior += "end;\n";
        behavior += "end.";
        parseAndCheck(ExpressionTests.structure, behavior);
        TestCase.assertEquals(new IntegerType(10), runStaticOperation("tests::Simple", "staticGetANumber"));
    }

    public void testGreaterOrEqualsExpression() throws CoreException {
        createNumberComparisonMethod(">=");
        TestCase.assertEquals(BooleanType.FALSE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new IntegerType(1), new IntegerType(2)));
        TestCase.assertEquals(BooleanType.TRUE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new IntegerType(1), new IntegerType(1)));
        TestCase.assertEquals(BooleanType.TRUE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new IntegerType(2), new IntegerType(1)));
        TestCase.assertEquals(BooleanType.FALSE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(1), new RealType(2)));
        TestCase.assertEquals(BooleanType.TRUE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(1), new RealType(1)));
        TestCase.assertEquals(BooleanType.TRUE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(2), new RealType(1)));
    }

    public void testGreaterThanExpression() throws CoreException {
        createNumberComparisonMethod(">");
        TestCase.assertEquals(BooleanType.FALSE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new IntegerType(1), new IntegerType(2)));
        TestCase.assertEquals(BooleanType.FALSE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new IntegerType(1), new IntegerType(1)));
        TestCase.assertEquals(BooleanType.TRUE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new IntegerType(2), new IntegerType(1)));
        TestCase.assertEquals(BooleanType.FALSE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(1), new RealType(2)));
        TestCase.assertEquals(BooleanType.FALSE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(1), new RealType(1)));
        TestCase.assertEquals(BooleanType.TRUE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(2), new RealType(1)));
        TestCase.assertEquals(BooleanType.FALSE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(1), new IntegerType(1)));
    }

    public void testLowerOrEqualsExpression() throws CoreException {
        createNumberComparisonMethod("<=");
        TestCase.assertEquals(BooleanType.TRUE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new IntegerType(1), new IntegerType(2)));
        TestCase.assertEquals(BooleanType.TRUE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new IntegerType(1), new IntegerType(1)));
        TestCase.assertEquals(BooleanType.FALSE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new IntegerType(2), new IntegerType(1)));
        TestCase.assertEquals(BooleanType.TRUE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(1), new RealType(2)));
        TestCase.assertEquals(BooleanType.TRUE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(1), new RealType(1)));
        TestCase.assertEquals(BooleanType.FALSE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(2), new RealType(1)));
    }

    public void testLowerThanExpression() throws CoreException {
        createNumberComparisonMethod("<");
        TestCase.assertEquals(BooleanType.TRUE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new IntegerType(1), new IntegerType(2)));
        TestCase.assertEquals(BooleanType.FALSE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new IntegerType(1), new IntegerType(1)));
        TestCase.assertEquals(BooleanType.FALSE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new IntegerType(2), new IntegerType(1)));
        TestCase.assertEquals(BooleanType.TRUE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(1), new RealType(2)));
        TestCase.assertEquals(BooleanType.FALSE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(1), new RealType(1)));
        TestCase.assertEquals(BooleanType.FALSE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(2), new RealType(1)));
        TestCase.assertEquals(BooleanType.FALSE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(1), new IntegerType(1)));
    }

    public void testNotEqualsExpression() throws CoreException {
        createNumberComparisonMethod("!=");
        TestCase.assertEquals(BooleanType.TRUE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new IntegerType(1), new IntegerType(2)));
        TestCase.assertEquals(BooleanType.FALSE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new IntegerType(1), new IntegerType(1)));
        TestCase.assertEquals(BooleanType.TRUE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(1), new RealType(2)));
        TestCase.assertEquals(BooleanType.FALSE,
                runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(1), new RealType(1)));
    }

    public void testNotExpression() throws CoreException {
        String behavior = "";
        behavior += "model tests;\n";
        behavior += "operation Simple.staticOperateOnBoolean;\n";
        behavior += "begin\n";
        behavior += "return not value;\n";
        behavior += "end;\n";
        behavior += "end.";
        parseAndCheck(ExpressionTests.structure, behavior);
        TestCase.assertEquals(BooleanType.TRUE, runStaticOperation("tests::Simple", "staticOperateOnBoolean", BooleanType.FALSE));
        TestCase.assertEquals(BooleanType.FALSE, runStaticOperation("tests::Simple", "staticOperateOnBoolean", BooleanType.TRUE));
    }

    public void testOrExpression() throws CoreException {
        createBinaryExpressionMethod("staticOperateOnTwoBooleans", "or");
        TestCase.assertEquals(BooleanType.FALSE,
                runStaticOperation("tests::Simple", "staticOperateOnTwoBooleans", BooleanType.FALSE, BooleanType.FALSE));
        TestCase.assertEquals(BooleanType.TRUE,
                runStaticOperation("tests::Simple", "staticOperateOnTwoBooleans", BooleanType.TRUE, BooleanType.FALSE));
        TestCase.assertEquals(BooleanType.TRUE,
                runStaticOperation("tests::Simple", "staticOperateOnTwoBooleans", BooleanType.TRUE, BooleanType.TRUE));
    }
    
    public void testIsClassifierExpression() throws CoreException {
        getRepository().getProperties().put(IRepository.EXTEND_BASE_OBJECT, "true");
        createExpressionMethod("staticCheckObject", "(value is tests::ClassB)");
        
        org.eclipse.uml2.uml.Class classA = getRepository().findNamedElement("tests::ClassA", UMLPackage.Literals.CLASS, null);
        org.eclipse.uml2.uml.Class classB = getRepository().findNamedElement("tests::ClassB", UMLPackage.Literals.CLASS, null);
        TestCase.assertNotNull(classA);
        TestCase.assertNotNull(classB);

        RuntimeObject a1 = getRuntime().getRuntimeClass(classA).newInstance();
        RuntimeObject b1 = getRuntime().getRuntimeClass(classB).newInstance();
        
        TestCase.assertEquals(BooleanType.FALSE, runStaticOperation("tests::Simple", "staticCheckObject", a1));
        TestCase.assertEquals(BooleanType.TRUE, runStaticOperation("tests::Simple", "staticCheckObject", b1));

    }

    public void testSameExpression() throws CoreException {
        getRepository().getProperties().put(IRepository.EXTEND_BASE_OBJECT, "true");
        createBinaryExpressionMethod("staticCompareTwoObjects", "==");

        org.eclipse.uml2.uml.Class classA = getRepository().findNamedElement("tests::ClassA", UMLPackage.Literals.CLASS, null);
        org.eclipse.uml2.uml.Class classB = getRepository().findNamedElement("tests::ClassB", UMLPackage.Literals.CLASS, null);
        TestCase.assertNotNull(classA);
        TestCase.assertNotNull(classB);

        RuntimeObject a1 = getRuntime().getRuntimeClass(classA).newInstance();
        RuntimeObject a2 = getRuntime().getRuntimeClass(classA).newInstance();
        RuntimeObject b1 = getRuntime().getRuntimeClass(classB).newInstance();
        RuntimeObject b2 = getRuntime().getRuntimeClass(classB).newInstance();

        TestCase.assertEquals(BooleanType.FALSE, runStaticOperation("tests::Simple", "staticCompareTwoObjects", a1, a2));
        TestCase.assertEquals(BooleanType.TRUE, runStaticOperation("tests::Simple", "staticCompareTwoObjects", a1, a1));
        TestCase.assertEquals(BooleanType.FALSE, runStaticOperation("tests::Simple", "staticCompareTwoObjects", b1, b2));
        TestCase.assertEquals(BooleanType.TRUE, runStaticOperation("tests::Simple", "staticCompareTwoObjects", b1, b1));
        TestCase.assertEquals(BooleanType.FALSE, runStaticOperation("tests::Simple", "staticCompareTwoObjects", a1, b1));
    }
    
    public void testNotNull() throws CoreException {
        getRepository().getProperties().put(IRepository.EXTEND_BASE_OBJECT, "true");

        String behavior = "";
        behavior += "model tests;\n";
        behavior += "operation Simple.staticOperateOnObject;\n";
        behavior += "begin\n";
        behavior += "return ?value;\n";
        behavior += "end;\n";
        behavior += "end.";
        parseAndCheck(ExpressionTests.structure, behavior);
        TestCase.assertEquals(BooleanType.TRUE, runStaticOperation("tests::Simple", "staticOperateOnObject", new StringType("")));
        TestCase.assertEquals(BooleanType.FALSE, runStaticOperation("tests::Simple", "staticOperateOnObject", (BasicType) null));
    }
    

    private void createBinaryExpressionMethod(String operationName, String operator) throws CoreException {
        String behavior = "";
        behavior += "model tests;\n";
        behavior += "operation Simple." + operationName + ";\n";
        behavior += "begin\n";
        behavior += "return value1 " + operator + " value2;\n";
        behavior += "end;\n";
        behavior += "end.";
        parseAndCheck(ExpressionTests.structure, behavior);
    }
    
    private void createExpressionMethod(String operationName, String expression) throws CoreException {
        String behavior = "";
        behavior += "model tests;\n";
        behavior += "operation Simple." + operationName + ";\n";
        behavior += "begin\n";
        behavior += "return " + expression + ";\n";
        behavior += "end;\n";
        behavior += "end.";
        parseAndCheck(ExpressionTests.structure, behavior);
    }

    private void createNumberComparisonMethod(String operator) throws CoreException {
        createBinaryExpressionMethod("staticCompareTwoNumbers", operator);
    }
}
