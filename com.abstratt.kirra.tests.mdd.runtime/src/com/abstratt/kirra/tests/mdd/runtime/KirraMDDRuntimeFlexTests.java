//package com.abstratt.kirra.tests.mdd.runtime;
//
//import org.eclipse.core.runtime.CoreException;
//
//import com.abstratt.kirra.Entity;
//import com.abstratt.kirra.IClientTranslator;
//import com.abstratt.kirra.Property;
//import com.abstratt.kirra.Relationship;
//import com.abstratt.kirra.Repository;
//import com.abstratt.kirra.mdd.runtime.flex.JavaFlexTranslator;
//
//public class KirraMDDRuntimeFlexTests extends AbstractKirraMDDRuntimeTests{
//
//	private static String model;
//	static {
//		model = "";
//		model += "package mypackage;\n";
//		model += "apply kirra;\n";
//		model += "import base;\n";
//		model += "[Entity] class MyClass1\n";
//		model += "attribute attr1 : Integer;\n";
//		model += "attribute attr2 : Double;\n";
//		model += "attribute attr3 : String;\n";
//		model += "attribute attr4 : Boolean;\n";
//		model += "attribute attr5 : Date;\n";		
//		model += "end;\n";
//		model += "[Entity] class MyClass2\n";
//        model += "reference ref1 : MyClass1;\n";
//		model += "end;\n";
//		model += "end.";
//	}
//
//	public KirraMDDRuntimeFlexTests(String name) {
//		super(name);
//	}
//	
//	
//	@Override
//	public void setUp() throws Exception {
//		super.setUp();
//	}
//
//	public void testPropertyTypes() throws CoreException {
//		parseAndCheck(model);
//		Repository kirra = getKirra();
//
//		Entity entity = kirra.getEntity("mypackage", "MyClass1");
//		Property attr1 = findNamedElement(entity.getProperties(), "attr1");
//		Property attr2 = findNamedElement(entity.getProperties(), "attr2");
//		Property attr3 = findNamedElement(entity.getProperties(), "attr3");
//		Property attr4 = findNamedElement(entity.getProperties(), "attr4");
//		Property attr5 = findNamedElement(entity.getProperties(), "attr5");
//		assertEquals("int", attr1.getTypeRef().getTypeName());
//		assertEquals("Number", attr2.getTypeRef().getTypeName());
//		assertEquals("String", attr3.getTypeRef().getTypeName());
//		assertEquals("Boolean", attr4.getTypeRef().getTypeName());
//		assertEquals("Date", attr5.getTypeRef().getTypeName());
//	}
//	
//	
//	public void testReferenceTypes() throws CoreException {
//		parseAndCheck(model);
//		Repository kirra = getKirra();
//
//		Entity entity = kirra.getEntity("mypackage", "MyClass2");
//		assertNotNull(entity);
//		Relationship ref1 = findNamedElement(entity.getRelationships(), "ref1");
//		assertNotNull(ref1);
//		assertEquals("MyClass1", ref1.getTypeRef().getTypeName());
//	}
//
//
//	protected IClientTranslator getTranslator() {
//		return new JavaFlexTranslator();
//	}
//	
//}
