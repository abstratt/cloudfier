package com.abstratt.kirra.tests.mdd.runtime;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.UMLPackage;

import com.abstratt.kirra.Entity;
import com.abstratt.kirra.KirraException;
import com.abstratt.kirra.KirraException.Kind;
import com.abstratt.kirra.Namespace;
import com.abstratt.kirra.Operation;
import com.abstratt.kirra.Parameter;
import com.abstratt.kirra.Property;
import com.abstratt.kirra.Relationship;
import com.abstratt.kirra.Repository;
import com.abstratt.kirra.Schema;
import com.abstratt.kirra.TypeRef.TypeKind;

public class KirraMDDRuntimeSchemaTests extends AbstractKirraMDDRuntimeTests {
	
	private final static String library = "package datatypes;\n"
	    + "primitive Integer;\n" 
	    + "primitive String;\n"
		+ "primitive Boolean;\n"	    
	    + "end.";


	public KirraMDDRuntimeSchemaTests(String name) {
		super(name);
	}
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
	}
	
	public void testNamespaces() throws CoreException {
		parseAndCheck("package pack1;end.","package pack2;end.", "package pack3;apply kirra;class Class1 attribute singleAttribute : Integer; end; end.");
		
		assertNotNull(getRepository().findPackage("mdd_extensions", UMLPackage.Literals.PROFILE));
		assertNotNull(getRepository().findPackage("kirra", UMLPackage.Literals.PROFILE));
		
		Repository kirra = getKirra();
		List<String> namespaces = kirra.getNamespaces();
		// profile is not expected to show
		assertFalse(namespaces.contains("kirra"));
		assertEquals(1, namespaces.size());
		assertTrue(namespaces.contains("pack3"));
	}
	
	public void testSchema() throws CoreException {
		String model1 = "package pack1;apply kirra;class Class1 attribute singleAttribute : Integer; end; end.";
		String model2 = "package pack2;apply kirra;class Class2 attribute singleAttribute : Integer; end; end.";
		String model3 = "package pack2;apply kirra;end.";
		parseAndCheck(model1, model2, model3, library);
		Repository kirra = getKirra();

		Schema schema = kirra.getSchema();
		assertNotNull(schema);

		List<Namespace> namespaces = schema.getNamespaces();
		assertEquals(2, namespaces.size());
		sortNamedElements(namespaces);
		assertEquals("pack1", namespaces.get(0).getName());
		assertEquals("pack2", namespaces.get(1).getName());
		
		List<Entity> namespace1Entities = namespaces.get(0).getEntities();
		assertEquals(1, namespace1Entities.size());
		assertEquals("pack1", namespace1Entities.get(0).getEntityNamespace());
		assertEquals("Class1", namespace1Entities.get(0).getName());
		
		List<Entity> namespace2Entities = namespaces.get(1).getEntities();
		// not expected to see Class3 as it is attribute-less
		assertEquals(1, namespace2Entities.size());
		assertEquals("pack2", namespace2Entities.get(0).getEntityNamespace());
		assertEquals("Class2", namespace2Entities.get(0).getName());
	}
	
	public void testEntityAttributeIsReference() throws CoreException {
		String model = "";
		model += "package mypackage;\n";
		model += "import mdd_types;\n";
		model += "class MyClass1\n";
		model += "attribute ref1 : MyClass2;\n";
		model += "attribute attr1 : String;\n";
		model += "end;\n";
		model += "class MyClass2\n";
		model += "attribute attr2 : String;\n";
		model += "end;\n";
		model += "end.";
		parseAndCheck(model);
		Repository kirra = getKirra();

		Entity entity1 = kirra.getEntity("mypackage", "MyClass1");
		Entity entity2 = kirra.getEntity("mypackage", "MyClass2");
		
		assertNotNull(entity1);
		assertNotNull(entity2);
		
		assertEquals(1, entity1.getProperties().size());
		assertEquals(1, entity2.getProperties().size());
		
		assertEquals(1, entity1.getRelationships().size());
		assertEquals(0, entity2.getRelationships().size());
		
		assertEquals("ref1", entity1.getRelationships().get(0).getName());
		assertEquals(Relationship.Style.LINK, entity1.getRelationships().get(0).getStyle());
		assertEquals(entity2.getTypeRef(), entity1.getRelationships().get(0).getTypeRef());
		assertNull(entity1.getRelationships().get(0).getOpposite());
	}

	
	public void testEntityProperties() throws CoreException {
		String model = "";
		model += "package mypackage;\n";
		model += "apply kirra;\n";
		model += "import mdd_types;\n";
		model += "class MyClass1\n";
		model += "[Essential] attribute attr1 : Integer;\n";
		model += "[Essential] attribute attr2 : String;\n";
		model += "end;\n";
		model += "class MyClass2\n";
		model += "attribute attr1 : Integer[0, 1];\n";
		model += "attribute attr2 : String[0, *];\n";
		model += "attribute attr3 : String[1, *];\n";
		model += "attribute attr4 : String[1, 1];\n";
		model += "derived attribute attr5 : String[1, 1] := { \"foo\" };\n";
		model += "end;\n";
		model += "end.";
		parseAndCheck(model, library);
		Repository kirra = getKirra();

		Entity entity = kirra.getEntity("mypackage", "MyClass1");
		List<Property> properties = entity.getProperties();
		assertEquals(2, properties.size());
		
		sortNamedElements(properties);
		
		assertEquals("attr1", properties.get(0).getName());
		assertEquals("Integer", properties.get(0).getTypeRef().getTypeName());
		
		assertEquals("attr2", properties.get(1).getName());
		assertEquals("String", properties.get(1).getTypeRef().getTypeName());
		
		entity = kirra.getEntity("mypackage", "MyClass2");
		properties = entity.getProperties();
		assertEquals(5, properties.size());
		
		sortNamedElements(properties);
		
		assertEquals("attr1", properties.get(0).getName());
		assertFalse(properties.get(0).isRequired());
		assertFalse(properties.get(0).isMultiple());
		assertFalse(properties.get(0).isDerived());
		
		assertEquals("attr2", properties.get(1).getName());
		assertFalse(properties.get(1).isRequired());
		assertTrue(properties.get(1).isMultiple());
		assertFalse(properties.get(1).isDerived());
		
		assertEquals("attr3", properties.get(2).getName());
		assertTrue(properties.get(2).isRequired());
		assertTrue(properties.get(2).isMultiple());
		assertFalse(properties.get(2).isDerived());

		assertEquals("attr4", properties.get(3).getName());
		assertTrue(properties.get(3).isRequired());
		assertFalse(properties.get(3).isMultiple());
		assertFalse(properties.get(3).isDerived());
		
		assertEquals("attr5", properties.get(4).getName());
		assertFalse(properties.get(4).isRequired());
		assertFalse(properties.get(4).isMultiple());
		assertTrue(properties.get(4).isDerived());
	}
	
	public void testEntityRelationships() throws CoreException {
		String model = "";
		model += "package mypackage;\n";
		model += "import datatypes;\n";
		model += "apply kirra;\n";
		model += "class MyClass1\n";
		model += "attribute singleAttribute : Integer;\n";
		model += "reference myClass2 : MyClass2;\n";
		model += "reference myClass3 : MyClass3[0,*];\n";
		model += "attribute myClass4 : MyClass4[0,1];\n";
		model += "attribute myClass5 : MyClass5[1,*];\n";
		model += "attribute myClass6 : MyClass6;\n";		
		model += "derived attribute myClass3Derived : MyClass3[0,*] :=  { self.myClass3 };\n";
		model += "end;\n";
		model += "class MyClass2\n";
		model += "attribute singleAttribute : Integer;\n"; 
		model += "end;\n";
		model += "class MyClass3\n";
		model += "attribute singleAttribute : Integer;\n";
		model += "end;\n";
		model += "class MyClass4\n";
		model += "attribute singleAttribute : Integer;\n";
		model += "end;\n";
		model += "class MyClass5\n";
		model += "attribute singleAttribute : Integer;\n";
		model += "end;\n";
        model += "abstract class MyClass6\n";
        model += "attribute singleAttribute : Integer;\n";
        model += "end;\n";		
        model += "class MyClass7\n";
        model += "attribute singleAttribute : Integer;\n";
        model += "end;\n";
		model += "association role MyClass1.myClass4; role myClass1 : MyClass1; end;\n";
		model += "association role MyClass1.myClass5; role myClass1 : MyClass1; end;\n";
		model += "association role myRole3 : MyClass3; navigable role myRole2 : MyClass2[1,*]; end;\n";
		model += "association navigable role myRole2 : MyClass2[1,*]; role myRole7 : MyClass7; end;\n";
		model += "association navigable role myRole4 : MyClass4[*]; navigable role myRole5 : MyClass5[*]; end;\n";
		model += "end.";
		parseAndCheck(library, model);
		Repository kirra = getKirra();

		Entity myClass1 = kirra.getEntity("mypackage", "MyClass1");
		List<Relationship> myClass1Relationships = myClass1.getRelationships();
		assertEquals(myClass1Relationships.toString(), 6, myClass1Relationships.size());

		sortNamedElements(myClass1Relationships);

		int index = 0;
		assertEquals("myClass2", myClass1Relationships.get(index).getName());
		assertEquals("MyClass2", myClass1Relationships.get(index)
				.getTypeRef().getTypeName());
		assertTrue(myClass1Relationships.get(index).isRequired());
		assertFalse(myClass1Relationships.get(index).isMultiple());
		assertTrue(myClass1Relationships.get(index).isVisible());
		assertFalse(myClass1Relationships.get(index).isDerived());

		index++;
		assertEquals("myClass3", myClass1Relationships.get(index).getName());
		assertEquals("MyClass3", myClass1Relationships.get(index)
				.getTypeRef().getTypeName());
		assertFalse(myClass1Relationships.get(index).isRequired());
		assertTrue(myClass1Relationships.get(index).isMultiple());
		assertTrue(myClass1Relationships.get(index).isVisible());
		assertFalse(myClass1Relationships.get(index).isDerived());
		
		index++;
		assertEquals("myClass3Derived", myClass1Relationships.get(index).getName());
		assertEquals("MyClass3", myClass1Relationships.get(index)
				.getTypeRef().getTypeName());
		assertFalse(myClass1Relationships.get(index).isRequired());
		assertTrue(myClass1Relationships.get(index).isMultiple());
		assertTrue(myClass1Relationships.get(index).isVisible());
		assertTrue(myClass1Relationships.get(index).isDerived());
		
		index++;
		assertEquals("myClass4", myClass1Relationships.get(index).getName());
		assertEquals("MyClass4", myClass1Relationships.get(index)
				.getTypeRef().getTypeName());
		assertFalse(myClass1Relationships.get(index).isRequired());
		assertFalse(myClass1Relationships.get(index).isMultiple());
		assertTrue(myClass1Relationships.get(index).isVisible());
		assertFalse(myClass1Relationships.get(index).isDerived());

		index++;
		assertEquals("myClass5", myClass1Relationships.get(index).getName());
		assertEquals("MyClass5", myClass1Relationships.get(index)
				.getTypeRef().getTypeName());
		assertTrue(myClass1Relationships.get(index).isRequired());
		assertTrue(myClass1Relationships.get(index).isMultiple());
		assertTrue(myClass1Relationships.get(index).isVisible());
		assertFalse(myClass1Relationships.get(index).isDerived());
		
        index++;
        assertEquals("myClass6", myClass1Relationships.get(index).getName());
        assertEquals("MyClass6", myClass1Relationships.get(index)
                .getTypeRef().getTypeName());
        assertTrue(myClass1Relationships.get(index).isRequired());
        assertFalse(myClass1Relationships.get(index).isMultiple());
        assertTrue(myClass1Relationships.get(index).isVisible());
        assertFalse(myClass1Relationships.get(index).isDerived());
		
		Entity myClass3 = kirra.getEntity("mypackage", "MyClass3");
        List<Relationship> myClass3Relationships = myClass3.getRelationships();
        assertEquals(myClass3Relationships.toString(), 1, myClass3Relationships.size());
        assertEquals("myRole2", myClass3Relationships.get(0).getName());
        assertEquals("MyClass2", myClass3Relationships.get(0)
                .getTypeRef().getTypeName());
        assertTrue(myClass3Relationships.get(0).isPrimary());
        assertTrue(myClass3Relationships.get(0).isRequired());
        assertTrue(myClass3Relationships.get(0).isMultiple());
        assertTrue(myClass3Relationships.get(0).isVisible());
        assertFalse(myClass3Relationships.get(0).isDerived());	
        
        Entity myClass4 = kirra.getEntity("mypackage", "MyClass4");
        List<Relationship> myClass4Relationships = myClass4.getRelationships();
        assertEquals(myClass4Relationships.toString(), 2, myClass4Relationships.size());
        assertEquals("myClass1", myClass4Relationships.get(0).getName());
        assertEquals("myRole5", myClass4Relationships.get(1).getName());
        assertEquals("MyClass1", myClass4Relationships.get(0)
                .getTypeRef().getTypeName());
        assertEquals("MyClass5", myClass4Relationships.get(1)
                .getTypeRef().getTypeName());
        
        assertTrue(myClass4Relationships.get(0).isRequired());
        assertFalse(myClass4Relationships.get(0).isMultiple());
        assertFalse(myClass4Relationships.get(0).isVisible());
        assertFalse(myClass4Relationships.get(0).isDerived());
        assertFalse(myClass4Relationships.get(0).isNavigable());
        
        assertFalse(myClass4Relationships.get(1).isRequired());
        assertTrue(myClass4Relationships.get(1).isMultiple());
        assertTrue(myClass4Relationships.get(1).isVisible());
        assertFalse(myClass4Relationships.get(1).isDerived());
        assertTrue(myClass4Relationships.get(1).isNavigable());

        Entity myClass5 = kirra.getEntity("mypackage", "MyClass5");
        List<Relationship> myClass5Relationships = myClass5.getRelationships();
        assertEquals(myClass5Relationships.toString(), 2, myClass5Relationships.size());
        assertEquals("myClass1", myClass5Relationships.get(0).getName());
        assertEquals("myRole4", myClass5Relationships.get(1).getName());
        assertEquals("MyClass1", myClass5Relationships.get(0)
        		.getTypeRef().getTypeName());
        assertEquals("MyClass4", myClass5Relationships.get(1)
                .getTypeRef().getTypeName());

        assertTrue(myClass5Relationships.get(0).isRequired());
        assertFalse(myClass5Relationships.get(0).isMultiple());
        assertFalse(myClass5Relationships.get(0).isVisible());
        assertFalse(myClass5Relationships.get(0).isDerived());
        
        assertFalse(myClass5Relationships.get(1).isRequired());
        assertTrue(myClass5Relationships.get(1).isMultiple());
        assertTrue(myClass5Relationships.get(1).isVisible());
        assertFalse(myClass5Relationships.get(1).isDerived());
        
		index++;
		Entity myClass7 = kirra.getEntity("mypackage", "MyClass7");
        List<Relationship> myClass7Relationships = myClass7.getRelationships();
        assertEquals(myClass7Relationships.toString(), 1, myClass7Relationships.size());
        assertEquals("myRole2", myClass7Relationships.get(0).getName());
        assertEquals("MyClass2", myClass7Relationships.get(0)
                .getTypeRef().getTypeName());
        assertTrue(myClass7Relationships.get(0).isPrimary());
        assertTrue(myClass7Relationships.get(0).isRequired());
        assertTrue(myClass7Relationships.get(0).isMultiple());
        assertTrue(myClass7Relationships.get(0).isVisible());
        assertFalse(myClass7Relationships.get(0).isDerived());

        Entity myClass2 = kirra.getEntity("mypackage", "MyClass2");
        List<Relationship> myClass2Relationships = myClass2.getRelationships();
        assertEquals(myClass2Relationships.toString(), 2, myClass2Relationships.size());
        assertEquals("myRole3", myClass2Relationships.get(0).getName());
        assertEquals("myRole7", myClass2Relationships.get(1).getName());
        assertEquals("MyClass3", myClass2Relationships.get(0)
                .getTypeRef().getTypeName());
        assertEquals("MyClass7", myClass2Relationships.get(1)
                .getTypeRef().getTypeName());
        
        assertTrue(myClass2Relationships.get(0).isRequired());
        assertFalse(myClass2Relationships.get(0).isMultiple());
        assertFalse(myClass2Relationships.get(0).isVisible());
        assertFalse(myClass2Relationships.get(0).isDerived());
        assertFalse(myClass2Relationships.get(0).isNavigable());
        assertFalse(myClass2Relationships.get(0).isPrimary());
        
        assertTrue(myClass2Relationships.get(1).isRequired());
        assertFalse(myClass2Relationships.get(1).isMultiple());
        assertFalse(myClass2Relationships.get(1).isVisible());
        assertFalse(myClass2Relationships.get(1).isDerived());
        assertFalse(myClass2Relationships.get(1).isNavigable());
        assertFalse(myClass2Relationships.get(1).isPrimary());
	}
	
	public void testEntityRelationships_NonNavigableMemberEnd() throws CoreException {
		String source = "";
		source += "package mypackage;\n";
		source += "import datatypes;\n";
		source += "class Make\n";
		source += "attribute name : String;\n";
		source += "end;\n";
		source += "class Model\n";
		source += "attribute name : String;\n";
		source += "attribute make : Make;\n";
		source += "end;\n";
		source += "association\n";
		source += "role models : Model[*];\n";
		source += "role Model.make;\n";
		source += "end;\n";
		source += "end.";
		parseAndCheck(library, source);
		Repository kirra = getKirra();

		Entity make = kirra.getEntity("mypackage", "Make");
		Entity model = kirra.getEntity("mypackage", "Model");
		
		assertEquals(1, make.getProperties().size());
		assertEquals(1, make.getRelationships().size());
		
		assertEquals(1, model.getProperties().size());
		assertEquals(1, model.getRelationships().size());
		
		Relationship modelMake = model.getRelationship("make");
		assertNotNull(modelMake);
		assertTrue(modelMake.isNavigable());
		
		Relationship makeModels = make.getRelationship("models");
		assertNotNull(makeModels);
		
		assertNotNull(modelMake.getOpposite());
		
		Relationship opposite = kirra.getOpposite(modelMake);
		
		assertNotNull(opposite);
		assertEquals(makeModels.getName(), opposite.getName());
		assertTrue(!opposite.isNavigable());
	}

	public void testEntityRelationships_Reflexive() throws CoreException {
		String source = "";
		source += "package mypackage;\n";
		source += "import datatypes;\n";
		source += "class User\n";
		source += "attribute name : String;\n";
		source += "end;\n";
		source += "association Friendship\n";
		source += "navigable role peer1 : User;\n";
		source += "navigable role peer2 : User;\n";
		source += "end;\n";
		source += "end.";
		parseAndCheck(library, source);
		Repository kirra = getKirra();

		Entity user = kirra.getEntity("mypackage", "User");
		
		List<Relationship> relationships = user.getRelationships();
		assertEquals(2, relationships.size());
		
		assertEquals("peer1", relationships.get(0).getName());
		assertEquals("peer2", relationships.get(0).getOpposite());
		assertEquals("peer2", relationships.get(1).getName());
		assertEquals("peer1", relationships.get(1).getOpposite());
	}

	
	public void testEntityAggregationRelationships() throws CoreException {
		String model = "";
		model += "package mypackage;\n";
		model += "apply kirra;\n";
		model += "\n";
		model += "class MyClass3\n";
		model += "attribute singleAttribute : Integer;\n";
		model += "attribute children : MyClass4[*];\n";
		model += "end;\n";
		model += "\n";
		model += "class MyClass4\n";
		model += "attribute singleAttribute : Integer;\n";
		model += "attribute parent : MyClass3;\n";
		model += "end;\n";
		model += "composition navigable role MyClass3.children; navigable role MyClass4.parent; end;\n";
		model += "end.";
		parseAndCheck(model);
		Repository kirra = getKirra();

		Entity myClass3 = kirra.getEntity("mypackage", "MyClass3");
		List<Relationship> myClass3Relationships = myClass3.getRelationships();
		assertEquals(1, myClass3Relationships.size());
		
		assertEquals("children", myClass3Relationships.get(0).getName());
		assertEquals("MyClass4", myClass3Relationships.get(0).getTypeRef().getTypeName());
		assertFalse(myClass3Relationships.get(0).isRequired());
		assertTrue(myClass3Relationships.get(0).isMultiple());
		assertTrue(myClass3Relationships.get(0).isVisible());
		assertTrue(myClass3Relationships.get(0).getStyle() == Relationship.Style.CHILD);
		assertEquals("parent", myClass3Relationships.get(0).getOpposite());		
		
		Entity myClass4 = kirra.getEntity("mypackage", "MyClass4");
		List<Relationship> myClass4Relationships = myClass4.getRelationships();
		assertEquals(1, myClass4Relationships.size());
		assertEquals("parent", myClass4Relationships.get(0).getName());
		assertEquals("MyClass3", myClass4Relationships.get(0).getTypeRef().getTypeName());
		assertTrue(myClass4Relationships.get(0).isRequired());
		assertFalse(myClass4Relationships.get(0).isMultiple());
		assertFalse(myClass4Relationships.get(0).isVisible());
	}


	public void testEnumeration() throws CoreException {
		String model = "";
		model += "package mypackage;\n";
		model += "apply kirra;\n";
		model += "enumeration Enum1 value1, value2, value3 end;\n";
		model += "class MyClass\n";
		model += "attribute attr1 : Enum1;\n";
		model += "end;\n";
		model += "end.";
		parseAndCheck(model);
		Repository kirra = getKirra();

		Entity entity = kirra.getEntity("mypackage", "MyClass");
		Property attr1 = findNamedElement(entity.getProperties(), "attr1");
		assertEquals("Enum1", attr1.getTypeRef().getTypeName());
		assertEquals(TypeKind.Enumeration, attr1.getTypeRef().getKind());
		assertEquals(3, attr1.getEnumerationLiterals().size());
		assertEquals(Arrays.asList("value1", "value2", "value3"), attr1.getEnumerationLiterals());

		try {
			kirra.getEntity("mypackage", "Enum1");
			fail("Enumerations are not entities");
		} catch (KirraException e) {
			assertEquals(Kind.SCHEMA, e.getKind());
		}
		
		Enumeration enum1 = this.getRepository().findNamedElement("mypackage::Enum1", UMLPackage.Literals.ENUMERATION, null);
		assertNotNull(enum1);
		assertEquals(enum1.getAppliedStereotypes().toString(), 0, enum1.getAppliedStereotypes().size());
	}
	
	public void testOperations() throws CoreException {
		
		String model = "";
		model += "package mypackage;\n";
		model += "apply kirra;\n";
		model += "import datatypes;\n";
		model += "class MyClass1\n";
		model += "attribute singleAttribute : Integer;\n";
		model += "[Action]operation action1();\n";
		model += "[Action]operation action2(par1 : Integer, par2 : Boolean) : String;\n";
		model += "[Action]operation action3() : MyClass1[*];\n";
		model += "[Action]static operation action4();\n";		
		model += "private operation nonaction2(par1 : Integer, par2 : Boolean) : String;\n";
		model += "[Finder]operation query1(par1 : Integer, par2 : Boolean) : MyClass1[*];\n";
		model += "private operation nonQuery1(par1 : Integer, par2 : Boolean) : MyClass1[*];\n";
		model += "end;\n";
		model += "end.";
		parseAndCheck(model, library);
		
		Repository kirra = getKirra();

		Entity entity = kirra.getEntity("mypackage", "MyClass1");
		List<Operation> operations = entity.getOperations();
		assertEquals(5, operations.size());
		
		sortNamedElements(operations);
		
		assertEquals("action1", operations.get(0).getName());
		assertNull(operations.get(0).getTypeRef());
		assertEquals(Operation.OperationKind.Action, operations.get(0).getKind());
		assertTrue(operations.get(0).isInstanceOperation());
		assertEquals(0, operations.get(0).getParameters().size());
		
		assertEquals("action2", operations.get(1).getName());
		assertEquals("String", operations.get(1).getTypeRef().getTypeName());
		assertEquals(Operation.OperationKind.Action, operations.get(1).getKind());
		assertTrue(operations.get(1).isInstanceOperation());
		assertEquals(2, operations.get(1).getParameters().size());
		
		final List<Parameter> operationParameters = operations.get(1).getParameters();
		sortNamedElements(operationParameters);

		assertEquals("par1", operationParameters.get(0).getName());
		assertEquals("Integer", operationParameters.get(0).getTypeRef().getTypeName());
		
		assertEquals("par2", operationParameters.get(1).getName());
		assertEquals("Boolean", operationParameters.get(1).getTypeRef().getTypeName());
		
		assertEquals("action3", operations.get(2).getName());
		assertEquals("MyClass1", operations.get(2).getTypeRef().getTypeName());
		assertEquals(Operation.OperationKind.Action, operations.get(2).getKind());
		assertTrue(operations.get(2).isInstanceOperation());
		assertEquals(0, operations.get(2).getParameters().size());
		
		assertEquals("action4", operations.get(3).getName());
		assertNull("MyClass1", operations.get(3).getTypeRef());
		assertEquals(Operation.OperationKind.Action, operations.get(3).getKind());
		assertFalse(operations.get(3).isInstanceOperation());
		assertEquals(0, operations.get(3).getParameters().size());
		
		assertEquals("query1", operations.get(4).getName());
		assertEquals("MyClass1", operations.get(4).getTypeRef().getTypeName());
		assertEquals(Operation.OperationKind.Finder, operations.get(4).getKind());
		assertFalse(operations.get(4).isInstanceOperation());
		assertEquals(2, operations.get(4).getParameters().size());
	}


	public void testEntities() throws CoreException {
		String source = "";
		source += "package mypackage;\n";
		source += "import datatypes;\n";
		source += "apply kirra;\n";
		source += "class MyClass1 attribute singleAttribute : Integer; end;\n";
		source += "class MyClass2 attribute singleAttribute : Integer; end;\n";
		source += "end.";
		parseAndCheck(library, source);
		Repository kirra = getKirra();
		
		List<Entity> entities = kirra.getEntities("mypackage");
		assertEquals(2, entities.size());
		
		// ensure order by entity name for ease of testing
		sortNamedElements(entities);
		
		assertEquals("mypackage", entities.get(0).getEntityNamespace());
		assertEquals("MyClass1", entities.get(0).getName());
		
		assertEquals("mypackage", entities.get(1).getEntityNamespace());
		assertEquals("MyClass2", entities.get(1).getName());
		
		kirra.getEntity("mypackage", "MyClass1");
		kirra.getEntity("mypackage", "MyClass2");
		try {
			kirra.getEntity("mypackage", "Unknown");
		} catch (KirraException e) {
			assertEquals(KirraException.Kind.SCHEMA, e.getKind());
		}
	}
	public void testInheritance() throws CoreException {
		String source = "";
		source += "package mypackage;\n";
		source += "abstract class BaseClass\n";
		source += "attribute attr1 : Integer;\n";
		source += "operation op1();\n";
		source += "end;\n";
		source += "class ConcreteClass specializes BaseClass end;\n";
		source += "end.";
		parseAndCheck(source);
		Repository kirra = getKirra();
		List<Entity> entities = kirra.getEntities("mypackage");
		assertEquals(2, entities.size());
		Entity concreteEntity = kirra.getEntity("mypackage", "ConcreteClass");
		assertEquals("ConcreteClass", concreteEntity.getName());
		assertTrue(concreteEntity.isConcrete());
		assertEquals(1, concreteEntity.getOperations().size());
		assertEquals("op1", concreteEntity.getOperations().get(0).getName());
		assertEquals(1, concreteEntity.getProperties().size());
		assertEquals("attr1", concreteEntity.getProperties().get(0).getName());
		
		Entity abstractEntity = kirra.getEntity("mypackage", "BaseClass");
		assertFalse(abstractEntity.isConcrete());
	}
	
	public void testAutoGeneratedProperties() throws CoreException {
		String source = "";
		source += "package mypackage;\n";
		source += "import datatypes;\n";
		source += "class Issue\n";
		source += "attribute summary : String;\n";
		source += "derived id attribute issueNumber : Integer;\n";
		source += "end;\n";
		source += "end.";
		parseAndCheck(library, source);
		Repository kirra = getKirra();

		Entity issue = kirra.getEntity("mypackage", "Issue");
		
		List<Property> issueProperties = issue.getProperties();
		assertEquals(2, issueProperties.size());
		
		assertEquals("summary", issueProperties.get(0).getName());
		
		assertEquals("issueNumber", issueProperties.get(1).getName());
		assertFalse(issueProperties.get(1).isDerived());
		assertFalse(issueProperties.get(1).isInitializable());
		assertFalse(issueProperties.get(1).isEditable());
		assertTrue(issueProperties.get(1).isUnique());
		assertTrue(issueProperties.get(1).isAutoGenerated());
	}

}
