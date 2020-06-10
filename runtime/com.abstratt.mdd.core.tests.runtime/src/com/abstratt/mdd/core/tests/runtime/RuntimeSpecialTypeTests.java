package com.abstratt.mdd.core.tests.runtime;

import java.util.Collections;

import org.eclipse.core.runtime.CoreException;

import com.abstratt.mdd.core.runtime.RuntimeClass;
import com.abstratt.mdd.core.runtime.RuntimeObject;
import com.abstratt.mdd.core.runtime.types.BlobInfo;
import com.abstratt.mdd.core.runtime.types.PictureType;
import com.abstratt.mdd.core.runtime.types.PrimitiveType;
import com.abstratt.mdd.core.runtime.types.StringType;

import junit.framework.Test;
import junit.framework.TestSuite;

public class RuntimeSpecialTypeTests extends AbstractRuntimeTests {

    public static Test suite() {
        return new TestSuite(RuntimeSpecialTypeTests.class);
    }

    public RuntimeSpecialTypeTests(String name) {
        super(name);
    }
    
    static String model = "";

    static {
    	model += "model tests;\n";
    	model += "  import base;\n";
        model += "  datatype Phone\n";
        model += "    attribute prefix : String;\n";
        model += "    attribute number : Integer;\n";
        model += "  end;\n";
        model += "  enumeration Role\n";
        model += "    attribute salary : Double;"
                + "   OWNER(salary:=10000.0); EMPLOYEE(salary:=2000.0); FAMILY(salary:=0.0);\n";
        model += "  end;\n";
        model += "  class Person\n";
        model += "    attribute name : String := \"John Doe\";\n";
        model += "    attribute photo : Picture := { Picture#empty() };\n";
        model += "    attribute note : Memo := { Memo#fromString(\"This is a memo note\") };\n";
        model += "    attribute phone : Phone := { ({ prefix := \"48\", number := \"98765-4321\" } as Phone) };\n";
        model += "    attribute personRole : Role := EMPLOYEE;\n";
        model += "    derived attribute salary : Double := { self.personRole.salary };\n";        
        model += "  end;\n";
        model += "end.";
    }

    public void testSpecialAttributes() throws CoreException {
        parseAndCheck(model);
        RuntimeClass personClass = getRuntimeClass("tests::Person");
        RuntimeObject person = personClass.newInstance(true);
        
        assertNotNull(readAttribute(person, "name"));
        assertNotNull(readAttribute(person, "photo"));
        assertNotNull(readAttribute(person, "phone"));
        person.attach();

        saveContext();
        
        RuntimeObject loaded = personClass.getInstance(person.getKey());
        assertNotNull(loaded);

        assertEquals(new StringType("John Doe"), readAttribute(loaded, "name"));
        assertNotNull(readAttribute(loaded, "photo"));
        assertNotNull(readAttribute(loaded, "phone"));

    }
    
    public void testEnumerationAttribute() throws CoreException {
        parseAndCheck(model);
        RuntimeClass personClass = getRuntimeClass("tests::Person");
        RuntimeObject person = personClass.newInstance();
        assertEquals(2000d, ((PrimitiveType) readAttribute(person, "salary")).primitiveValue());
    }

}
