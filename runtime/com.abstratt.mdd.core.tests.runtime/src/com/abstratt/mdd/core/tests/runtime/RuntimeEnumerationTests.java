package com.abstratt.mdd.core.tests.runtime;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;

import com.abstratt.mdd.core.runtime.RuntimeClass;
import com.abstratt.mdd.core.runtime.RuntimeObject;
import com.abstratt.mdd.core.runtime.types.BooleanType;
import com.abstratt.mdd.core.runtime.types.IntegerType;
import com.abstratt.mdd.core.runtime.types.PrimitiveType;
import com.abstratt.mdd.core.runtime.types.RealType;
import com.abstratt.mdd.core.runtime.types.StringType;

public class RuntimeEnumerationTests extends AbstractRuntimeTests {

    public static Test suite() {
        return new TestSuite(RuntimeEnumerationTests.class);
    }

    public RuntimeEnumerationTests(String name) {
        super(name);
    }
    
    static String model = "";

    static {
    	model += "model tests;\n";
    	model += "  import base;\n";
        model += "  enumeration Role\n";
        model += "    attribute salary : Double;"
        		+ "   OWNER(salary:=10000.0); EMPLOYEE(salary:=2000.0); FAMILY(salary:=0.0);\n";
        model += "  end;\n";
        model += "  class Person\n";
        model += "    attribute name : String;\n";
        model += "    attribute personRole : Role := EMPLOYEE;\n";
        model += "    derived attribute salary : Double := { self.personRole.salary };\n";
        model += "  end;\n";
        model += "end.";
    }

    public void testEnumerationAttribute() throws CoreException {
        parseAndCheck(model);
        RuntimeClass personClass = getRuntimeClass("tests::Person");
        RuntimeObject person = personClass.newInstance();
        assertEquals(2000d, ((PrimitiveType) readAttribute(person, "salary")).primitiveValue());

    }

    @Override
    protected void originalRunTest() throws Throwable {
        // invoke explicitly - can't rely on #compilationCompleted as we don't
        // compile anything here
        setupRuntime();
        super.originalRunTest();
    }

}
