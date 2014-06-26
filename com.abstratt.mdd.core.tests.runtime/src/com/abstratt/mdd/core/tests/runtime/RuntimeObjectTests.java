package com.abstratt.mdd.core.tests.runtime;

import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
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

    public static Test suite() {
        return new TestSuite(RuntimeObjectTests.class);
    }

    static String model = "";

    static {
        RuntimeObjectTests.model += "model tests;\n";
        RuntimeObjectTests.model += "  import base;\n";
        RuntimeObjectTests.model += "  enumeration Role OWNER, EMPLOYEE, FAMILY end;\n";
        RuntimeObjectTests.model += "  interface Named\n";
        RuntimeObjectTests.model += "    operation getName() : String;\n";
        RuntimeObjectTests.model += "  end;\n";
        RuntimeObjectTests.model += "  class Person implements Named\n";
        RuntimeObjectTests.model += "    attribute name : String[0,1];\n";
        RuntimeObjectTests.model += "    attribute active : Boolean := true;\n";
        RuntimeObjectTests.model += "    attribute personRole : Role := EMPLOYEE;\n";
        RuntimeObjectTests.model += "    static operation newPerson(name : String) : Person;\n";
        RuntimeObjectTests.model += "    begin\n";
        RuntimeObjectTests.model += "      var newPerson : Person;\n";
        RuntimeObjectTests.model += "      newPerson := new Person;\n";
        RuntimeObjectTests.model += "      newPerson.name := name;\n";
        RuntimeObjectTests.model += "      return newPerson;\n";
        RuntimeObjectTests.model += "    end;\n";
        RuntimeObjectTests.model += "    operation getName() : String;\n";
        RuntimeObjectTests.model += "    begin\n";
        RuntimeObjectTests.model += "      return self.name;\n";
        RuntimeObjectTests.model += "    end;\n";
        RuntimeObjectTests.model += "    operation setName(name : String);\n";
        RuntimeObjectTests.model += "    begin\n";
        RuntimeObjectTests.model += "      self.name := name;\n";
        RuntimeObjectTests.model += "    end;\n";
        RuntimeObjectTests.model += "    operation toString() : String;\n";
        RuntimeObjectTests.model += "    begin\n";
        RuntimeObjectTests.model += "      return \"name: \" + self.name;\n";
        RuntimeObjectTests.model += "    end;\n";
        RuntimeObjectTests.model += "    operation hasRole(r : Role) : Boolean;\n";
        RuntimeObjectTests.model += "    begin\n";
        RuntimeObjectTests.model += "      return r = self.personRole;\n";
        RuntimeObjectTests.model += "    end;\n";
        RuntimeObjectTests.model += "    operation hasAnyRole() : Boolean;\n";
        RuntimeObjectTests.model += "    begin\n";
        RuntimeObjectTests.model += "      return not(self.personRole == null);\n";
        RuntimeObjectTests.model += "    end;\n";
        RuntimeObjectTests.model += "  end;\n";
        RuntimeObjectTests.model += "end.";
    }

    public RuntimeObjectTests(String name) {
        super(name);
    }

    public void testEnumerationEquals() throws CoreException {
        String[] sources = { RuntimeObjectTests.model };
        parseAndCheck(sources);
        RuntimeClass personClass = getRuntimeClass("tests::Person");
        RuntimeObject person = personClass.newInstance();
        TestCase.assertTrue(((BooleanType) runOperation(person, "hasAnyRole")).primitiveValue());
        Enumeration role = getRepository().findNamedElement("tests::Role", UMLPackage.Literals.ENUMERATION, null);
        writeAttribute(person, "personRole", new EnumerationType(role.getOwnedLiteral("OWNER")));
        TestCase.assertTrue(((BooleanType) runOperation(person, "hasRole", new EnumerationType(role.getOwnedLiteral("OWNER"))))
                .primitiveValue());
        TestCase.assertFalse(((BooleanType) runOperation(person, "hasRole", new EnumerationType(role.getOwnedLiteral("EMPLOYEE"))))
                .primitiveValue());
        TestCase.assertTrue(((BooleanType) runOperation(person, "hasAnyRole")).primitiveValue());
        writeAttribute(person, "personRole", null);
        TestCase.assertFalse(((BooleanType) runOperation(person, "hasAnyRole")).primitiveValue());
    }

    public void testFactoryInvocation() throws CoreException {
        String[] sources = { RuntimeObjectTests.model };
        parseAndCheck(sources);
        RuntimeObject newPerson = (RuntimeObject) runStaticOperation("tests::Person", "newPerson", new StringType("John Doe"));
        TestCase.assertNotNull(newPerson);
        TestCase.assertEquals(new StringType("John Doe"), readAttribute(newPerson, "name"));
    }

    public void testObjectCreation() throws CoreException {
        String[] sources = { RuntimeObjectTests.model };
        parseAndCheck(sources);
        RuntimeClass personClass = getRuntimeClass("tests::Person");
        TestCase.assertNotNull(personClass);
        RuntimeObject person = personClass.newInstance();
        TestCase.assertNotNull(person);
        TestCase.assertNotNull(person.getKey());

        TestCase.assertNull(personClass.getInstance(person.getKey()));
        TestCase.assertFalse(personClass.getAllInstances().contains(person));

        person.attach();

        TestCase.assertNotNull(personClass.getInstance(person.getKey()));
        TestCase.assertTrue(personClass.getAllInstances().contains(person));
    }

    public void testObjectCreation_DefaultValues() throws CoreException {
        String[] sources = { RuntimeObjectTests.model };
        parseAndCheck(sources);
        RuntimeClass personClass = getRuntimeClass("tests::Person");
        RuntimeObject person = personClass.newInstance();

        // test initial values
        TestCase.assertEquals(BooleanType.TRUE, readAttribute(person, "active"));
        BasicType personRole = readAttribute(person, "personRole");
        TestCase.assertNotNull(personRole);
        TestCase.assertNotNull(((EnumerationType) personRole).getValue());
        TestCase.assertEquals("EMPLOYEE", ((EnumerationType) personRole).getValue().getName());
    }

    public void testObjectDestruction() throws CoreException {
        String[] sources = { RuntimeObjectTests.model };
        parseAndCheck(sources);
        RuntimeClass personClass = getRuntimeClass("tests::Person");
        TestCase.assertNotNull(personClass);
        RuntimeObject person = personClass.newInstance(true);
        person.save();
        TestCase.assertTrue(personClass.getAllInstances().contains(person));
        runOperation(person, "setName", new StringType("Foo"));
        person.destroy();
        TestCase.assertFalse(personClass.getAllInstances().contains(person));
        try {
            runOperation(person, "getName");
            TestCase.fail("Should have failed, object has been destroyed");
        } catch (ObjectNotActiveException e) {
            // expected
        }
        try {
            readAttribute(person, "name");
            TestCase.fail("Should have failed, object has been destroyed");
        } catch (ObjectNotActiveException e) {
            // expected
        }
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
        TestCase.assertEquals(new IntegerType(1), read);
    }

    public void testRunInterfaceOperation() throws CoreException {
        String[] sources = { RuntimeObjectTests.model };
        parseAndCheck(sources);
        RuntimeClass personClass = getRuntimeClass("tests::Person");
        RuntimeObject person = personClass.newInstance();
        writeAttribute(person, "name", new StringType("Foo"));

        // interface operation testing
        Operation operation = get("tests::Named::getName", UMLPackage.Literals.OPERATION);
        TestCase.assertEquals(new StringType("Foo"), getRuntime().runOperation(null, person, operation));
    }

    public void testRunOperation() throws CoreException {
        String[] sources = { RuntimeObjectTests.model };
        parseAndCheck(sources);
        RuntimeClass personClass = getRuntimeClass("tests::Person");
        RuntimeObject person = personClass.newInstance();

        // basic operation testing
        runOperation(person, "setName", new StringType("Foo"));
        TestCase.assertEquals(new StringType("Foo"), runOperation(person, "getName"));
    }

    public void testToString() throws CoreException {
        String[] sources = { RuntimeObjectTests.model };
        parseAndCheck(sources);
        RuntimeClass personClass = getRuntimeClass("tests::Person");
        RuntimeObject person = personClass.newInstance();
        writeAttribute(person, "name", new StringType("Bar"));
        StringType result = (StringType) runOperation(person, "toString");
        TestCase.assertEquals("name: Bar", result.primitiveValue());
    }

    public void testWriteReadAttribute() throws CoreException {
        String[] sources = { RuntimeObjectTests.model };
        parseAndCheck(sources);
        RuntimeClass personClass = getRuntimeClass("tests::Person");
        RuntimeObject person = personClass.newInstance();

        // basic attribute testing
        writeAttribute(person, "name", new StringType("Bar"));
        TestCase.assertEquals(new StringType("Bar"), readAttribute(person, "name"));
    }

    @Override
    protected Properties createDefaultSettings() {
        Properties defaultSettings = super.createDefaultSettings();
        defaultSettings.setProperty(IRepository.EXTEND_BASE_OBJECT, Boolean.TRUE.toString());
        return defaultSettings;
    }
}
