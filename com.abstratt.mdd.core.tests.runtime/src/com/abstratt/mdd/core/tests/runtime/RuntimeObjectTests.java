package com.abstratt.mdd.core.tests.runtime;

import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.UMLPackage;

import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.runtime.ObjectNotActiveException;
import com.abstratt.mdd.core.runtime.RuntimeClass;
import com.abstratt.mdd.core.runtime.RuntimeObject;
import com.abstratt.mdd.core.runtime.types.BasicType;
import com.abstratt.mdd.core.runtime.types.BooleanType;
import com.abstratt.mdd.core.runtime.types.EnumerationType;
import com.abstratt.mdd.core.runtime.types.IntegerType;
import com.abstratt.mdd.core.runtime.types.StringType;

public class RuntimeObjectTests extends AbstractRuntimeTests {

	static String model = "";

	static {
		model += "model tests;\n";
		model += "  import base;\n";
		model += "  enumeration Role OWNER, EMPLOYEE, FAMILY end;\n";
		model += "  interface Named\n";
		model += "    operation getName() : String;\n";
		model += "  end;\n";
		model += "  class Person implements Named\n";
		model += "    attribute name : String[0,1];\n";
		model += "    attribute active : Boolean := true;\n";
		model += "    attribute personRole : Role := EMPLOYEE;\n";
		model += "    static operation newPerson(name : String) : Person;\n";
		model += "    begin\n";
		model += "      var newPerson : Person;\n";
		model += "      newPerson := new Person;\n";
		model += "      newPerson.name := name;\n";
		model += "      return newPerson;\n";
		model += "    end;\n";
		model += "    operation getName() : String;\n";
		model += "    begin\n";
		model += "      return self.name;\n";
		model += "    end;\n";
		model += "    operation setName(name : String);\n";
		model += "    begin\n";
		model += "      self.name := name;\n";
		model += "    end;\n";
        model += "    operation toString() : String;\n";
        model += "    begin\n";
        model += "      return \"name: \" + self.name;\n";
        model += "    end;\n";
        model += "    operation hasRole(r : Role) : Boolean;\n";
        model += "    begin\n";
        model += "      return r = self.personRole;\n";
        model += "    end;\n";
        model += "    operation hasAnyRole() : Boolean;\n";
        model += "    begin\n";
        model += "      return not(self.personRole == null);\n";
        model += "    end;\n";
        model += "  end;\n";        
		model += "end.";
	}

	public static Test suite() {
		return new TestSuite(RuntimeObjectTests.class);
	}

	public RuntimeObjectTests(String name) {
		super(name);
	}
	
	public void testObjectCreation() throws CoreException {
		String[] sources = { model };
		parseAndCheck(sources);
		RuntimeClass personClass = getRuntimeClass("tests::Person");
		assertNotNull(personClass);
		RuntimeObject person = personClass.newInstance();
		assertNotNull(person);
		assertNotNull(person.getKey());
		
		assertNull(personClass.getInstance(person.getKey()));
		assertFalse(personClass.getAllInstances().contains(person));
		
		person.attach();

		assertNotNull(personClass.getInstance(person.getKey()));
		assertTrue(personClass.getAllInstances().contains(person));
	}
		
	public void testObjectCreation_DefaultValues() throws CoreException {
		String[] sources = { model };
		parseAndCheck(sources);
		RuntimeClass personClass = getRuntimeClass("tests::Person");
		RuntimeObject person = personClass.newInstance();

		// test initial values
		assertEquals(BooleanType.TRUE,  readAttribute(person, "active"));
		BasicType personRole = readAttribute(person, "personRole");
		assertNotNull(personRole);
		assertNotNull(((EnumerationType) personRole).getValue());
		assertEquals("EMPLOYEE", ((EnumerationType) personRole).getValue().getName());
	}
	
	public void testRunOperation() throws CoreException {
		String[] sources = { model };
		parseAndCheck(sources);
		RuntimeClass personClass = getRuntimeClass("tests::Person");
		RuntimeObject person = personClass.newInstance();

		// basic operation testing
		runOperation(person, "setName", new StringType("Foo"));
		assertEquals(new StringType("Foo"), runOperation(person, "getName"));
	}

