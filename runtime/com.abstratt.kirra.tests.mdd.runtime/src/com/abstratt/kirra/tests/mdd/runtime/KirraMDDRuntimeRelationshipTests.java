package com.abstratt.kirra.tests.mdd.runtime;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;

import com.abstratt.kirra.Entity;
import com.abstratt.kirra.Instance;
import com.abstratt.kirra.InstanceRef;
import com.abstratt.kirra.Repository;
import com.abstratt.kirra.mdd.core.KirraMDDCore;
import com.abstratt.mdd.core.IRepository;

public class KirraMDDRuntimeRelationshipTests extends AbstractKirraMDDRuntimeTests {

    private static String sampleModel;
    private static String polymorphicRelationshipModel;

    static {
    	polymorphicRelationshipModel = "";
        polymorphicRelationshipModel += "package org;\n";
        polymorphicRelationshipModel += "import base;\n";
        polymorphicRelationshipModel += "abstract class Party\n";
        polymorphicRelationshipModel += "end;\n";
        polymorphicRelationshipModel += "class Organization specializes Party\n";
        polymorphicRelationshipModel += "attribute name : Integer[0,1];\n";
        polymorphicRelationshipModel += "attribute members : Party[*];\n";
        polymorphicRelationshipModel += "end;\n";
        polymorphicRelationshipModel += "class Person specializes Party\n";
        polymorphicRelationshipModel += "attribute firstName : String[0,1];\n";
        polymorphicRelationshipModel += "attribute lastName : String[0,1];\n";        
        polymorphicRelationshipModel += "end;\n";
        polymorphicRelationshipModel += "composition role Organization.members; navigable role organization : Organization[0,1]; end;\n";        
        polymorphicRelationshipModel += "end.";

    	
		sampleModel = "";
        sampleModel += "package mypackage;\n";
        sampleModel += "import base;\n";
        sampleModel += "class MyClass1\n";
        sampleModel += "attribute single : Integer[0,1];\n";
        sampleModel += "reference myClass2 : MyClass2[0,1];\n";
        sampleModel += "attribute myClass3s : MyClass3[*] {nonunique};\n";
        sampleModel += "attribute myClass4 : MyClass4[0,1];\n";
        sampleModel += "attribute myClass5 : MyClass5[*];\n";
        sampleModel += "derived attribute myClass3Duplicated : MyClass3[*] {nonunique} := {self.myClass3s.union(self.myClass3s)};\n";
        sampleModel += "end;\n";
        sampleModel += "class MyClass2\n";
        sampleModel += "attribute single : Integer[0,1];\n";
        sampleModel += "end;\n";
        sampleModel += "class MyClass3\n";
        sampleModel += "attribute single : Integer[0,1];\n";
        sampleModel += "end;\n";
        sampleModel += "class MyClass4\n";
        sampleModel += "attribute single : Integer[0,1];\n";
        sampleModel += "end;\n";
        sampleModel += "class MyClass5\n";
        sampleModel += "attribute single : Integer[0,1];\n";
        sampleModel += "end;\n";
        sampleModel += "class MyClass6\n";
        sampleModel += "attribute single : Integer[0,1];\n";
        sampleModel += "attribute myClass7s : MyClass7[*];\n";
        sampleModel += "end;\n";
        sampleModel += "class MyClass7\n";
        sampleModel += "attribute single : Integer[0,1];\n";
        sampleModel += "end;\n";
        sampleModel += "association role MyClass1.myClass4; role myClass1 : MyClass1; end;\n";
        sampleModel += "association role MyClass1.myClass5; role myClass1 : MyClass1; end;\n";
        sampleModel += "composition role MyClass6.myClass7s; navigable role myClass6 : MyClass6; end;\n";
        sampleModel += "association role MyClass1.myClass3s; navigable role myClass1 : MyClass1; end;\n";
        sampleModel += "end.";
    }

    public KirraMDDRuntimeRelationshipTests(String name) {
        super(name);
    }

