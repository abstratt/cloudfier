package com.abstratt.mdd.core.tests.runtime;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.uml2.uml.UMLPackage;

import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.runtime.RuntimeObject;
import com.abstratt.mdd.core.runtime.types.BooleanType;
import com.abstratt.mdd.core.runtime.types.IntegerType;
import com.abstratt.mdd.core.runtime.types.RealType;

public class ExpressionTests extends AbstractRuntimeTests {

	private static String structure;

	static {
		structure = "";
		structure += "model tests;\n";
		structure += "import base;\n";
		structure += "class Simple\n";
		structure += "static operation staticGetANumber() : Integer;\n";
		structure += "static operation staticGetABoolean() : Boolean;\n";
		structure += "static operation staticOperateOnInteger(value :  Integer) : Integer;\n";
		structure += "static operation staticOperateOnTwoIntegers(value1 :  Integer, value2 : Integer) : Integer;\n";
		structure += "static operation staticOperateOnBoolean(value :  Boolean) : Boolean;\n";
		structure += "static operation staticOperateOnTwoBooleans(value1 :  Boolean, value2 : Boolean) : Boolean;\n";
		structure += "static operation staticDoubleNumber(number : Integer) : Integer;\n";
		structure += "static operation staticCompareTwoNumbers(value1 :  Number, value2 : Number) : Boolean;\n";
		structure += "static operation staticCompareTwoObjects(value1 :  Object, value2 : Object) : Boolean;\n";		
		structure += "end;\n";
		structure += "class ClassA end;\n";
		structure += "class ClassB end;\n";
		structure += "end.";
	}

	public static Test suite() {
		return new TestSuite(ExpressionTests.class);
	}

	public ExpressionTests(String arg0) {
		super(arg0);
	}

	private void createNumberComparisonMethod(String operator) throws CoreException {
		createBinaryExpressionMethod("staticCompareTwoNumbers", operator);
	}
	
	private void createBinaryExpressionMethod(String operationName, String operator) throws CoreException {
		String behavior = "";
		behavior += "model tests;\n";
		behavior += "operation Simple." + operationName + ";\n";
		behavior += "begin\n";
		behavior += "return value1 " + operator + " value2;\n";
		behavior += "end;\n";
		behavior += "end.";
		parseAndCheck(structure, behavior);
	}

	
	public void testAndExpression() throws CoreException {
		createBinaryExpressionMethod("staticOperateOnTwoBooleans", "and");
		assertEquals(BooleanType.FALSE, runStaticOperation("tests::Simple", "staticOperateOnTwoBooleans", BooleanType.FALSE, BooleanType.FALSE));
		assertEquals(BooleanType.FALSE, runStaticOperation("tests::Simple", "staticOperateOnTwoBooleans", BooleanType.TRUE, BooleanType.FALSE));
		assertEquals(BooleanType.TRUE, runStaticOperation("tests::Simple", "staticOperateOnTwoBooleans", BooleanType.TRUE, BooleanType.TRUE));
	}
	
