package com.abstratt.kirra.tests.mdd.runtime;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;

import com.abstratt.kirra.Entity;
import com.abstratt.kirra.Instance;
import com.abstratt.kirra.Repository;
import com.abstratt.kirra.mdd.core.KirraMDDCore;
import com.abstratt.mdd.core.IRepository;

public class KirraMDDRuntimeRelationshipTests extends AbstractKirraMDDRuntimeTests {

	public KirraMDDRuntimeRelationshipTests(String name) {
		super(name);
	}
	
	private static String model;
	static {
		model = "";
		model += "package mypackage;\n";
		model += "import base;\n";
		model += "class MyClass1\n";
		model += "attribute single : Integer[0,1];\n";
		model += "reference myClass2 : MyClass2[0,1];\n";
		model += "attribute myClass3s : MyClass3[*] {nonunique};\n";
		model += "attribute myClass4 : MyClass4[0,1];\n";
		model += "attribute myClass5 : MyClass5[*];\n";	
		model += "derived attribute myClass3Duplicated : MyClass3[*] {nonunique} := {self.myClass3s.union(self.myClass3s)};\n";
		model += "end;\n";
		model += "class MyClass2\n";
		model += "attribute single : Integer[0,1];\n";
		model += "end;\n";
		model += "class MyClass3\n";
		model += "attribute single : Integer[0,1];\n";
		model += "end;\n";
		model += "class MyClass4\n";
		model += "attribute single : Integer[0,1];\n";
		model += "end;\n";
		model += "class MyClass5\n";
		model += "attribute single : Integer[0,1];\n";
		model += "end;\n";
		model += "class MyClass6\n";
		model += "attribute single : Integer[0,1];\n";
		model += "attribute myClass7s : MyClass7[*];\n";
		model += "end;\n";				
		model += "class MyClass7\n";
		model += "attribute single : Integer[0,1];\n";
		model += "end;\n";		
		model += "association role MyClass1.myClass4; role myClass1 : MyClass1; end;\n";
		model += "association role MyClass1.myClass5; role myClass1 : MyClass1; end;\n";
		model += "composition role MyClass6.myClass7s; navigable role myClass6 : MyClass6; end;\n";
		model += "association role MyClass1.myClass3s; navigable role myClass1 : MyClass1; end;\n";
		model += "end.";
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
		assertNotNull(kirra.getInstance(order.getEntityNamespace(), order.getEntityName(), order.getObjectId(), false));
		assertNotNull(kirra.getInstance(item.getEntityNamespace(), item.getEntityName(), item.getObjectId(), false));
		
		// delete parent
		kirra.deleteInstance(order);
		
		kirra.saveContext();
		assertNull(kirra.getInstance(order.getEntityNamespace(), order.getEntityName(), order.getObjectId(), false));
		// should have deleted child objects as well
		assertNull(kirra.getInstance(item.getEntityNamespace(), item.getEntityName(), item.getObjectId(), false));
	}
	
	
	public void _testCreateWithTwoLinks() throws CoreException {
		parseAndCheck(model);
		Repository kirra = getKirra();

		Instance createdClass3a = kirra.createInstance(new Instance("mypackage", "MyClass3"));
		Instance createdClass3b = kirra.createInstance(new Instance("mypackage", "MyClass3"));
		List<Instance> class3Instances = Arrays.asList(createdClass3a, createdClass3b);
		
		Instance newClass1 = new Instance("mypackage", "MyClass1");
		newClass1.setRelated("myClass3", class3Instances);
		Instance createdClass1 = kirra.createInstance(newClass1);
		
		assertTrue(!createdClass1.isNew());
		assertTrue(createdClass1.isFull());
		assertNotNull(createdClass1.getRelated("myClass3"));
		assertEquals(2, createdClass1.getRelated("myClass3").size());
		List<Instance> related = createdClass1.getRelated("myClass3");
		sortInstances(related);
		sortInstances(class3Instances);
		assertEquals(class3Instances.get(0).getObjectId(), related.get(0).getObjectId());
		assertEquals(class3Instances.get(1).getObjectId(), related.get(1).getObjectId());
	}
	

