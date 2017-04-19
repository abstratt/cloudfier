package com.abstratt.kirra.tests.mdd.runtime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import com.abstratt.kirra.Instance;
import com.abstratt.kirra.KirraException;
import com.abstratt.kirra.Repository;

import junit.framework.TestCase;

public class KirraMDDRuntimeDataTests extends AbstractKirraMDDRuntimeTests {

    private static String model;

    static {
        KirraMDDRuntimeDataTests.model = "";
        KirraMDDRuntimeDataTests.model += "package mypackage;\n";
        KirraMDDRuntimeDataTests.model += "import base;\n";
        KirraMDDRuntimeDataTests.model += "apply kirra;\n";
        KirraMDDRuntimeDataTests.model += "[Entity] class MyClass1\n";
        KirraMDDRuntimeDataTests.model += "attribute attr1 : Integer[0,1] := 5;\n";
        KirraMDDRuntimeDataTests.model += "attribute attr2 : String[0,1];\n";
        KirraMDDRuntimeDataTests.model += "derived attribute attr6 : Boolean := { self.attr1 > 0 };\n";
        KirraMDDRuntimeDataTests.model += "end;\n";
        KirraMDDRuntimeDataTests.model += "[Entity] class MyClass2\n";
        KirraMDDRuntimeDataTests.model += "attribute attr3 : Boolean[0,1];\n";
        KirraMDDRuntimeDataTests.model += "attribute attr4 : Date[0,1];\n";
        KirraMDDRuntimeDataTests.model += "attribute attr5 : MyEnum[0,1] := value2;\n";
        KirraMDDRuntimeDataTests.model += "end;\n";
        KirraMDDRuntimeDataTests.model += "[Entity] class MyClass2a specializes MyClass2 end;\n";
        KirraMDDRuntimeDataTests.model += "[Entity] class MyClass2b specializes MyClass2 end;\n";
        KirraMDDRuntimeDataTests.model += "[Entity] class MyClass3\n";
        KirraMDDRuntimeDataTests.model += "attribute name : String[0,1];\n";
        KirraMDDRuntimeDataTests.model += "attribute myClass4 : MyClass4;\n";
        KirraMDDRuntimeDataTests.model += "end;\n";
        KirraMDDRuntimeDataTests.model += "[Entity] class MyClass4\n";
        KirraMDDRuntimeDataTests.model += "attribute name : String[0,1];\n";
        KirraMDDRuntimeDataTests.model += "attribute myClass3 : MyClass3;\n";
        KirraMDDRuntimeDataTests.model += "end;\n";
        KirraMDDRuntimeDataTests.model += "[Entity] class MyClass5\n";
        KirraMDDRuntimeDataTests.model += "derived id attribute myId1 : Integer;\n";
        KirraMDDRuntimeDataTests.model += "derived id attribute myId2 : String;\n";
        KirraMDDRuntimeDataTests.model += "end;\n";
        KirraMDDRuntimeDataTests.model += "association MyClass3_MyClass4\n";
        KirraMDDRuntimeDataTests.model += "role MyClass4.myClass3;\n";
        KirraMDDRuntimeDataTests.model += "role MyClass3.myClass4;\n";
        KirraMDDRuntimeDataTests.model += "end;\n";
        KirraMDDRuntimeDataTests.model += "enumeration MyEnum value1; value2; value3; end;\n";
        KirraMDDRuntimeDataTests.model += "[Entity] class MyClass6\n";
        KirraMDDRuntimeDataTests.model += "attribute myImage : Picture;\n";
        KirraMDDRuntimeDataTests.model += "end;\n";        
        KirraMDDRuntimeDataTests.model += "end.";
    }

    public KirraMDDRuntimeDataTests(String name) {
        super(name);
    }

    // disabled due to association behavior having changed
    public void __testUpdateGraph() throws CoreException {
        parseAndCheck(KirraMDDRuntimeDataTests.model);
        Repository kirra = getKirra();

        Instance newInstance3 = new Instance();
        newInstance3.setEntityName("MyClass3");
        newInstance3.setEntityNamespace("mypackage");
        newInstance3.setValue("name", "my3");
        Instance created3 = kirra.createInstance(newInstance3);

        Instance newInstance4 = new Instance();
        newInstance4.setEntityName("MyClass4");
        newInstance4.setEntityNamespace("mypackage");
        newInstance4.setValue("name", "my4");
        created3.setSingleRelated("myClass4", newInstance4);

        Instance updated3 = kirra.updateInstance(created3);
        Instance related4 = updated3.getSingleRelated("myClass4");
        TestCase.assertNotNull(related4);
        TestCase.assertEquals(newInstance4.getEntityName(), related4.getEntityName());
        TestCase.assertEquals(newInstance4.getValue("name"), related4.getValue("name"));

        Instance loaded4 = kirra.getInstance(related4.getEntityNamespace(), related4.getEntityName(), related4.getObjectId(), true);
        TestCase.assertNotNull(loaded4);
        TestCase.assertNotNull(loaded4.getSingleRelated("myClass3"));
        TestCase.assertEquals(updated3.getObjectId(), loaded4.getSingleRelated("myClass3").getObjectId());

        // try to upload a saved graph
        kirra.updateInstance(updated3);
    }

