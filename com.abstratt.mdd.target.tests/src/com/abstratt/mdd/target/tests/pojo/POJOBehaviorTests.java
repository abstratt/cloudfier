package com.abstratt.mdd.target.tests.pojo;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.UMLPackage;

import com.abstratt.mdd.core.target.ILanguageMapper;
import com.abstratt.mdd.core.target.ITargetPlatform;
import com.abstratt.mdd.core.target.TargetCore;
import com.abstratt.mdd.core.tests.harness.AbstractRepositoryBuildingTests;
import com.abstratt.mdd.core.tests.harness.AssertHelper;

public class POJOBehaviorTests extends AbstractRepositoryBuildingTests {

    public static Test suite() {
        return new TestSuite(POJOBehaviorTests.class);
    }

    private ILanguageMapper pojoMapper;

    public POJOBehaviorTests(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        ITargetPlatform platform = TargetCore.getBuiltInPlatform("pojo");
        Assert.assertNotNull(platform);
        pojoMapper = (ILanguageMapper) platform.getMapper(null);
        Assert.assertNotNull(pojoMapper);
    }

    public void testAccessEnumValue() throws CoreException {
        String source = "";
        source += "model simple;\n";
        source += "  import base;\n";
        source += "  enumeration E VAL1, VAL2 end;\n";
        source += "  class AClass\n";
        source += "    operation getEnumValue() : E;\n";
        source += "    begin\n";
        source += "      return E#VAL1;\n";
        source += "    end;\n";
        source += "  end;\n";
        source += "end.";
        parseAndCheck(source);
        Operation getEnumValue = getRepository().findNamedElement("simple::AClass::getEnumValue", UMLPackage.Literals.OPERATION, null);
        String actual = pojoMapper.mapBehavior(getEnumValue);
        String expected = "{ return E.VAL1; } ";
        Assert.assertTrue(actual, AssertHelper.areEqual(expected, actual));
    }

    public void testCallOperation() throws CoreException {
        String source = "";
        source += "model simple;\n";
        source += "  import base;\n";
        source += "  class Simple\n";
        source += "    operation invoked(amount : Integer);\n";
        source += "    operation invoker();\n";
        source += "    begin\n";
        source += "      self.invoked(5);\n";
        source += "    end;\n";
        source += "  end;\n";
        source += "end.";
        parseAndCheck(source);
        Operation getBalanceOperation = getRepository().findNamedElement("simple::Simple::invoker", UMLPackage.Literals.OPERATION, null);
        String actual = pojoMapper.mapBehavior(getBalanceOperation);
        String expected = "{ this.invoked(5); }";
        Assert.assertTrue(actual, AssertHelper.areEqual(expected, actual));
    }

    public void testDerivedAttribute() throws CoreException {
        String source = "";
        source += "model simple;\n";
        source += "  import base;\n";
        source += "  class SomeClass\n";
        source += "    derived attribute flag : Boolean := () : Boolean {return true};\n";
        source += "  end;\n";
        source += "end.";
        parseAndCheck(source);
        org.eclipse.uml2.uml.Class clazz = getRepository().findNamedElement("simple::SomeClass", UMLPackage.Literals.CLASS, null);
        Property flagProperty = clazz.getAttribute("flag", null);
        Assert.assertNotNull(flagProperty);
        Assert.assertTrue(flagProperty.isDerived());
        String actual = pojoMapper.map(clazz);
        String expected = "package simple; public class SomeClass { public Boolean getFlag() { return true; } }";
        Assert.assertTrue(actual, AssertHelper.areEqual(expected, actual));
    }

    public void testEmptyOperation() throws CoreException {
        String source = "";
        source += "model simple;\n";
        source += "  import base;\n";
        source += "  class Account\n";
        source += "    operation balance(arg1 : Boolean, arg2 : String) : Double; begin end;\n";
        source += "  end;\n";
        source += "end.";
        parseAndCheck(source);
        org.eclipse.uml2.uml.Class clazz = getRepository().findNamedElement("simple::Account", UMLPackage.Literals.CLASS, null);
        String actual = pojoMapper.map(clazz);
        String expected = "package simple; public class Account { public Double balance(Boolean arg1, String arg2) {}}";
        Assert.assertTrue(actual, AssertHelper.areEqual(expected, actual));
    }

    public void testGetProperty() throws CoreException {
        String source = "";
        source += "model simple;\n";
        source += "  import base;\n";
        source += "  class Account\n";
        source += "    attribute balance : Integer;\n";
        source += "    operation getBalance() : Integer;\n";
        source += "    begin\n";
        source += "      return self.balance;\n";
        source += "    end;\n";
        source += "  end;\n";
        source += "end.";
        parseAndCheck(source);
        Operation getBalanceOperation = getRepository()
                .findNamedElement("simple::Account::getBalance", UMLPackage.Literals.OPERATION, null);
        String actual = pojoMapper.mapBehavior(getBalanceOperation);
        String expected = "{ return this.balance; } ";
        Assert.assertTrue(actual, AssertHelper.areEqual(expected, actual));
    }

    public void testSetProperty() throws CoreException {
        String source = "";
        source += "model simple;\n";
        source += "  import base;\n";
        source += "  class Account\n";
        source += "    attribute balance : Integer;\n";
        source += "    operation deposit(amount : Integer);\n";
        source += "    begin\n";
        source += "      self.balance := self.balance + amount;\n";
        source += "    end;\n";
        source += "  end;\n";
        source += "end.";
        parseAndCheck(source);
        Operation getBalanceOperation = getRepository().findNamedElement("simple::Account::deposit", UMLPackage.Literals.OPERATION, null);
        String actual = pojoMapper.mapBehavior(getBalanceOperation);
        String expected = "{ this.balance=this.balance+amount; }";
        Assert.assertTrue(actual, AssertHelper.areEqual(expected, actual));
    }

}
