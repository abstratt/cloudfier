package com.abstratt.kirra.tests.mdd.runtime;

import java.util.Arrays;
import java.util.Collections;

import junit.framework.TestCase;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.CoreException;

import com.abstratt.kirra.Instance;
import com.abstratt.kirra.KirraException;
import com.abstratt.kirra.KirraException.Kind;
import com.abstratt.kirra.Repository;

public class KirraMDDRuntimeValidationTests extends AbstractKirraMDDRuntimeTests {

    private static String model;

    static {
        KirraMDDRuntimeValidationTests.model = "";
        KirraMDDRuntimeValidationTests.model += "package mypackage;\n";
        KirraMDDRuntimeValidationTests.model += "import base;\n";
        KirraMDDRuntimeValidationTests.model += "apply kirra;\n";
        KirraMDDRuntimeValidationTests.model += "[Entity] class MyClass1\n";
        KirraMDDRuntimeValidationTests.model += "attribute attr1 : Integer invariant { self.attr1 > 50 };\n";
        KirraMDDRuntimeValidationTests.model += "attribute attr2 : String;\n";
        KirraMDDRuntimeValidationTests.model += "derived attribute attr2a : String := \"\";\n";
        KirraMDDRuntimeValidationTests.model += "end;\n";
        KirraMDDRuntimeValidationTests.model += "[Entity] class MyClass2\n";
        KirraMDDRuntimeValidationTests.model += "attribute attr3 : String;\n";
        KirraMDDRuntimeValidationTests.model += "reference ref1 : MyClass1[0,1];\n";
        KirraMDDRuntimeValidationTests.model += "end;\n";
        KirraMDDRuntimeValidationTests.model += "[Entity] class MyClass3\n";
        KirraMDDRuntimeValidationTests.model += "attribute attr4 : String[0,1];\n";
        KirraMDDRuntimeValidationTests.model += "end;\n";
        KirraMDDRuntimeValidationTests.model += "[Entity] class MyClass4\n";
        KirraMDDRuntimeValidationTests.model += "attribute attr5 : String;\n";
        KirraMDDRuntimeValidationTests.model += "reference ref2 : MyClass3;\n";
        KirraMDDRuntimeValidationTests.model += "derived attribute ref2a : MyClass3 := { self.ref2 };\n";
        KirraMDDRuntimeValidationTests.model += "end;\n";
        KirraMDDRuntimeValidationTests.model += "[Entity] class MyClass5\n";
        KirraMDDRuntimeValidationTests.model += "attribute attr6 : String;\n";
        KirraMDDRuntimeValidationTests.model += "derived attribute attr7 : String := \"\";\n";
        KirraMDDRuntimeValidationTests.model += "end;\n";
        KirraMDDRuntimeValidationTests.model += "[Entity] class MyClass6\n";
        KirraMDDRuntimeValidationTests.model += "id attribute attr8 : String;\n";
        KirraMDDRuntimeValidationTests.model += "end;\n";

        KirraMDDRuntimeValidationTests.model += "end.";
    }

    public KirraMDDRuntimeValidationTests(String name) {
        super(name);
    }

    // no current support to saving a related object for reachability
    public void _testRelshipReqPropertyCreateInstance() throws CoreException {
        parseAndCheck(KirraMDDRuntimeValidationTests.model);
        Repository kirra = getKirra();

        Instance newInstance1 = kirra.newInstance("mypackage", "MyClass1");
        newInstance1.setValue("attr1", 1000);
        // missing required attr2

        Instance newInstance2 = kirra.newInstance("mypackage", "MyClass2");
        newInstance2.setValue("attr3", "bar");
        newInstance2.setRelated("ref1", Arrays.asList(newInstance1));

        try {
            kirra.createInstance(newInstance2);
            kirra.saveContext();
            TestCase.fail();
        } catch (KirraException e) {
            TestCase.assertEquals(e.toString(), KirraException.Kind.VALIDATION, e.getKind());
            TestCase.assertTrue(e.toString(), e.getMessage().contains("attr2"));
        }

        newInstance1.setValue("attr2", "foo");

        kirra.createInstance(newInstance2);
    }

    public void testAllClear() throws CoreException {
        setupRuntime();
    }

    public void testAttributeInvariant() throws CoreException {
        parseAndCheck(KirraMDDRuntimeValidationTests.model);
        Repository kirra = getKirra();

        Instance newInstance = kirra.newInstance("mypackage", "MyClass1");
        newInstance.setValue("attr1", 100);
        newInstance.setValue("attr2", "Foo");
        Instance created = kirra.createInstance(newInstance);
        created.setValue("attr1", 50);
        kirra.updateInstance(created);
        try {
            kirra.saveContext();
            TestCase.fail();
        } catch (KirraException e) {
            TestCase.assertTrue(e.toString(), e.getMessage().contains("attr1"));
            TestCase.assertEquals(e.toString(), Kind.VALIDATION, e.getKind());
        }
    }