	public void testRunInterfaceOperation() throws CoreException {
		String[] sources = { model };
		parseAndCheck(sources);
		RuntimeClass personClass = getRuntimeClass("tests::Person");
		RuntimeObject person = personClass.newInstance();
        writeAttribute(person, "name", new StringType("Foo"));
		
		// interface operation testing
		Operation operation = get("tests::Named::getName", UMLPackage.Literals.OPERATION);
		assertEquals(new StringType("Foo"), getRuntime().runOperation(null, person, operation));
	}
	
	public void testWriteReadAttribute() throws CoreException {
		String[] sources = { model };
		parseAndCheck(sources);
		RuntimeClass personClass = getRuntimeClass("tests::Person");
		RuntimeObject person = personClass.newInstance();

		// basic attribute testing
		writeAttribute(person, "name", new StringType("Bar"));
		assertEquals(new StringType("Bar"), readAttribute(person, "name"));
	}
	
	public void testReadStaticDerivedAttribute() throws CoreException {
		String source = "";
		source += "model tests;\n";
		source += "  import base;\n";
		source += "  class MyClass\n";
		source += "    derived static attribute count : Integer := { 1 };\n";
        source += "  end;\n";        
		source += "end.";

		parseAndCheck(new String[] { source });
		RuntimeClass personClass = getRuntimeClass("tests::MyClass");
		BasicType read = readAttribute(personClass.getClassObject(), "count");
		assertEquals(new IntegerType(1), read);
	}

	
    public void testToString() throws CoreException {
        String[] sources = { model };
		parseAndCheck(sources);
        RuntimeClass personClass = getRuntimeClass("tests::Person");
        RuntimeObject person = personClass.newInstance();
        writeAttribute(person, "name", new StringType("Bar"));
        StringType result = (StringType) runOperation(person, "toString");
        assertEquals("name: Bar", result.primitiveValue());
    }
    
    public void testEnumerationEquals() throws CoreException {
        String[] sources = { model };
		parseAndCheck(sources);
        RuntimeClass personClass = getRuntimeClass("tests::Person");
        RuntimeObject person = personClass.newInstance();
        assertTrue(((BooleanType) runOperation(person, "hasAnyRole")).primitiveValue());
        Enumeration role = getRepository().findNamedElement("tests::Role", UMLPackage.Literals.ENUMERATION, null);
        writeAttribute(person, "personRole", new EnumerationType(role.getOwnedLiteral("OWNER")));
        assertTrue(((BooleanType) runOperation(person, "hasRole", new EnumerationType(role.getOwnedLiteral("OWNER")))).primitiveValue());
        assertFalse(((BooleanType) runOperation(person, "hasRole", new EnumerationType(role.getOwnedLiteral("EMPLOYEE")))).primitiveValue());
        assertTrue(((BooleanType) runOperation(person, "hasAnyRole")).primitiveValue());
        writeAttribute(person, "personRole", null);
        assertFalse(((BooleanType) runOperation(person, "hasAnyRole")).primitiveValue());
    }
	
	public void testFactoryInvocation() throws CoreException {
		String[] sources = { model };
		parseAndCheck(sources);
		RuntimeObject newPerson = (RuntimeObject) runStaticOperation("tests::Person", "newPerson", new StringType("John Doe"));
		assertNotNull(newPerson);
		assertEquals(new StringType("John Doe"), readAttribute(newPerson, "name"));
	}

	public void testObjectDestruction() throws CoreException {
		String[] sources = { model };
		parseAndCheck(sources);
		RuntimeClass personClass = getRuntimeClass("tests::Person");
		assertNotNull(personClass);
		RuntimeObject person = personClass.newInstance(true);
		person.save();
		assertTrue(personClass.getAllInstances().contains(person));
		runOperation(person, "setName", new StringType("Foo"));
		person.destroy();
		assertFalse(personClass.getAllInstances().contains(person));
		try {
			runOperation(person, "getName");
			fail("Should have failed, object has been destroyed");
		} catch (ObjectNotActiveException e) {
			// expected
		}
		try {
			readAttribute(person, "name");
			fail("Should have failed, object has been destroyed");
		} catch (ObjectNotActiveException e) {
			// expected
		}
	}
	
   @Override
    protected Properties createDefaultSettings() {
        Properties defaultSettings = super.createDefaultSettings();
        defaultSettings.setProperty(IRepository.EXTEND_BASE_OBJECT, Boolean.TRUE.toString());
        return defaultSettings;
    }
}