    public void testAttributePointingToEntity() throws CoreException {
        String model = "";
        model += "package mypackage;\n";
        model += "class MyClass\n";
        model += "attribute single : Integer[0,1];\n";
        model += "attribute attr1 : MyClass2;\n";
        model += "end;\n";
        model += "class MyClass2\n";
        model += "attribute single : Integer[0,1];\n";
        model += "end;\n";
        model += "end.";
        parseAndCheck(model);
        Repository kirra = getKirra();

        Entity entity1 = kirra.getEntity("mypackage", "MyClass");
        TestCase.assertEquals(entity1.getRelationships().toString(), 1, entity1.getRelationships().size());

        Entity entity2 = kirra.getEntity("mypackage", "MyClass2");
        TestCase.assertEquals(entity2.getRelationships().toString(), 0, entity2.getRelationships().size());
    }

    public void testCascadeDelete() throws CoreException {
        String model = "";
        model += "package orderapp;\n";
        model += "import base;\n";
        model += "class Order\n";
        model += "attribute orderNo : Integer[0,1];\n";
        model += "attribute orderItems : Item[*];\n";
        model += "end;\n";
        model += "class Item\n";
        model += "attribute itemNo : Integer[0,1];\n";
        model += "end;\n";
        model += "composition role Order.orderItems; navigable role itemOrder : Order; end;\n";
        model += "end.";

        parseAndCheck(model);
        Repository kirra = getKirra();

        Instance newOrder = new Instance("orderapp", "Order");
        Instance order = kirra.createInstance(newOrder);
        Instance newItem = new Instance("orderapp", "Item");
        newItem.setSingleRelated("itemOrder", order);
        Instance item = kirra.createInstance(newItem);

        kirra.saveContext();
        TestCase.assertNotNull(kirra.getInstance(order.getEntityNamespace(), order.getEntityName(), order.getObjectId(), false));
        TestCase.assertNotNull(kirra.getInstance(item.getEntityNamespace(), item.getEntityName(), item.getObjectId(), false));

        // delete parent
        kirra.deleteInstance(order);

        kirra.saveContext();
        TestCase.assertNull(kirra.getInstance(order.getEntityNamespace(), order.getEntityName(), order.getObjectId(), false));
        // should have deleted child objects as well
        TestCase.assertNull(kirra.getInstance(item.getEntityNamespace(), item.getEntityName(), item.getObjectId(), false));
    }

    public void testCascadeDelete_PolymorphicRelationship() throws CoreException {
        parseAndCheck(polymorphicRelationshipModel);
        Repository kirra = getKirra();

        Instance newOrg = new Instance("org", "Organization");
        Instance org = kirra.createInstance(newOrg);
        Instance newPerson = new Instance("org", "Person");
        newPerson.setSingleRelated("organization", org);
        Instance person = kirra.createInstance(newPerson);

        kirra.saveContext();
        assertNotNull(kirra.getInstance(org.getEntityNamespace(), org.getEntityName(), org.getObjectId(), false));
        assertNotNull(kirra.getInstance(person.getEntityNamespace(), person.getEntityName(), person.getObjectId(), false));

        // delete parent
        kirra.deleteInstance(org);

        kirra.saveContext();
        assertNull(kirra.getInstance(org.getEntityNamespace(), org.getEntityName(), org.getObjectId(), false));
        // should have deleted child objects as well
        assertNull(kirra.getInstance(person.getEntityNamespace(), person.getEntityName(), person.getObjectId(), false));
    }

    public void testPolymorphicRelationship() throws CoreException {
        parseAndCheck(polymorphicRelationshipModel);
        Repository kirra = getKirra();

        Instance newOrg = new Instance("org", "Organization");
        Instance org1 = kirra.createInstance(newOrg);
        newOrg.setSingleRelated("organization", org1);
        Instance org2 = kirra.createInstance(newOrg);
        Instance newPerson = new Instance("org", "Person");
        newPerson.setSingleRelated("organization", org1);
        Instance person = kirra.createInstance(newPerson);

        kirra.saveContext();
        List<Instance> members = kirra.getRelatedInstances(org1.getEntityNamespace(), org1.getEntityName(), org1.getObjectId(), "members", false);
		assertEquals(2, members.size());
		
		Set<InstanceRef> memberRefs = members.stream().map(instance -> instance.getReference()).collect(Collectors.toSet());
		
		assertTrue(memberRefs.contains(org2.getReference()));
		assertTrue(memberRefs.contains(person.getReference()));
    }
    