    public void testDerivedAttributesCreateInstance() throws CoreException {
        parseAndCheck(KirraMDDRuntimeValidationTests.model);
        Repository kirra = getKirra();

        Instance newInstance = new Instance();
        newInstance.setEntityName("MyClass5");
        newInstance.setEntityNamespace("mypackage");
        try {
            kirra.createInstance(newInstance);
            kirra.saveContext();
            TestCase.fail();
        } catch (KirraException e) {
            TestCase.assertEquals(e.toString(), KirraException.Kind.VALIDATION, e.getKind());
            TestCase.assertTrue(e.toString(), StringUtils.containsIgnoreCase(e.getMessage(), "Attr6"));
        }
        newInstance.setValue("attr6", "100");
        kirra.createInstance(newInstance);
    }

    public void testReqRelshipDeleteInstance() throws CoreException {
        parseAndCheck(KirraMDDRuntimeValidationTests.model);
        Repository kirra = getKirra();

        Instance referrer = new Instance();
        referrer.setEntityName("MyClass4");
        referrer.setEntityNamespace("mypackage");
        referrer.setValue("attr5", "bar");

        Instance target1 = new Instance();
        target1.setEntityName("MyClass3");
        target1.setEntityNamespace("mypackage");
        target1 = kirra.createInstance(target1);

        Instance target2 = new Instance();
        target2.setEntityName("MyClass3");
        target2.setEntityNamespace("mypackage");
        target2 = kirra.createInstance(target2);

        referrer.setRelated("ref2", Arrays.asList(target1));

        referrer = kirra.createInstance(referrer);
        kirra.saveContext();
        try {
            kirra.deleteInstance(target1);
            kirra.saveContext();
            TestCase.fail();
        } catch (KirraException e) {
            TestCase.assertEquals(e.toString(), KirraException.Kind.VALIDATION, e.getKind());
        }
    }

    public void testReqRelshipDeleteInstance_NoLongerReferred() throws CoreException {
        parseAndCheck(KirraMDDRuntimeValidationTests.model);
        Repository kirra = getKirra();

        Instance referrer = new Instance();
        referrer.setEntityName("MyClass4");
        referrer.setEntityNamespace("mypackage");
        referrer.setValue("attr5", "bar");

        Instance target1 = new Instance();
        target1.setEntityName("MyClass3");
        target1.setEntityNamespace("mypackage");
        target1 = kirra.createInstance(target1);

        Instance target2 = new Instance();
        target2.setEntityName("MyClass3");
        target2.setEntityNamespace("mypackage");
        target2 = kirra.createInstance(target2);

        referrer.setRelated("ref2", Arrays.asList(target1));

        referrer = kirra.createInstance(referrer);
        kirra.saveContext();

        // deleting target1 would have failed as it is needed by the referrer

        // switching the referrer to target2 will allow target1 to be deleted
        referrer.setRelated("ref2", Arrays.asList(target2));
        kirra.updateInstance(referrer);
        kirra.saveContext();

        kirra.deleteInstance(target1);
        kirra.saveContext();

        // but now it is target2 that cannot be deleted
        try {
            kirra.deleteInstance(target2);
            kirra.saveContext();
            TestCase.fail();
        } catch (KirraException e) {
            TestCase.assertEquals(e.toString(), KirraException.Kind.VALIDATION, e.getKind());
        }
    }

    public void testReqRelshipExistingPeerCreateInstance() throws CoreException {
        parseAndCheck(KirraMDDRuntimeValidationTests.model);
        Repository kirra = getKirra();

        Instance newInstance = new Instance();
        newInstance.setEntityName("MyClass4");
        newInstance.setEntityNamespace("mypackage");
        newInstance.setValue("attr5", "bar");
        try {
            kirra.createInstance(newInstance);
            kirra.saveContext();
            TestCase.fail();
        } catch (KirraException e) {
            TestCase.assertEquals(e.toString(), KirraException.Kind.VALIDATION, e.getKind());
            TestCase.assertTrue(e.toString(), StringUtils.containsIgnoreCase(e.getMessage(), "Ref2"));
        }

        Instance newInstance3 = new Instance();
        newInstance3.setEntityName("MyClass3");
        newInstance3.setEntityNamespace("mypackage");

        Instance created3 = kirra.createInstance(newInstance3);

        newInstance.setRelated("ref2", Arrays.asList(created3));

        kirra.createInstance(newInstance);
    }

    public void testReqRelshipUpdateInstance() throws CoreException {
        parseAndCheck(KirraMDDRuntimeValidationTests.model);
        Repository kirra = getKirra();

        Instance newInstance = kirra.newInstance("mypackage", "MyClass4");
        newInstance.setValue("attr5", "bar");

        Instance newInstance3 = kirra.newInstance("mypackage", "MyClass3");

        newInstance.setRelated("ref2", Arrays.asList(newInstance3));

        Instance created = kirra.createInstance(newInstance);

        created.setRelated("ref2", Collections.<Instance> emptyList());
        try {
            kirra.updateInstance(created);
            kirra.saveContext();
            TestCase.fail();
        } catch (KirraException e) {
            TestCase.assertEquals(e.toString(), KirraException.Kind.VALIDATION, e.getKind());
            TestCase.assertTrue(e.toString(), e.getMessage().contains("ref2"));
        }
        created.setRelated("ref2", Arrays.asList(newInstance3));

        kirra.updateInstance(created);
    }

