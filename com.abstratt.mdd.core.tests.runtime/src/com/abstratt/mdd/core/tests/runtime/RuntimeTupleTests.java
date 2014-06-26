package com.abstratt.mdd.core.tests.runtime;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;

import com.abstratt.mdd.core.runtime.RuntimeObject;
import com.abstratt.mdd.core.runtime.types.IntegerType;
import com.abstratt.mdd.core.runtime.types.StringType;

public class RuntimeTupleTests extends AbstractRuntimeTests {

    public static Test suite() {
        return new TestSuite(RuntimeTupleTests.class);
    }

    public RuntimeTupleTests(String name) {
        super(name);
    }

    public void testReadStaticDerivedAttribute() throws CoreException {
        String source = "";
        source += "model tests;\n";
        source += "  import base;\n";
        source += "  class MyClass\n";
        source += "    static operation value() : {name : String, age : Integer};\n";
        source += "    begin\n";
        source += "        return { name := \"John\" , age := 10 };\n";
        source += "    end;\n";
        source += "  end;\n";
        source += "end.";

        parseAndCheck(new String[] { source });
        RuntimeObject read = (RuntimeObject) runStaticOperation("tests::MyClass", "value");
        TestCase.assertNotNull(read);
        TestCase.assertEquals(new IntegerType(10), readAttribute(read, "age"));
        TestCase.assertEquals(new StringType("John"), readAttribute(read, "name"));
    }
}