	public void testCreateWithOneLink() throws CoreException {
		parseAndCheck(model);
		Repository kirra = getKirra();

		Instance createdClass2 = kirra.createInstance(new Instance("mypackage", "MyClass2"));
		
		Instance newClass1 = new Instance("mypackage", "MyClass1");
		newClass1.setRelated("myClass2", Arrays.asList(createdClass2));
		Instance createdClass1 = kirra.createInstance(newClass1);
		
		assertTrue(!createdClass1.isNew());
		assertTrue(createdClass1.isFull());
		assertNotNull(createdClass1.getRelated("myClass2"));
		assertEquals(1, createdClass1.getRelated("myClass2").size());
		assertEquals(createdClass2.getObjectId(), createdClass1.getRelated("myClass2").get(0).getObjectId());
	}
	
	public void testDefault() throws CoreException {
		parseAndCheck(model);
		Repository kirra = getKirra();

		String[] emptyClasses = {"MyClass2", "MyClass3", "MyClass4", "MyClass5"};
		
		Instance transientInstance1 = kirra.newInstance("mypackage", "MyClass1");
		assertEquals(0, transientInstance1.getValues().size());
		Collection<List<Instance>> links1 = transientInstance1.getLinks().values();
		for (List<Instance> list : links1)
			assertEquals(0, list.size());
		
		for (String className : emptyClasses) {
			Instance transientInstance = kirra.newInstance("mypackage", className);
			assertEquals(className, 0, transientInstance.getValues().size());
			Collection<List<Instance>> links = transientInstance.getLinks().values();
			for (List<Instance> list : links)
				assertEquals(0, list.size());
		}
	}

	public void testDerivedRelationship() throws CoreException {
		parseAndCheck(model);
		Repository kirra = getKirra();

		Instance newClass1 = new Instance("mypackage", "MyClass1");
		Instance createdClass1 = kirra.createInstance(newClass1);
		Instance newClass3 = new Instance("mypackage", "MyClass3");
		newClass3.setSingleRelated("myClass1", createdClass1);
		Instance createdClass3 = kirra.createInstance(newClass3);

		List<Instance> base = kirra.getRelatedInstances("mypackage", "MyClass1", createdClass1.getObjectId(), "myClass3s", true);
		assertEquals(1, base.size());
		assertEquals(createdClass3.getObjectId(), base.get(0).getObjectId());
		
		List<Instance> derived = kirra.getRelatedInstances("mypackage", "MyClass1", createdClass1.getObjectId(), "myClass3Duplicated", true);
		assertEquals(2, derived.size());
		assertEquals(createdClass3.getObjectId(), derived.get(0).getObjectId());
		assertEquals(createdClass3.getObjectId(), derived.get(1).getObjectId());
	}

	
	/**
	 * Regression test for bug #227.
	 */
	public void testReferenceToEnumeration() throws CoreException {
		String model = "";
		model += "package mypackage;\n";
		model += "apply kirra;\n";
		model += "enumeration Enum1 value1, value2, value3 end;\n";
		model += "class MyClass\n";
		model += "reference enum1 : Enum1;\n";
		model += "end;\n";
		model += "end.";
		parseAndCheck(model);
		Repository kirra = getKirra();

		Entity entity = kirra.getEntity("mypackage", "MyClass");
		assertEquals(entity.getRelationships().toString(), 0, entity.getRelationships().size());
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
		assertEquals(entity1.getRelationships().toString(), 1, entity1.getRelationships().size());
		
		Entity entity2 = kirra.getEntity("mypackage", "MyClass2");
		assertEquals(entity2.getRelationships().toString(), 0, entity2.getRelationships().size());
	}
	
	@Override
	protected Properties createDefaultSettings() {
		Properties defaultSettings = super.createDefaultSettings();
		defaultSettings.setProperty(IRepository.WEAVER, KirraMDDCore.WEAVER);
		defaultSettings.setProperty(IRepository.EXTEND_BASE_OBJECT, Boolean.TRUE.toString());
		return defaultSettings;
	}
}