    public void testRequiredAttributesCreateInstance() throws CoreException {
        parseAndCheck(KirraMDDRuntimeValidationTests.model);
        Repository kirra = getKirra();

        Instance newInstance = kirra.newInstance("mypackage", "MyClass1");
        try {
            kirra.createInstance(newInstance);
            kirra.saveContext();
            TestCase.fail();
        } catch (KirraException e) {
            TestCase.assertEquals(e.toString(), KirraException.Kind.VALIDATION, e.getKind());
            TestCase.assertTrue(e.toString(), StringUtils.containsIgnoreCase(e.getMessage(), "Attr1"));
        }
        newInstance.setValue("attr1", 100);

        try {
            kirra.createInstance(newInstance);
            kirra.saveContext();
            TestCase.fail();
        } catch (KirraException e) {
            TestCase.assertEquals(e.toString(), KirraException.Kind.VALIDATION, e.getKind());
            TestCase.assertTrue(e.toString(), StringUtils.containsIgnoreCase(e.getMessage(), "Attr2"));
        }

        newInstance.setValue("attr2", "Foo");

        kirra.createInstance(newInstance);
    }

    public void testRequiredAttributesUpdateInstance() throws CoreException {
        parseAndCheck(KirraMDDRuntimeValidationTests.model);
        Repository kirra = getKirra();

        Instance newInstance = kirra.newInstance("mypackage", "MyClass1");
        newInstance.setValue("attr1", 51);
        newInstance.setValue("attr2", "bar");
        Instance created = kirra.createInstance(newInstance);

        created.setValue("attr1", null);
        created.setValue("attr2", null);

        kirra.saveContext();
        try {
            kirra.updateInstance(created);
            kirra.saveContext();
            TestCase.fail();
        } catch (KirraException e) {
            TestCase.assertEquals(e.toString(), KirraException.Kind.VALIDATION, e.getKind());
            TestCase.assertTrue(e.toString(), StringUtils.containsIgnoreCase(e.getMessage(), "Attr1"));
        }
        created.setValue("attr1", 100);

        try {
            kirra.updateInstance(created);
            kirra.saveContext();
            TestCase.fail();
        } catch (KirraException e) {
            TestCase.assertEquals(e.toString(), KirraException.Kind.VALIDATION, e.getKind());
            TestCase.assertTrue(e.toString(), StringUtils.containsIgnoreCase(e.getMessage(), "Attr2"));
        }

        created.setValue("attr2", "Foo");

        kirra.updateInstance(created);
    }

    public void testRequiredPrivateAttributes() throws CoreException {
        String model = "";
        model += "package mypackage;\n";
        model += "import base;\n";
        model += "apply kirra;\n";
        model += "[Entity] class MyClass1\n";
        model += "public attribute _attr1 : Integer := 10;\n";
        model += "derived attribute attr3 : Integer := { self._attr1 };\n";
        model += "attribute attr2 : String;\n";
        model += "end;\n";
        model += "end.";
        parseAndCheck(model);
        Repository kirra = getKirra();

        Instance newInstance = kirra.newInstance("mypackage", "MyClass1");
        newInstance.setValue("attr2", "bar");
        Instance created = kirra.createInstance(newInstance);
        TestCase.assertEquals(10L, created.getValue("attr3"));
    }

    public void testRequiredRelationshipCreateInstance() throws CoreException {
        parseAndCheck(KirraMDDRuntimeValidationTests.model);
        Repository kirra = getKirra();

        Instance newInstance = kirra.newInstance("mypackage", "MyClass4");
        newInstance.setValue("attr5", "bar");
        try {
            kirra.createInstance(newInstance);
            kirra.saveContext();
            TestCase.fail();
        } catch (KirraException e) {
            TestCase.assertEquals(e.toString(), KirraException.Kind.VALIDATION, e.getKind());
            TestCase.assertTrue(e.toString(), StringUtils.containsIgnoreCase(e.getMessage(), "Ref2"));
        }

        Instance newInstance3 = kirra.newInstance("mypackage", "MyClass3");

        newInstance.setRelated("ref2", Arrays.asList(newInstance3));

        kirra.createInstance(newInstance);
    }

    public void testUniqueAttributes() throws CoreException {
        parseAndCheck(KirraMDDRuntimeValidationTests.model);
        Repository kirra = getKirra();

        Instance newInstance = kirra.newInstance("mypackage", "MyClass6");
        newInstance.setValue("attr8", "value1");
        kirra.createInstance(newInstance);

        newInstance.setValue("attr8", "value2");
        kirra.createInstance(newInstance);

        try {
            // same value will fail
            kirra.createInstance(newInstance);
            TestCase.fail();
        } catch (KirraException e) {
            TestCase.assertEquals(e.toString(), KirraException.Kind.VALIDATION, e.getKind());
        }
    }

}