    public void testCreateWithOneLink() throws CoreException {
        parseAndCheck(sampleModel);
        Repository kirra = getKirra();

        Instance createdClass2 = kirra.createInstance(new Instance("mypackage", "MyClass2"));

        Instance newClass1 = new Instance("mypackage", "MyClass1");
        newClass1.setRelated("myClass2", createdClass2);
        Instance createdClass1 = kirra.createInstance(newClass1);

        TestCase.assertTrue(!createdClass1.isNew());
        TestCase.assertTrue(createdClass1.isFull());
        TestCase.assertNotNull(createdClass1.getRelated("myClass2"));
        TestCase.assertEquals(createdClass2.getObjectId(), createdClass1.getRelated("myClass2").getObjectId());
    }

    public void testDefault() throws CoreException {
        parseAndCheck(sampleModel);
        Repository kirra = getKirra();

        String[] emptyClasses = { "MyClass2", "MyClass3", "MyClass4", "MyClass5" };

        Instance transientInstance1 = kirra.newInstance("mypackage", "MyClass1");
        TestCase.assertEquals(0, transientInstance1.getValues().size());
        for (Instance link : transientInstance1.getLinks().values())
            TestCase.assertNull(link);

        for (String className : emptyClasses) {
            Instance transientInstance = kirra.newInstance("mypackage", className);
            TestCase.assertEquals(className, 0, transientInstance.getValues().size());
            for (Instance link : transientInstance.getLinks().values())
            	TestCase.assertNull(link);
        }
    }

    public void testDerivedRelationship() throws CoreException {
        parseAndCheck(sampleModel);
        Repository kirra = getKirra();

        Instance newClass1 = new Instance("mypackage", "MyClass1");
        Instance createdClass1 = kirra.createInstance(newClass1);
        Instance newClass3 = new Instance("mypackage", "MyClass3");
        newClass3.setSingleRelated("myClass1", createdClass1);
        Instance createdClass3 = kirra.createInstance(newClass3);

        List<Instance> base = kirra.getRelatedInstances("mypackage", "MyClass1", createdClass1.getObjectId(), "myClass3s", true);
        TestCase.assertEquals(1, base.size());
        TestCase.assertEquals(createdClass3.getObjectId(), base.get(0).getObjectId());

        List<Instance> derived = kirra
                .getRelatedInstances("mypackage", "MyClass1", createdClass1.getObjectId(), "myClass3Duplicated", true);
        TestCase.assertEquals(2, derived.size());
        TestCase.assertEquals(createdClass3.getObjectId(), derived.get(0).getObjectId());
        TestCase.assertEquals(createdClass3.getObjectId(), derived.get(1).getObjectId());
    }

    /**
     * Regression test for bug #227.
     */
    public void testReferenceToEnumeration() throws CoreException {
        String model = "";
        model += "package mypackage;\n";
        model += "apply kirra;\n";
        model += "enumeration Enum1 value1; value2; value3; end;\n";
        model += "class MyClass\n";
        model += "reference enum1 : Enum1;\n";
        model += "end;\n";
        model += "end.";
        parseAndCheck(model);
        Repository kirra = getKirra();

        Entity entity = kirra.getEntity("mypackage", "MyClass");
        TestCase.assertEquals(entity.getRelationships().toString(), 0, entity.getRelationships().size());
    }

    @Override
    protected Properties createDefaultSettings() {
        Properties defaultSettings = super.createDefaultSettings();
        defaultSettings.setProperty(IRepository.WEAVER, KirraMDDCore.WEAVER);
        defaultSettings.setProperty(IRepository.EXTEND_BASE_OBJECT, Boolean.TRUE.toString());
        return defaultSettings;
    }
}