	public void testEqualsExpression() throws CoreException {
		createNumberComparisonMethod("=");
		assertEquals(BooleanType.FALSE, runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new IntegerType(1), new IntegerType(2)));
		assertEquals(BooleanType.TRUE, runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new IntegerType(1), new IntegerType(1)));
		assertEquals(BooleanType.FALSE, runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(1), new RealType(2)));
		assertEquals(BooleanType.TRUE, runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(1), new RealType(1)));
		assertEquals(BooleanType.TRUE, runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(1), new IntegerType(1)));
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
		parseAndCheck(structure, behavior);
		assertEquals(new IntegerType(10), runStaticOperation("tests::Simple", "staticGetANumber"));
	}

	public void testGreaterOrEqualsExpression() throws CoreException {
		createNumberComparisonMethod(">=");
		assertEquals(BooleanType.FALSE, runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new IntegerType(1), new IntegerType(2)));
		assertEquals(BooleanType.TRUE, runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new IntegerType(1), new IntegerType(1)));
		assertEquals(BooleanType.TRUE, runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new IntegerType(2), new IntegerType(1)));		
		assertEquals(BooleanType.FALSE, runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(1), new RealType(2)));
		assertEquals(BooleanType.TRUE, runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(1), new RealType(1)));
		assertEquals(BooleanType.TRUE, runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(2), new RealType(1)));		
	}
	
	public void testGreaterThanExpression() throws CoreException {
		createNumberComparisonMethod(">");
		assertEquals(BooleanType.FALSE, runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new IntegerType(1), new IntegerType(2)));
		assertEquals(BooleanType.FALSE, runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new IntegerType(1), new IntegerType(1)));
		assertEquals(BooleanType.TRUE, runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new IntegerType(2), new IntegerType(1)));		
		assertEquals(BooleanType.FALSE, runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(1), new RealType(2)));
		assertEquals(BooleanType.FALSE, runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(1), new RealType(1)));
		assertEquals(BooleanType.TRUE, runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(2), new RealType(1)));		
		assertEquals(BooleanType.FALSE, runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(1), new IntegerType(1)));
	}
	
	public void testLowerOrEqualsExpression() throws CoreException {
		createNumberComparisonMethod("<=");
		assertEquals(BooleanType.TRUE, runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new IntegerType(1), new IntegerType(2)));
		assertEquals(BooleanType.TRUE, runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new IntegerType(1), new IntegerType(1)));
		assertEquals(BooleanType.FALSE, runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new IntegerType(2), new IntegerType(1)));		
		assertEquals(BooleanType.TRUE, runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(1), new RealType(2)));
		assertEquals(BooleanType.TRUE, runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(1), new RealType(1)));
		assertEquals(BooleanType.FALSE, runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(2), new RealType(1)));		
	}
	
	public void testLowerThanExpression() throws CoreException {
		createNumberComparisonMethod("<");
		assertEquals(BooleanType.TRUE, runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new IntegerType(1), new IntegerType(2)));
		assertEquals(BooleanType.FALSE, runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new IntegerType(1), new IntegerType(1)));
		assertEquals(BooleanType.FALSE, runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new IntegerType(2), new IntegerType(1)));		
		assertEquals(BooleanType.TRUE, runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(1), new RealType(2)));
		assertEquals(BooleanType.FALSE, runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(1), new RealType(1)));
		assertEquals(BooleanType.FALSE, runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(2), new RealType(1)));		
		assertEquals(BooleanType.FALSE, runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(1), new IntegerType(1)));
	}
	
	public void testNotEqualsExpression() throws CoreException {
		createNumberComparisonMethod("!=");
		assertEquals(BooleanType.TRUE, runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new IntegerType(1), new IntegerType(2)));
		assertEquals(BooleanType.FALSE, runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new IntegerType(1), new IntegerType(1)));
		assertEquals(BooleanType.TRUE, runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(1), new RealType(2)));
		assertEquals(BooleanType.FALSE, runStaticOperation("tests::Simple", "staticCompareTwoNumbers", new RealType(1), new RealType(1)));
	}


	public void testNotExpression() throws CoreException {
		String behavior = "";
		behavior += "model tests;\n";
		behavior += "operation Simple.staticOperateOnBoolean;\n";
		behavior += "begin\n";
		behavior += "return not value;\n";
		behavior += "end;\n";
		behavior += "end.";
		parseAndCheck(structure, behavior);
		assertEquals(BooleanType.TRUE, runStaticOperation("tests::Simple", "staticOperateOnBoolean", BooleanType.FALSE));
		assertEquals(BooleanType.FALSE, runStaticOperation("tests::Simple", "staticOperateOnBoolean", BooleanType.TRUE));
	}

	public void testOrExpression() throws CoreException {
		createBinaryExpressionMethod("staticOperateOnTwoBooleans", "or");
		assertEquals(BooleanType.FALSE, runStaticOperation("tests::Simple", "staticOperateOnTwoBooleans", BooleanType.FALSE, BooleanType.FALSE));
		assertEquals(BooleanType.TRUE, runStaticOperation("tests::Simple", "staticOperateOnTwoBooleans", BooleanType.TRUE, BooleanType.FALSE));
		assertEquals(BooleanType.TRUE, runStaticOperation("tests::Simple", "staticOperateOnTwoBooleans", BooleanType.TRUE, BooleanType.TRUE));
	}

	public void testSameExpression() throws CoreException {
		getRepository().getProperties().put(IRepository.EXTEND_BASE_OBJECT, "true");
		createBinaryExpressionMethod("staticCompareTwoObjects", "==");
		
		org.eclipse.uml2.uml.Class classA = getRepository().findNamedElement("tests::ClassA", UMLPackage.Literals.CLASS, null);
		org.eclipse.uml2.uml.Class classB = getRepository().findNamedElement("tests::ClassB", UMLPackage.Literals.CLASS, null);
		assertNotNull(classA);
		assertNotNull(classB);
		
		RuntimeObject a1 = getRuntime().getRuntimeClass(classA).newInstance();
		RuntimeObject a2 = getRuntime().getRuntimeClass(classA).newInstance();
		RuntimeObject b1 = getRuntime().getRuntimeClass(classB).newInstance();
		RuntimeObject b2 = getRuntime().getRuntimeClass(classB).newInstance();

		assertEquals(BooleanType.FALSE, runStaticOperation("tests::Simple", "staticCompareTwoObjects", a1, a2));
		assertEquals(BooleanType.TRUE, runStaticOperation("tests::Simple", "staticCompareTwoObjects", a1, a1));
		assertEquals(BooleanType.FALSE, runStaticOperation("tests::Simple", "staticCompareTwoObjects", b1, b2));
		assertEquals(BooleanType.TRUE, runStaticOperation("tests::Simple", "staticCompareTwoObjects", b1, b1));
		assertEquals(BooleanType.FALSE, runStaticOperation("tests::Simple", "staticCompareTwoObjects", a1, b1));
	}
}
