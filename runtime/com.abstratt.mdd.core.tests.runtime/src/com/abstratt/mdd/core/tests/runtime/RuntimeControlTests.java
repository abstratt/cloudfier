package com.abstratt.mdd.core.tests.runtime;

import org.eclipse.core.runtime.CoreException;

import com.abstratt.mdd.core.IProblem;
import com.abstratt.mdd.core.IProblem.Severity;
import com.abstratt.mdd.core.runtime.types.BasicType;
import com.abstratt.mdd.core.runtime.types.IntegerType;
import com.abstratt.mdd.frontend.core.IdsShouldBeRequiredSingle;
import com.abstratt.mdd.frontend.core.TypeMismatch;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class RuntimeControlTests extends AbstractRuntimeTests {

	public static Test suite() {
		return new TestSuite(RuntimeControlTests.class);
	}

	public RuntimeControlTests(String name) {
		super(name);
	}

	public void testElvis() throws CoreException {
		String source = "";
		source += "model tests;\n";
		source += "import mdd_types;\n";
		source += "class Simple\n";
		source += "static operation compute(value : Integer[0,1]) : Integer;\n";
		source += "begin\n";
		source += "  return value ?: 1;\n";
		source += "end;\n";
		source += "end;\n";
		source += "end.";
		String[] sources = { source };
		parseAndCheck(sources);
		IntegerType number1 = new IntegerType(1);
		IntegerType number2 = new IntegerType(2);
		TestCase.assertEquals(number2, runStaticOperation("tests::Simple", "compute", number2));
		TestCase.assertEquals(number1, runStaticOperation("tests::Simple", "compute", new BasicType[] {null}));
	}
	
	public void testTernary() throws CoreException {
		String source = "";
		source += "model tests;\n";
		source += "import mdd_types;\n";
		source += "class Simple\n";
		source += "static operation compute(value : Integer) : Integer;\n";
		source += "begin\n";
		source += "  return value > 0 ? 1 : 0;\n";
		source += "end;\n";
		source += "end;\n";
		source += "end.";
		String[] sources = { source };
		parseAndCheck(sources);
		IntegerType number1 = new IntegerType(1);
		IntegerType number3 = new IntegerType(3);
		IntegerType negative3 = new IntegerType(-3);
		IntegerType zero = new IntegerType(0);
		TestCase.assertEquals(number1, runStaticOperation("tests::Simple", "compute", number3));
		TestCase.assertEquals(number1, runStaticOperation("tests::Simple", "compute", number1));
		TestCase.assertEquals(zero, runStaticOperation("tests::Simple", "compute", negative3));
		TestCase.assertEquals(zero, runStaticOperation("tests::Simple", "compute", zero));
	}
	
	public void testTernary_composite() throws CoreException {
		String source = "";
		source += "model tests;\n";
		source += "import mdd_types;\n";
		source += "class Simple\n";
		source += "static operation compute(value : Integer) : Integer;\n";
		source += "begin\n";
		source += "  return value > 0 ? 1 : value < 0 ? -1 : 0;\n";
		source += "end;\n";
		source += "end;\n";
		source += "end.";
		String[] sources = { source };
		parseAndCheck(sources);
		IntegerType number1 = new IntegerType(1);
		IntegerType number3 = new IntegerType(3);
		IntegerType negative3 = new IntegerType(-3);
		IntegerType negative1 = new IntegerType(-1);
		IntegerType zero = new IntegerType(0);
		TestCase.assertEquals(number1, runStaticOperation("tests::Simple", "compute", number3));
		TestCase.assertEquals(negative1, runStaticOperation("tests::Simple", "compute", negative3));
		TestCase.assertEquals(zero, runStaticOperation("tests::Simple", "compute", zero));
	}
    
    public void testEarlyReturn() throws CoreException {
        String source = "";
        source += "model tests;\n";
        source += "import mdd_types;\n";
        source += "class Simple\n";
        source += "static operation doIt() : Integer;\n";
        source += "begin\n";
        source += "  return 1; \n";
        source += "  return 2;\n";
        source += "end;\n";
        source += "end;\n";
        source += "end.";
        String[] sources = { source };
        parseAndCheck(sources);
        TestCase.assertEquals(new IntegerType(1), runStaticOperation("tests::Simple", "doIt"));
    }
}