    public void testCreateInstance() throws CoreException {
        parseAndCheck(KirraMDDRuntimeDataTests.model);
        Repository kirra = getKirra();

        Instance newInstance = new Instance();
        newInstance.setEntityName("MyClass1");
        newInstance.setEntityNamespace("mypackage");
        newInstance.setValue("attr1", 10);
        newInstance.setValue("attr2", "bar");

        TestCase.assertTrue(newInstance.isNew());
        Instance created = kirra.createInstance(newInstance);
        TestCase.assertFalse(created.isNew());

        TestCase.assertEquals(10L, created.getValue("attr1"));
        TestCase.assertEquals("bar", created.getValue("attr2"));
    }

    public void testCreateInstanceWithAutoGeneratedIds() throws CoreException {
        parseAndCheck(KirraMDDRuntimeDataTests.model);
        Repository kirra = getKirra();

        Instance newInstance = new Instance("mypackage", "MyClass5");
        Instance created = kirra.createInstance(newInstance);

        TestCase.assertNotNull(created.getValue("myId1"));
        TestCase.assertNotNull(created.getValue("myId2"));
    }

    public void testDeleteInstance() throws CoreException {
        parseAndCheck(KirraMDDRuntimeDataTests.model);
        Repository kirra = getKirra();

        Instance newInstance = new Instance();
        newInstance.setEntityName("MyClass1");
        newInstance.setEntityNamespace("mypackage");
        Instance created = kirra.createInstance(newInstance);

        TestCase.assertNotNull(kirra.getInstance("mypackage", "MyClass1", created.getObjectId(), true));

        kirra.deleteInstance(created);

        TestCase.assertNull(kirra.getInstance("mypackage", "MyClass1", created.getObjectId(), true));

        // delete an already deleted object
        try {
            kirra.deleteInstance(created);
            TestCase.fail("should have failed");
        } catch (KirraException e) {
            // expected
            TestCase.assertEquals(KirraException.Kind.OBJECT_NOT_FOUND, e.getKind());
        }
    }

    public void testDerivedValues() throws CoreException {
        parseAndCheck(KirraMDDRuntimeDataTests.model);
        Repository kirra = getKirra();
        Instance created1 = kirra.newInstance("mypackage", "MyClass1");

        created1 = kirra.createInstance(created1);

        created1.setValue("attr1", 10L);
        Instance updated1 = kirra.updateInstance(created1);
        TestCase.assertEquals(10L, updated1.getValue("attr1"));
        TestCase.assertEquals(true, updated1.getValue("attr6"));

        created1.setValue("attr1", -100);
        updated1 = kirra.updateInstance(created1);
        TestCase.assertEquals(-100L, updated1.getValue("attr1"));
        TestCase.assertEquals(false, updated1.getValue("attr6"));
    }

    public void testGetAllInstances() throws CoreException {
        parseAndCheck(KirraMDDRuntimeDataTests.model);
        Repository kirra = getKirra();

        // no instances created yet
        TestCase.assertEquals(0, kirra.getInstances("mypackage", "MyClass1", true).size());
        TestCase.assertEquals(0, kirra.getInstances("mypackage", "MyClass2", true).size());

        Instance newInstance = new Instance("mypackage", "MyClass1");

        newInstance.setValue("attr1", 10);
        newInstance.setValue("attr2", "bar");
        Instance created1 = kirra.createInstance(newInstance);

        TestCase.assertEquals(1, kirra.getInstances("mypackage", "MyClass1", true).size());
        TestCase.assertEquals(0, kirra.getInstances("mypackage", "MyClass2", true).size());

        newInstance.setValue("attr1", 20);
        newInstance.setValue("attr2", "foo");
        Instance created2 = kirra.createInstance(newInstance);

        TestCase.assertEquals(2, kirra.getInstances("mypackage", "MyClass1", true).size());
        TestCase.assertEquals(0, kirra.getInstances("mypackage", "MyClass2", true).size());

        Instance newInstance2 = new Instance("mypackage", "MyClass2");
        newInstance2.setValue("attr3", true);
        newInstance2.setValue("attr4", LocalDateTime.now());
        newInstance2.setValue("attr5", "value1");
        Instance created3 = kirra.createInstance(newInstance2);

        List<Instance> myClass1Instances = kirra.getInstances("mypackage", "MyClass1", true);
        List<Instance> myClass2Instances = kirra.getInstances("mypackage", "MyClass2", true);

        TestCase.assertEquals(2, myClass1Instances.size());
        TestCase.assertEquals(1, myClass2Instances.size());

        TestCase.assertNotNull(findById(myClass1Instances, created1.getObjectId()));
        TestCase.assertNotNull(findById(myClass1Instances, created2.getObjectId()));
        TestCase.assertNotNull(findById(myClass2Instances, created3.getObjectId()));

        kirra.deleteInstance(created1);

        TestCase.assertEquals(1, kirra.getInstances("mypackage", "MyClass1", true).size());
        TestCase.assertEquals(1, kirra.getInstances("mypackage", "MyClass2", true).size());

        kirra.deleteInstance(created2);
        kirra.deleteInstance(created3);

        TestCase.assertEquals(0, kirra.getInstances("mypackage", "MyClass1", true).size());
        TestCase.assertEquals(0, kirra.getInstances("mypackage", "MyClass2", true).size());

        kirra.createInstance(new Instance("mypackage", "MyClass2a"));
        TestCase.assertEquals(1, kirra.getInstances("mypackage", "MyClass2a", true).size());
        TestCase.assertEquals(0, kirra.getInstances("mypackage", "MyClass2b", true).size());
        TestCase.assertEquals(1, kirra.getInstances("mypackage", "MyClass2", true).size());
        TestCase.assertEquals(0, kirra.getInstances("mypackage", "MyClass2", true, false).size());

        kirra.createInstance(new Instance("mypackage", "MyClass2b"));
        TestCase.assertEquals(1, kirra.getInstances("mypackage", "MyClass2a", true).size());
        TestCase.assertEquals(1, kirra.getInstances("mypackage", "MyClass2b", true).size());
        TestCase.assertEquals(2, kirra.getInstances("mypackage", "MyClass2", true).size());
        TestCase.assertEquals(0, kirra.getInstances("mypackage", "MyClass2", true, false).size());

    }

