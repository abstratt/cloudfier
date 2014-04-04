package com.abstratt.mdd.target.tests.pojo;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.uml2.uml.UMLPackage;

import com.abstratt.mdd.core.target.ILanguageMapper;
import com.abstratt.mdd.core.target.ITargetPlatform;
import com.abstratt.mdd.core.target.TargetCore;
import com.abstratt.mdd.core.tests.harness.AbstractRepositoryBuildingTests;
import com.abstratt.mdd.core.tests.harness.AssertHelper;

public class POJOStructureTests extends AbstractRepositoryBuildingTests {

	private ILanguageMapper pojoMapper;

	public POJOStructureTests(String name) {
		super(name);
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		ITargetPlatform platform = TargetCore
				.getBuiltInPlatform("pojo");
		Assert.assertNotNull(platform);
		pojoMapper = (ILanguageMapper) platform.getMapper(null);
		Assert.assertNotNull(pojoMapper);
	}

	public void testEmptyClass() throws CoreException {
		String source = "";
		source += "model simple;\n";
		source += "  class Account\n";
		source += "  end;\n";
		source += "end.";
		parseAndCheck(source);
		org.eclipse.uml2.uml.Class clazz= getRepository().findNamedElement(
				"simple::Account",
				UMLPackage.Literals.CLASS, null);
		String actual = pojoMapper.map(clazz);
		String expected = "package simple; public class Account {}";
		Assert.assertTrue(actual, AssertHelper.areEqual(expected, actual));
	}
	
	public void testSimpleReference() throws CoreException {
		String source = "";
		source += "model simple;\n";
		source += "  class Customer end;\n";
		source += "  class Account\n";
		source += "    private reference owner : Customer;\n";
		source += "  end;\n";
		source += "end.";
		parseAndCheck(source);
		org.eclipse.uml2.uml.Class clazz= getRepository().findNamedElement(
				"simple::Account",
				UMLPackage.Literals.CLASS, null);
		String actual = pojoMapper.map(clazz);
		String expected = "package simple; public class Account { private Customer owner; }";
		Assert.assertTrue(actual, AssertHelper.areEqual(expected, actual));
	}
	
	public void testReferenceAcrossPackage() throws CoreException {
		String customer = "";
		customer += "model customer;\n";
		customer += "  class Customer end;\n";
		customer += "end.";

		String account = "";
		account += "model account;\n";
		account += "  class Account\n";
		account += "    private reference owner : customer::Customer;\n";
		account += "  end;\n";
		account += "end.";
		parseAndCheck(account, customer);
		org.eclipse.uml2.uml.Class clazz= getRepository().findNamedElement(
				"account::Account",
				UMLPackage.Literals.CLASS, null);
		String actual = pojoMapper.map(clazz);
		String expected = "package account; import customer.Customer; public class Account { private Customer owner; }";
		Assert.assertTrue(actual, AssertHelper.areEqual(expected, actual));
	}

	
	public void testOrderedMultipleReference() throws CoreException {
		String source = "";
		source += "model simple;\n";
		source += "  class Transaction end;\n";
		source += "  class Account\n";
		source += "    private reference transactions : Transaction[*]{ordered};\n";
		source += "  end;\n";
		source += "end.";
		parseAndCheck(source);
		org.eclipse.uml2.uml.Class clazz= getRepository().findNamedElement(
				"simple::Account",
				UMLPackage.Literals.CLASS, null);
		String actual = pojoMapper.map(clazz);
		String expected = "package simple; import java.util.List; public class Account { private List<Transaction> transactions; }";
		Assert.assertTrue(actual, AssertHelper.areEqual(expected, actual));
	}
	
	public void testUniqueMultipleReference() throws CoreException {
		String source = "";
		source += "model simple;\n";
		source += "  class Customer end;\n";
		source += "  class Account\n";
		source += "    private reference owners : Customer[*];\n";
		source += "  end;\n";
		source += "end.";
		parseAndCheck(source);
		org.eclipse.uml2.uml.Class clazz= getRepository().findNamedElement(
				"simple::Account",
				UMLPackage.Literals.CLASS, null);
		String actual = pojoMapper.map(clazz);
		String expected = "package simple; import java.util.Set; public class Account { private Set<Customer> owners; }";
		Assert.assertTrue(actual, AssertHelper.areEqual(expected, actual));
	}

	public void testPrivateAttribute() throws CoreException {
		String source = "";
		source += "model simple;\n";
		source += "  import base;\n";
		source += "  class Account\n";
		source += "    private attribute name : String;\n";
		source += "  end;\n";
		source += "end.";
		parseAndCheck(source);
		org.eclipse.uml2.uml.Class clazz= getRepository().findNamedElement(
				"simple::Account",
				UMLPackage.Literals.CLASS, null);
		String actual = pojoMapper.map(clazz);
		String expected = "package simple; public class Account { private String name; }";
		Assert.assertTrue(actual, AssertHelper.areEqual(expected, actual));
	}
	
	public void testReadonlyAttribute() throws CoreException {
		String source = "";
		source += "model simple;\n";
		source += "  import base;\n";
		source += "  class Account\n";
		source += "    readonly attribute name : String;\n";
		source += "  end;\n";
		source += "end.";
		parseAndCheck(source);
		org.eclipse.uml2.uml.Class clazz= getRepository().findNamedElement(
				"simple::Account",
				UMLPackage.Literals.CLASS, null);
		String actual = pojoMapper.map(clazz);
		String expected = "package simple; public class Account { private String name; public String getName() { return this.name; }}";
		Assert.assertTrue(actual, AssertHelper.areEqual(expected, actual));
	}
	
	public void testPublicAttribute() throws CoreException {
		String source = "";
		source += "model simple;\n";
		source += "  import base;\n";
		source += "  class Account\n";
		source += "    attribute name : String;\n";
		source += "  end;\n";
		source += "end.";
		parseAndCheck(source);
		org.eclipse.uml2.uml.Class clazz= getRepository().findNamedElement(
				"simple::Account",
				UMLPackage.Literals.CLASS, null);
		String actual = pojoMapper.map(clazz);
		String expected = "package simple; public class Account { private String name; public String getName(){return this.name;} public void setName(String name){this.name=name;}}";
		Assert.assertTrue(actual, AssertHelper.areEqual(expected, actual));
	}
	
	public void testPropertyInitialization() throws CoreException {
		String source = "";
		source += "model simple;\n";
		source += "  import base;\n";
		source += "  class Account\n";
		source += "    private attribute number : Integer := 5;\n";
		source += "    private attribute balance : Double := 4.5;\n";
		source += "    private attribute name : String := \"foo\";\n";
		source += "  end;\n";
		source += "end.";
		parseAndCheck(source);
		org.eclipse.uml2.uml.Class clazz= getRepository().findNamedElement(
				"simple::Account",
				UMLPackage.Literals.CLASS, null);
		String actual = pojoMapper.map(clazz);
		String expected = "package simple; public class Account { private Integer number = 5; private Double balance = 4.5;private String name = \"foo\";}";
		Assert.assertTrue(actual, AssertHelper.areEqual(expected, actual));
	}

	public static Test suite() {
		return new TestSuite(POJOStructureTests.class);
	}

}