    public void testGetInstance() throws CoreException {
        parseAndCheck(KirraMDDRuntimeDataTests.model);
        Repository kirra = getKirra();

        Instance newInstance1 = new Instance();
        newInstance1.setEntityName("MyClass1");
        newInstance1.setEntityNamespace("mypackage");
        newInstance1.setValue("attr1", 10);
        newInstance1.setValue("attr2", "bar");
        Instance created1 = kirra.createInstance(newInstance1);

        Instance loaded1 = kirra.getInstance("mypackage", "MyClass1", created1.getObjectId(), true);
        TestCase.assertNotNull(loaded1);
        TestCase.assertFalse(loaded1.isNew());

        TestCase.assertEquals(10L, loaded1.getValue("attr1"));
        TestCase.assertEquals("bar", loaded1.getValue("attr2"));

        Instance newInstance2 = new Instance();
        newInstance2.setEntityName("MyClass2");
        newInstance2.setEntityNamespace("mypackage");
        newInstance2.setValue("attr3", true);
        LocalDateTime today = LocalDateTime.now();
        newInstance2.setValue("attr4", today);
        newInstance2.setValue("attr5", "value2");
        Instance created2 = kirra.createInstance(newInstance2);

        Instance loaded2 = kirra.getInstance("mypackage", "MyClass2", created2.getObjectId(), true);
        TestCase.assertNotNull(loaded2);
        TestCase.assertFalse(loaded2.isNew());

        TestCase.assertEquals(true, loaded2.getValue("attr3"));
        TestCase.assertEquals(today, loaded2.getValue("attr4"));
        TestCase.assertEquals("value2", loaded2.getValue("attr5"));
    }

    public void testNewInstanceDefaults() throws CoreException {
        parseAndCheck(KirraMDDRuntimeDataTests.model);
        Repository kirra = getKirra();
        Instance created1 = kirra.newInstance("mypackage", "MyClass1");
        TestCase.assertEquals(5L, created1.getValue("attr1"));
        TestCase.assertNull(created1.getValue("attr2"));

        Instance created2 = kirra.newInstance("mypackage", "MyClass2");
        TestCase.assertNull(created2.getValue("attr3"));
        TestCase.assertNull(created2.getValue("attr4"));
        TestCase.assertEquals("value2", created2.getValue("attr5"));
    }

    public void testSetValueWithQuotes() throws CoreException {
        parseAndCheck(KirraMDDRuntimeDataTests.model);
        Repository kirra = getKirra();

        Instance newInstance1 = new Instance();
        newInstance1.setEntityName("MyClass1");
        newInstance1.setEntityNamespace("mypackage");
        // quotes caused trouble when persisting to a relational DB
        newInstance1.setValue("attr2", "'bar'");
        Instance created1 = kirra.createInstance(newInstance1);
        TestCase.assertEquals("'bar'", created1.getValue("attr2"));
    }

    public void testUpdateInstance() throws CoreException {
        parseAndCheck(KirraMDDRuntimeDataTests.model);
        Repository kirra = getKirra();

        Instance newInstance = new Instance();
        newInstance.setEntityName("MyClass1");
        newInstance.setEntityNamespace("mypackage");
        newInstance.setValue("attr1", 10);
        newInstance.setValue("attr2", "bar");
        Instance created = kirra.createInstance(newInstance);

        created.setValue("attr1", 20);
        created.setValue("attr2", "foo");

        Instance updated = kirra.updateInstance(created);
        TestCase.assertFalse(updated.isNew());

        TestCase.assertEquals(20L, updated.getValue("attr1"));
        TestCase.assertEquals("foo", updated.getValue("attr2"));
    }
}
