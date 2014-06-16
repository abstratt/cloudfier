package com.abstratt.nodestore.tests;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Package;

import com.abstratt.kirra.Entity;
import com.abstratt.kirra.SchemaManagement;
import com.abstratt.kirra.SchemaManagementSnapshot;
import com.abstratt.kirra.TypeRef;
import com.abstratt.kirra.TypeRef.TypeKind;
import com.abstratt.kirra.mdd.runtime.KirraMDDSchemaBuilder;
import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.tests.harness.AbstractRepositoryBuildingTests;
import com.abstratt.nodestore.jdbc.ConnectionProvider;
import com.abstratt.nodestore.jdbc.SQLGenerator;

public class SQLGeneratorTests extends AbstractRepositoryBuildingTests {
	private SQLGenerator generator;
	private SchemaManagement schema;
	public SQLGeneratorTests(String name) throws SQLException {
		super(name);
	}
	
	@Override
	protected Properties createDefaultSettings() {
		Properties defaultSettings = super.createDefaultSettings();
		defaultSettings.setProperty(IRepository.EXTEND_BASE_OBJECT, Boolean.TRUE.toString());
		// so the kirra profile is available as a system package (no need to load)
		defaultSettings.setProperty("mdd.enableKirra", Boolean.TRUE.toString());
		defaultSettings.setProperty("mdd.modelWeaver", "kirraWeaver");
		return defaultSettings;
	}
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		runInContext(new Runnable() {
			@Override
			public void run() {
				try {
					buildBasicModel();
				} catch (CoreException e) {
					throw new RuntimeException(e);
				}
			}
		});

	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	@Override
	protected void originalRunTest() throws Throwable {
		super.originalRunTest();
		// only gets here in case of success
		validateSchemaCreation();
	}
	
	private void validateSchemaCreation() throws SQLException {
		ConnectionProvider connectionProvider = new ConnectionProvider();
		Connection connection = connectionProvider.acquireConnection();
		try {
			// ensures the schema to be created for the current repo is sane 
			runSql(connection, generator.generateCreateSchema());
			for (Package package_ : getRepository().getTopLevelPackages(null))
				runSql(connection, generator.generateCreateTables(package_.getName()));
		} finally {
			connectionProvider.releaseConnection(false);
		}
	}

	private void buildBasicModel() throws CoreException {
		String model = "";
		model += "package mypackage;\n";
		model += "apply kirra;\n";
		model += "[Entity]class MyClass1\n";
		model += "attribute attr1 : Integer;\n";
		model += "attribute attr2 : String[0,1];\n";
		model += "attribute attr3 : Date[0,1];\n";
		model += "reference myClass2 : MyClass2;\n";
		model += "reference myClass3 : MyClass3[0,1];\n";
		model += "attribute myClass4s : MyClass4[*];\n";
		model += "attribute myClass5s : MyClass5[*];\n";
		model += "end;\n";
		model += "[Entity]class MyClass2\n";
		model += "attribute attr1 : Integer[0, 1];\n";
		model += "end;\n";
		model += "[Entity]class MyClass3\n";
		model += "attribute attr1 : String[0, 1];\n";
		model += "end;\n";
		model += "[Entity]class MyClass4\n";
		model += "attribute attr4 : String[0, 1];\n";
		model += "attribute myClass1 : MyClass1[0,1];\n";
		model += "end;\n";
		model += "[Entity]class MyClass5\n";
		model += "attribute attr1 : String[0, 1];\n";
		model += "end;\n";
		model += "[Entity]class MyClass6\n";
		model += "attribute attr1 : String[0, 1];\n";
		model += "end;\n";
		model += "[Entity]class MyClass7\n";
		model += "attribute attr1 : String[0, 1];\n";
		model += "end;\n";
		model += "[Entity]class MyClass8\n";
		model += "attribute attr1 : String[0, 1];\n";
		model += "end;\n";
		model += "[Entity]class MyClass9\n";
		model += "attribute attr1 : String[0, 1];\n";
		model += "end;\n";
		model += "[Entity]class MyClass10\n";
		model += "attribute attr1 : String[0, 1];\n";
		model += "reference myClass8s : MyClass8[*];\n";
		model += "end;\n";		
		model += "[Entity]class MyClass11\n";
		model += "attribute attr1 : String[0, 1];\n";
		model += "end;\n";
		model += "association ManyToManyNavigableAssociation navigable role myClass7s : MyClass7[*]; navigable role myClass6s : MyClass6[*]; end;\n";
		model += "association role MyClass1.myClass4s; role MyClass4.myClass1; end;\n";
		model += "composition role MyClass1.myClass5s; navigable role myClass1 : MyClass1; end;\n";
		model += "association navigable role myClass8s : MyClass8[*]; role myClass9 : MyClass9; end;\n";
		model += "association role myClass11 : MyClass11; navigable role myClass8s : MyClass8[*]; end;\n";
		model += "end.";
		parseAndCheck(model);
	}
	
	@Override
	protected void compilationCompleted() throws CoreException {
		this.schema = new SchemaManagementSnapshot(new KirraMDDSchemaBuilder());
		generator = new SQLGenerator(getName(), schema);
	}
	
	public void testGenerateValidation() throws CoreException {
		String model = "";
		model += "package custom;\n";
		model += "class Class1\n";
		model += "attribute attr1 : String[0, 1];\n";
		model += "attribute ref1 : Class2[0,1];\n";
		model += "attribute ref2 : Class2[1,1];\n";
		model += "attribute ref3 : Class2[0,*];\n";
		model += "attribute ref4 : Class2[1,*];\n";
		model += "end;\n";		
		model += "class Class2\n";
		model += "attribute attr2 : String[0, 1];\n";
		model += "end;\n";
		model += "association navigable role roleOneA1 : Class1; navigable role roleTwoA1 : Class2; end;\n";
		model += "association navigable role roleOneA2 : Class1; navigable role roleTwoA2 : Class2[0,1]; end;\n";
		model += "association navigable role roleOneA3 : Class1; navigable role roleTwoA3 : Class2[0,*]; end;\n";
		model += "association navigable role roleOneA4 : Class1; navigable role roleTwoA4 : Class2[1,*]; end;\n";
		model += "association role roleOneB1 : Class1; navigable role roleTwoB1 : Class2; end;\n";
		model += "association role roleOneB2 : Class1; navigable role roleTwoB2 : Class2[0,1]; end;\n";
		model += "association role roleOneB3 : Class1; navigable role roleTwoB3 : Class2[0,*]; end;\n";
		model += "association role roleOneB4 : Class1; navigable role roleTwoB4 : Class2[1,*]; end;\n";
		model += "association role roleOneC1 : Class1[*]; navigable role roleTwoC1 : Class2; end;\n";
		model += "association role roleOneC2 : Class1[*]; navigable role roleTwoC2 : Class2[0,1]; end;\n";
		model += "association role roleOneC3 : Class1[*]; navigable role roleTwoC3 : Class2[0,*]; end;\n";
		model += "association role roleOneC4 : Class1[*]; navigable role roleTwoC4 : Class2[1,*]; end;\n";
		model += "association navigable role roleOneD1 : Class1[*]; navigable role roleTwoD1 : Class2; end;\n";
		model += "association navigable role roleOneD2 : Class1[*]; navigable role roleTwoD2 : Class2[0,1]; end;\n";
		model += "association navigable role roleOneD3 : Class1[*]; navigable role roleTwoD3 : Class2[0,*]; end;\n";
		model += "association navigable role roleOneD4 : Class1[*]; navigable role roleTwoD4 : Class2[1,*]; end;\n";
		model += "end.";
		parseAndCheck(model);
		
		Entity entity1 = schema.getEntity("custom", "Class1");
		Entity entity2 = schema.getEntity("custom", "Class2");
		check(generator.generateValidate(entity1.getRelationship("ref1")));
		
		check(generator.generateValidate(entity1.getRelationship("ref2")),
				"select 1 from " + tablePrefix("custom") + "Class1 as __self__ left join " + tablePrefix("custom") + "Class2 as ref2 on ref2.id = __self__.ref2 where ref2.id is null;");
		
		check(generator.generateValidate(entity1.getRelationship("ref3")));
		
		check(generator.generateValidate(entity1.getRelationship("ref4")),
				"select 1 from " + tablePrefix("custom") + "Class1 as __self__ left join " + tablePrefix("custom") + "Class1_ref4 as Class1_ref4 on Class1_ref4.__self__ = __self__.id where Class1_ref4.ref4 is null;");

		check(generator.generateValidate(entity1.getRelationship("roleTwoA1")));
		check(generator.generateValidate(entity2.getRelationship("roleOneA1")),
				"select 1 from " + tablePrefix("custom") + "Class2 as roleTwoA1 left join " + tablePrefix("custom") + "roleOneA1_roleTwoA1 as roleOneA1_roleTwoA1 on roleOneA1_roleTwoA1.roleTwoA1 = roleTwoA1.id where roleOneA1_roleTwoA1.roleOneA1 is null;");

		check(generator.generateValidate(entity1.getRelationship("roleTwoA2")));
		check(generator.generateValidate(entity2.getRelationship("roleOneA2")), 
				"select 1 from " + tablePrefix("custom") + "Class2 as roleTwoA2 left join " + tablePrefix("custom") + "roleOneA2_roleTwoA2 as roleOneA2_roleTwoA2 on roleOneA2_roleTwoA2.roleTwoA2 = roleTwoA2.id where roleOneA2_roleTwoA2.roleOneA2 is null;");

		check(generator.generateValidate(entity1.getRelationship("roleTwoA3")));
		check(generator.generateValidate(entity2.getRelationship("roleOneA3")),
				"select 1 from " + tablePrefix("custom") + "Class2 as roleTwoA3 left join " + tablePrefix("custom") + "Class1 as roleOneA3 on roleOneA3.id = roleTwoA3.roleOneA3 where roleOneA3.id is null;");
		
		check(generator.generateValidate(entity1.getRelationship("roleTwoA4")));
		check(generator.generateValidate(entity2.getRelationship("roleOneA4")),
				"select 1 from " + tablePrefix("custom") + "Class2 as roleTwoA4 left join " + tablePrefix("custom") + "Class1 as roleOneA4 on roleOneA4.id = roleTwoA4.roleOneA4 where roleOneA4.id is null;");
		
		check(generator.generateValidate(entity1.getRelationship("roleTwoB1")), "select 1 from " + tablePrefix("custom") + "Class1 as roleOneB1 left join " + tablePrefix("custom") + "Class2 as roleTwoB1 on roleTwoB1.id = roleOneB1.roleTwoB1 where roleTwoB1.id is null;");
		check(generator.generateValidate(entity2.getRelationship("roleOneB1")));
		
		check(generator.generateValidate(entity1.getRelationship("roleTwoB2")));
		check(generator.generateValidate(entity2.getRelationship("roleOneB2")));
		
		check(generator.generateValidate(entity1.getRelationship("roleTwoB3")));
		check(generator.generateValidate(entity2.getRelationship("roleOneB3")));
		
		check(generator.generateValidate(entity1.getRelationship("roleTwoB4")), 
				"select 1 from " + tablePrefix("custom") + "Class1 as roleOneB4 left join " + tablePrefix("custom") + "Class1_roleTwoB4 as Class1_roleTwoB4 on Class1_roleTwoB4.roleOneB4 = roleOneB4.id where Class1_roleTwoB4.roleTwoB4 is null;");
		check(generator.generateValidate(entity2.getRelationship("roleOneB4")));

		check(generator.generateValidate(entity1.getRelationship("roleTwoC1")), "select 1 from " + tablePrefix("custom") + "Class1 as roleOneC1 left join " + tablePrefix("custom") + "Class2 as roleTwoC1 on roleTwoC1.id = roleOneC1.roleTwoC1 where roleTwoC1.id is null;");
		check(generator.generateValidate(entity2.getRelationship("roleOneC1")));
		
		check(generator.generateValidate(entity1.getRelationship("roleTwoC2")));
		check(generator.generateValidate(entity2.getRelationship("roleOneC2")));
		
		check(generator.generateValidate(entity1.getRelationship("roleTwoC3")));
		check(generator.generateValidate(entity2.getRelationship("roleOneC3")));
		
		check(generator.generateValidate(entity1.getRelationship("roleTwoC4")), 
				"select 1 from " + tablePrefix("custom") + "Class1 as roleOneC4 left join " + tablePrefix("custom") + "Class1_roleTwoC4 as Class1_roleTwoC4 on Class1_roleTwoC4.roleOneC4 = roleOneC4.id where Class1_roleTwoC4.roleTwoC4 is null;");
		check(generator.generateValidate(entity2.getRelationship("roleOneC4")));

		check(generator.generateValidate(entity1.getRelationship("roleTwoD1")), "select 1 from " + tablePrefix("custom") + "Class1 as roleOneD1 left join " + tablePrefix("custom") + "Class2 as roleTwoD1 on roleTwoD1.id = roleOneD1.roleTwoD1 where roleTwoD1.id is null;");
		check(generator.generateValidate(entity2.getRelationship("roleOneD1")));
		
		check(generator.generateValidate(entity1.getRelationship("roleTwoD2")));
		check(generator.generateValidate(entity2.getRelationship("roleOneD2")));
		
		check(generator.generateValidate(entity1.getRelationship("roleTwoD3")));
		check(generator.generateValidate(entity2.getRelationship("roleOneD3")));
		
		// a lower bound of 1 results in a validation query
		check(generator.generateValidate(entity1.getRelationship("roleTwoD4")), "select 1 from " + tablePrefix("custom") + "Class1 as roleOneD4 left join " + tablePrefix("custom") + "roleTwoD4_roleOneD4 as roleTwoD4_roleOneD4 on roleTwoD4_roleOneD4.roleOneD4 = roleOneD4.id where roleTwoD4_roleOneD4.roleTwoD4 is null;");
		check(generator.generateValidate(entity2.getRelationship("roleOneD4")));
	}
	
	public void testGenerateTable() throws CoreException {
		
		Class myClass = getClass("mypackage::MyClass1");
		check(generator.generateTable(schema.getEntity(ref(myClass))),
			"create table " + tablePrefix("mypackage") + "MyClass1 (" +
			    "id bigint primary key, " +
			    "attr1 bigint not null, " +
			    "attr2 varchar, " +
			    "attr3 date, " +
			    "myClass2 bigint not null, " +
			    "myClass3 bigint);");
	}
	
	
	public void testGenerateSelectRelated_mappingTable() throws CoreException {
		String model = "";
		model += "package custom;\n";
		model += "class Class1\n";
		model += "attribute attr1 : String[0, 1];\n";
		model += "end;\n";		
		model += "class Class2\n";
		model += "attribute attr2 : String[0, 1];\n";
		model += "end;\n";
		model += "association OneToMany role roleOne : Class1; navigable role roleMany : Class2[*]; end;\n";
		model += "end.";
		parseAndCheck(model);
		
		Class class1 = getClass("custom::Class1");
		check(generator.generateSelectRelatedKeys(schema.getEntity(ref(class1)).getRelationship("roleMany"), 17),
				"select roleMany.id from " + tablePrefix("custom") + "Class2 as roleMany join " + 
						tablePrefix("custom") + "OneToMany as OneToMany on OneToMany.roleMany = roleMany.id where OneToMany.roleOne = 17;");
	}

	public void testAssociation_ManyToOne_MemberOwned() throws CoreException {
		String model = "";
		model += "package custom;\n";
		model += "class Person\n";
		model += "attribute name : String[0, 1];\n";
		model += "attribute accounts : Account[*];\n";
		model += "end;\n";		
		model += "class Account\n";
		model += "attribute number : String[0, 1];\n";
		model += "attribute owner : Person[0,1];\n";
		model += "end;\n";
		model += "association role Person.accounts; role Account.owner;end;\n";
		model += "end.";
		parseAndCheck(model);

		Class account = getClass("custom::Account");
		Class person = getClass("custom::Person");

		
		check(generator.generateTable(schema.getEntity(ref(person))),
				"create table " + tablePrefix("custom") + "Person (" +
						"id bigint primary key, " +
				"name varchar);");
		check(generator.generateTable(schema.getEntity(ref(account))),
				"create table " + tablePrefix("custom") + "Account (" +
					    "id bigint primary key, " +
					    "number varchar, " +
					    "owner bigint);");
		
		check(generator.generateConstraints(schema.getEntity(ref(person))));
		
		check(generator.generateConstraints(schema.getEntity(ref(account))), "alter table " + tablePrefix("custom") + "Account " +
			    "add constraint owner " +
				"foreign key (owner) references " + tablePrefix("custom") + "Person (id) on delete set null deferrable initially deferred;");
		
		check(generator.generateSelectRelatedKeys(schema.getEntity(ref(person)).getRelationship("accounts"), 17),
				"select id from " + tablePrefix("custom") + "Account where owner = 17;");
		
		check(generator.generateSelectRelatedKeys(schema.getEntity(ref(account)).getRelationship("owner"), 17),
				"select owner.id from " + tablePrefix("custom") + "Person as owner join " + tablePrefix("custom") + "Account as accounts on owner.id = accounts.owner where accounts.id = 17;");

	}
	
	public void testAssociation_singleMemberOwned_navigableMultipleAssociationOwned() throws CoreException {
		String model = "";
		model += "package custom;\n";
		model += "class Class1\n";
		model += "attribute attr1 : String[0, 1];\n";
		model += "attribute class2s : Class2[*];\n";
		model += "end;\n";		
		model += "class Class2\n";
		model += "attribute attr2 : String[0, 1];\n";
		model += "end;\n";
		model += "association OneToMany role Class1.class2s; navigable role roleOne : Class1;end;\n";
		model += "end.";
		parseAndCheck(model);

		Class class1 = getClass("custom::Class1");
		Class class2 = getClass("custom::Class2");

		check(generator.generateTable(schema.getEntity(ref(class1))),
				"create table " + tablePrefix("custom") + "Class1 (" +
					    "id bigint primary key, " +
					    "attr1 varchar);");
		
		check(generator.generateTable(schema.getEntity(ref(class2))),
				"create table " + tablePrefix("custom") + "Class2 (" +
					    "id bigint primary key, " +
					    "attr2 varchar, " +
					    "roleOne bigint not null);");
		
		check(generator.generateConstraints(schema.getEntity(ref(class1))));
		
		check(generator.generateConstraints(schema.getEntity(ref(class2))), "alter table " + tablePrefix("custom") + "Class2 " +
			    "add constraint roleOne " +
				"foreign key (roleOne) references " + tablePrefix("custom") + "Class1 (id) on delete no action deferrable initially deferred;");
		
		check(generator.generateSelectRelatedKeys(schema.getEntity(ref(class1)).getRelationship("class2s"), 17),
				"select id from " + tablePrefix("custom") + "Class2 where roleOne = 17;");
	}
	
	public void testComposition_multipleMemberOwned_navigableSingleAssociationOwned() throws CoreException {
		String model = "";
		model += "package custom;\n";
		model += "class MyClass6\n";
		model += "attribute attr1 : String[0, 1];\n";
		model += "attribute myClass7s : MyClass7[*];\n";
		model += "end;\n";				
		model += "class MyClass7\n";
		model += "attribute attr2 : String[0, 1];\n";
		model += "end;\n";		
		model += "composition role MyClass6.myClass7s; navigable role roleOne : MyClass6[0,1]; end;\n";
		model += "end.";
		parseAndCheck(model);

		Class myClass6 = getClass("custom::MyClass6");
		Class myClass7 = getClass("custom::MyClass7");

		check(false, generator.generateTable(schema.getEntity(ref(myClass6))),
				"create table " + tablePrefix("custom", "MyClass6") + " (" +
					    "id bigint primary key, " +
					    "\"attr1\" varchar);");
		
		check(false, generator.generateTable(schema.getEntity(ref(myClass7))),
				"create table " + tablePrefix("custom", "MyClass7") + " (" +
					    "id bigint primary key, " +
					    "\"attr2\" varchar, " +
					    "\"roleOne\" bigint);");
		
		check(false, generator.generateConstraints(schema.getEntity(ref(myClass6))));
		
		check(false, generator.generateConstraints(schema.getEntity(ref(myClass7))), "alter table " + tablePrefix("custom", "MyClass7") + " " +
			    "add constraint \"roleOne\" " +
				"foreign key (\"roleOne\") references " + tablePrefix("custom", "MyClass6") + " (id) on delete set null deferrable initially deferred;");
		
		check(false, generator.generateSelectRelatedKeys(schema.getEntity(ref(myClass6)).getRelationship("myClass7s"), 17),
				"select id from " + tablePrefix("custom", "MyClass7") + " where \"roleOne\" = 17;");
		
		check(false, generator.generateSetRelated(schema.getEntity(ref(myClass6)).getRelationship("myClass7s"), 1, Arrays.asList(2L, 3L, 4L), true),
				"update " + tablePrefix("custom", "MyClass7") + " set \"roleOne\" = null where \"roleOne\" = 1;",
				"update " + tablePrefix("custom", "MyClass7") + " set \"roleOne\" = 1 where id in (2, 3, 4);");
		
		check(false, generator.generateSetRelated(schema.getEntity(ref(myClass7)).getRelationship("roleOne"), 101, Arrays.asList(104L), true),
				"update " + tablePrefix("custom", "MyClass7") + " set \"roleOne\" = null where id = 101;",				
				"update " + tablePrefix("custom", "MyClass7") + " set \"roleOne\" = 104 where id = 101;");
	}
	
	public void testAssociation_oneToOneBiDi_schema() throws CoreException {
		buildOneToOneBiDi();

		Class associated1 = getClass("custom::Associated1");
		Class associated2 = getClass("custom::Associated2");

		check(false, generator.generateTable(schema.getEntity(ref(associated1))),
				"create table " + tablePrefix("custom", "Associated1") + " (" +
					    "id bigint primary key, " +
					    "\"attr1\" varchar);");
		
		check(false, generator.generateTable(schema.getEntity(ref(associated2))),
				"create table " + tablePrefix("custom", "Associated2") + " (" +
					    "id bigint primary key, " +
					    "\"attr2\" varchar);",
			    "create table " + tablePrefix("custom", "OneToOne") + " (" +
		    		"\"end1\" bigint not null, " +
					"\"end2\" bigint not null);");
		
		check(false, generator.generateConstraints(schema.getEntity(ref(associated1))));
		check(false, generator.generateConstraints(schema.getEntity(ref(associated2))), 
				"alter table " + tablePrefix("custom", "OneToOne") + " " +
				    "add constraint \"end1\" " +
					"foreign key (\"end1\") references " + tablePrefix("custom", "Associated1") + " (id) on delete cascade;",
				"alter table " + tablePrefix("custom", "OneToOne") + " " +
				    "add constraint \"end2\" " +
					"foreign key (\"end2\") references " + tablePrefix("custom", "Associated2") + " (id) on delete cascade;");
	}
	
	public void testAssociation_oneToOneBiDi_getRelated() throws CoreException {
		buildOneToOneBiDi();

		Class associated1 = getClass("custom::Associated1");
		Class associated2 = getClass("custom::Associated2");

		check(generator.generateSelectRelatedKeys(schema.getEntity(ref(associated1)).getRelationship("end2"), 17),
				"select end2.id from " + tablePrefix("custom") + "Associated2 as end2 join " +
				tablePrefix("custom") + "OneToOne as OneToOne on OneToOne.end2 = end2.id where OneToOne.end1 = 17;");
		
		check(generator.generateSelectRelatedKeys(schema.getEntity(ref(associated2)).getRelationship("end1"), 17),
				"select end1.id from " + tablePrefix("custom") + "Associated1 as end1 join " +
				tablePrefix("custom") + "OneToOne as OneToOne on OneToOne.end1 = end1.id where OneToOne.end2 = 17;");
	}

	public void testAssociation_oneToOneBiDi_setRelated() throws CoreException {
		buildOneToOneBiDi();
		
		Class associated1 = getClass("custom::Associated1");
		Class associated2 = getClass("custom::Associated2");

		check(generator.generateSetRelated(schema.getEntity(ref(associated1)).getRelationship("end2"), 17, Arrays.asList(45L), true),
				"delete from " + tablePrefix("custom") + "OneToOne where end1 = 17;",
				"insert into " + tablePrefix("custom") + "OneToOne (end2, end1) values (45, 17);");
		
		check(generator.generateSetRelated(schema.getEntity(ref(associated2)).getRelationship("end1"), 17, Arrays.asList(45L), true),
				"delete from " + tablePrefix("custom") + "OneToOne where end2 = 17;",
				"insert into " + tablePrefix("custom") + "OneToOne (end1, end2) values (45, 17);");
	}
	
	public void testAssociation_oneToOneBiDi_CRUD() throws CoreException {
		buildOneToOneBiDi();
		
		Class associated1 = getClass("custom::Associated1");

		check(generator.generateInsert(schema.getEntity(ref(associated1)), Collections.singletonMap("attr1", (Object) "value1"), Collections.<String, Collection<Long>>emptyMap(), 123L),
				"insert into " + tablePrefix("custom") + "Associated1 (id, attr1) values (123, 'value1');");
		
		check(generator.generateUpdate(schema.getEntity(ref(associated1)), Collections.singletonMap("attr1", (Object) "value2"), Collections.<String, Collection<Long>>emptyMap(), 123L),
				"update " + tablePrefix("custom") + "Associated1 set attr1 = 'value2' where id = 123;");
		
		check(generator.generateSelectOne(schema.getEntity(ref(associated1)), 123L),
				"select id, attr1 from " + tablePrefix("custom") + "Associated1 where id = 123;");
	}


	protected void buildOneToOneBiDi() throws CoreException {
		String structure = "";
		structure += "model custom;\n";
		structure += "  class Associated1\n";
		structure += "    attribute attr1 : String[0, 1];\n";
		structure += "  end;\n";
		structure += "  class Associated2\n";
		structure += "    attribute attr2 : String[0, 1];\n";
		structure += "  end;\n";
		structure += "  association OneToOne\n";
		structure += "    navigable role end1 : Associated1[0,1]; \n";
		structure += "    navigable role end2 : Associated2[0,1]; \n";
		structure += " end;\n";
		structure += "end.";		
		parseAndCheck(structure);
	}
	
	protected void buildOneToOneReference() throws CoreException {
		String structure = "";
		structure += "model custom;\n";
		structure += "  class State\n";
		structure += "    attribute stateName : String[0, 1];\n";
		structure += "  end;\n";
		structure += "  class City\n";
		structure += "    attribute cityName : String[0, 1];\n";
		structure += "    reference cityState : State;\n";
		structure += "  end;\n";
		structure += "end.";		
		parseAndCheck(structure);
	}

	public void testAssociation_oneToOneReference_getRelated() throws CoreException {
		buildOneToOneReference();
		Class city = getClass("custom::City");
		check(generator.generateSelectRelatedKeys(schema.getEntity(ref(city)).getRelationship("cityState"), 17),
				"select cityState.id from " + tablePrefix("custom") + "State as cityState join " +
				tablePrefix("custom") + "City as __self__ on cityState.id = __self__.cityState where __self__.id = 17;");
	}
	
	protected void buildOneToOneUniDi() throws CoreException {
		String structure = "";
		structure += "model custom;\n";
		structure += "  class Car\n";
		structure += "    attribute vehicleNumber : String[0, 1];\n";
		structure += "    attribute carEngine : Engine[0, 1];\n";		
		structure += "  end;\n";
		structure += "  class Engine\n";
		structure += "    attribute engineNumber : String[0, 1];\n";
		structure += "  end;\n";
		structure += "  association role Car.carEngine; role engineCar : Car; end;\n";
		structure += "end.";		
		parseAndCheck(structure);
	}

	public void testAssociation_oneToOneUniDi() throws CoreException {
		buildOneToOneUniDi();
		Class car = getClass("custom::Car");
		Class engine = getClass("custom::Engine");
		check(generator.generateTable(schema.getEntity(ref(car))),
				"create table " + tablePrefix("custom") + "Car (" +
					    "id bigint primary key, " +
					    "vehicleNumber varchar, " +
					    "carEngine bigint);");

		check(generator.generateSelectRelatedKeys(schema.getEntity(ref(car)).getRelationship("carEngine"), 17),
				"select carEngine.id from " + tablePrefix("custom") + "Engine as carEngine join " +
				tablePrefix("custom") + "Car as engineCar on carEngine.id = engineCar.carEngine where engineCar.id = 17;");
		
		check(generator.generateTable(schema.getEntity(ref(engine))),
				"create table " + tablePrefix("custom") + "Engine (" +
					    "id bigint primary key, " +
					    "engineNumber varchar);");

		check(generator.generateSelectRelatedKeys(schema.getEntity(ref(engine)).getRelationship("engineCar"), 17), 
				"select engineCar.id from " + tablePrefix("custom") + "Car as engineCar join " +
						tablePrefix("custom") + "Engine as carEngine on carEngine.id = engineCar.carEngine where carEngine.id = 17;");
	}


	protected void buildOneToManyUniDi() throws CoreException {
		String structure = "";
		structure += "model custom;\n";
		structure += "  class State\n";
		structure += "    attribute stateName : String[0, 1];\n";
		structure += "    reference stateCities : City[0,*];\n";
		structure += "  end;\n";
		structure += "  class City\n";
		structure += "    attribute cityName : String[0, 1];\n";
		structure += "  end;\n";
		structure += "end.";		
		parseAndCheck(structure);
	}

	public void testAssociation_oneToManyUniDi_getRelated() throws CoreException {
		buildOneToManyUniDi();
		Class state = getClass("custom::State");
		check(generator.generateSelectRelatedKeys(schema.getEntity(ref(state)).getRelationship("stateCities"), 17),
				"select stateCities.id from " + tablePrefix("custom") + "City as stateCities" + " join " +
				tablePrefix("custom") + "State_stateCities as State_stateCities" + " on State_stateCities.stateCities = stateCities.id where State_stateCities.__self__ = 17;");
	}
	
	public void testGenerateTableManyToManyBiDiAssociation_basicSchema() throws CoreException {
		
		Class myClass = getClass("mypackage::MyClass6");
		
		check(generator.generateTable(schema.getEntity(ref(myClass))), 
			"create table " + tablePrefix("mypackage") + "MyClass6 (" +
				"id bigint primary key, " +
				"attr1 varchar);",
		    "create table " + tablePrefix("mypackage") + "ManyToManyNavigableAssociation (" +
	    		"myClass7s bigint not null, " +
				"myClass6s bigint not null);");
	}

	public void testGenerateTableManyToOneUnidirectionalAssociation_basicSchema() throws CoreException {
		Class myClass8 = getClass("mypackage::MyClass8");
		check(generator.generateTable(schema.getEntity(ref(myClass8))),
				"create table " + tablePrefix("mypackage") + "MyClass8 (" +
						"id bigint primary key, " +
				"attr1 varchar);");
		
		Class myClass9 = getClass("mypackage::MyClass9");
		check(generator.generateTable(schema.getEntity(ref(myClass9))),
				"create table " + tablePrefix("mypackage") + "MyClass9 (" +
				    "id bigint primary key, " +
				    "attr1 varchar);",
                "create table " + tablePrefix("mypackage") + "MyClass9_myClass8s (" +
				    "myClass8s bigint not null, " +
				    "myClass9 bigint not null);");
		
		Class myClass11 = getClass("mypackage::MyClass11");
		check(generator.generateTable(schema.getEntity(ref(myClass11))),
				"create table " + tablePrefix("mypackage") + "MyClass11 (" +
						"id bigint primary key, " +
						"attr1 varchar);",
						"create table " + tablePrefix("mypackage") + "MyClass11_myClass8s (" +
								"myClass8s bigint not null, " +
				"myClass11 bigint not null);");
		
		Class myClass10 = getClass("mypackage::MyClass10");
		check(generator.generateTable(schema.getEntity(ref(myClass10))),
				"create table " + tablePrefix("mypackage") + "MyClass10 (" +
				    "id bigint primary key, " +
				    "attr1 varchar);",
                "create table " + tablePrefix("mypackage") + "MyClass10_myClass8s (" +
				    "myClass8s bigint not null, " +
				    "__self__ bigint not null);");
		
	}
	
	public void testGenerateTableManyToOneUnidirectionalAssociation_constraints() throws CoreException {
		Class myClass8 = getClass("mypackage::MyClass8");
		check(generator.generateConstraints(schema.getEntity(ref(myClass8))));
		
		Class myClass9 = getClass("mypackage::MyClass9");
		check(generator.generateConstraints(schema.getEntity(ref(myClass9))),
				"alter table " + tablePrefix("mypackage") + "MyClass9_myClass8s " +
					    "add constraint myClass8s " +
						"foreign key (myClass8s) references " + tablePrefix("mypackage") + "MyClass8 (id) on delete cascade;",
				"alter table " + tablePrefix("mypackage") + "MyClass9_myClass8s " +
					    "add constraint myClass9 " +
						"foreign key (myClass9) references " + tablePrefix("mypackage") + "MyClass9 (id) on delete cascade;");
		
		Class myClass10 = getClass("mypackage::MyClass10");
		check(generator.generateConstraints(schema.getEntity(ref(myClass10))),
				"alter table " + tablePrefix("mypackage") + "MyClass10_myClass8s " +
					    "add constraint myClass8s " +
						"foreign key (myClass8s) references " + tablePrefix("mypackage") + "MyClass8 (id) on delete cascade;",
				"alter table " + tablePrefix("mypackage") + "MyClass10_myClass8s " +
					    "add constraint __self__ " +
						"foreign key (__self__) references " + tablePrefix("mypackage") + "MyClass10 (id) on delete cascade;");
		
		Class myClass11 = getClass("mypackage::MyClass11");
		check(generator.generateConstraints(schema.getEntity(ref(myClass11))),
				"alter table " + tablePrefix("mypackage") + "MyClass11_myClass8s " +
					    "add constraint myClass8s " +
						"foreign key (myClass8s) references " + tablePrefix("mypackage") + "MyClass8 (id) on delete cascade;",
				"alter table " + tablePrefix("mypackage") + "MyClass11_myClass8s " +
					    "add constraint myClass11 " +
						"foreign key (myClass11) references " + tablePrefix("mypackage") + "MyClass11 (id) on delete cascade;");
	}

	public void check(boolean ignoreQuotes, List<String> actual, String... expected) {
		assertEquals(actual.toString() + "!=" + Arrays.asList(expected),expected.length, actual.size());
		for (int i = 0; i < expected.length; i++)
			compareStatements(ignoreQuotes, "" + i, expected[i], actual.get(i));
	}

	private void runSql(Connection connection, List<String> actual) {
		try {
			Statement statement = connection.createStatement();
			for (String sql : actual)
				try {
    				statement.execute(sql);
				} catch (SQLException sqle) {
					fail(sqle.getMessage() + " - SQL: " + sql);
				}
			statement.close();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void check(List<String> actual, String... expected) {
		check(true, actual, expected);
	}


	public void testGenerateTableManyToManyBiDiAssociation_constraints() throws CoreException {
		Class myClass = getClass("mypackage::MyClass6");
		List<String> stmts = generator.generateConstraints(schema.getEntity(ref(myClass)));
		assertEquals(2, stmts.size());
		String stmt1 = "alter table " + tablePrefix("mypackage") + "ManyToManyNavigableAssociation " +
				"add constraint myClass7s " +
				"foreign key (myClass7s) references " + tablePrefix("mypackage") + "MyClass7 (id) on delete cascade;";
		compareStatements(stmt1, stmts.get(0));
		String stmt2 = "alter table " + tablePrefix("mypackage") + "ManyToManyNavigableAssociation " +
		    "add constraint myClass6s " +
			"foreign key (myClass6s) references " + tablePrefix("mypackage") + "MyClass6 (id) on delete cascade;";
		compareStatements(stmt2, stmts.get(1));
	}
	
	public void testGenerateTableWithAssociationProperties() throws CoreException {
		
		Class myClass = getClass("mypackage::MyClass5");
		
		List<String> stmts1 = generator.generateTable(schema.getEntity(ref(myClass)));
		assertEquals(1, stmts1.size());
		String expected1 = "create table " + tablePrefix("mypackage") + "MyClass5 (" +
		    "id bigint primary key, " +
		    "attr1 varchar, " +
		    "myClass1 bigint not null);";
		compareStatements(expected1, stmts1.get(0));
	}
	
	private TypeRef ref(Class umlType) {
        if (umlType == null)
            return null;
        return new TypeRef(umlType.getPackage().getName(), umlType.getName(), TypeKind.Entity);
	}

	public void testGenerateInsert() throws CoreException, ParseException {
		
		Class myClass = getClass("mypackage::MyClass1");
		
		Map<String, Object> values = new HashMap<String, Object>();
		
		values.put("attr1", 45);
		values.put("attr2", "test");
		values.put("attr3", new SimpleDateFormat("yyyy/MM/dd").parse("2012/11/13"));
		
		List<String> stmts1 = generator.generateInsert(schema.getEntity(ref(myClass)), values, Collections.<String, Collection<Long>>emptyMap(), null);
		assertEquals(1, stmts1.size());
		String expected1 = "insert into " + tablePrefix("mypackage") + "MyClass1 (id, attr1, attr2, attr3, myClass2, myClass3) values (nextval('" + getName() + ".sequence'), 45, 'test', '2012-11-13', -1, null);";
		compareStatements(expected1, stmts1.get(0));
	}
	
    public void testGenerateInsertWithAutoGeneratedColumns() throws CoreException, ParseException {
		String model = "";
		model += "package custom;\n";
		model += "class Issue\n";
		model += "derived id attribute attr1 : Integer;\n";
		model += "derived id attribute attr2 : String;\n";
		model += "end;\n";		
		model += "end.";
		parseAndCheck(model);
	
		Class myClass = getClass("custom::Issue");
		
		List<String> stmts1 = generator.generateInsert(schema.getEntity(ref(myClass)), Collections.<String, Object>emptyMap(), Collections.<String, Collection<Long>>emptyMap(), null);
		assertEquals(1, stmts1.size());
		String expected1 = "insert into " + tablePrefix("custom") + "Issue (id, attr1, attr2) values (nextval('" + getName() + ".sequence'), nextval('" + getName() + ".sequence'), nextval('" + getName() + ".sequence'));";
		compareStatements(expected1, stmts1.get(0));
	}
	
	public void testGenerateUpdate() throws CoreException, ParseException {
		
		Class myClass = getClass("mypackage::MyClass1");
		
		Map<String, Object> values = new HashMap<String, Object>();
		
		values.put("attr1", 45);
		values.put("attr2", "test");
		values.put("attr3", new SimpleDateFormat("yyyy/MM/dd").parse("2012/11/13"));
		
		List<String> stmts1 = generator.generateUpdate(schema.getEntity(ref(myClass)), values, Collections.<String, Collection<Long>>emptyMap(), 54L);
		assertEquals(1, stmts1.size());
		String expected1 = "update " + tablePrefix("mypackage") + "MyClass1 set attr1 = 45, attr2 = 'test', attr3 = '2012-11-13', myClass2 = null, myClass3 = null where id = 54;";
		compareStatements(expected1, stmts1.get(0));
	}
	

	public void testGenerateSelectAll() throws CoreException {
		Class myClass = getClass("mypackage::MyClass1");
		
		List<String> stmts1 = generator.generateSelectAll(schema.getEntity(ref(myClass)));
		assertEquals(1, stmts1.size());
		String expected1 = "select id, attr1, attr2, attr3, myClass2, myClass3 from " + tablePrefix("mypackage") + "MyClass1;";
		compareStatements(expected1, stmts1.get(0));
	}
	
	public void testGenerateSelectOne() throws CoreException {
		Class myClass = getClass("mypackage::MyClass1");
		
		List<String> stmts1 = generator.generateSelectOne(schema.getEntity(ref(myClass)), 43);
		assertEquals(1, stmts1.size());
		String expected1 = "select id, attr1, attr2, attr3, myClass2, myClass3 from " + tablePrefix("mypackage") + "MyClass1 where id = 43;";
		compareStatements(expected1, stmts1.get(0));
	}

	public void testGenerateSelectRelated() throws CoreException {
		String model = "";
		model += "package custom;\n";
		model += "class State\n";
		model += "attribute stateName : String[0, 1];\n";
		model += "end;\n";		
		model += "class City\n";
		model += "attribute cityName : String[0, 1];\n";
		model += "end;\n";
		model += "association OneToMany navigable role cities : City[*]; navigable role cityState : State; end;\n";
		model += "end.";
		parseAndCheck(model);
		
		Class myState = getClass("custom::State");
		Class myCity = getClass("custom::City");
		// 1 -> many
		check(generator.generateSelectRelatedKeys(schema.getEntity(ref(myState)).getRelationship("cities"), 17), 
				"select id from " + tablePrefix("custom") + "City where \"cityState\" = 17;");
		// many -> 1
		check(generator.generateSelectRelatedKeys(schema.getEntity(ref(myCity)).getRelationship("cityState"), 17), 
				"select \"cityState\".id from " + tablePrefix("custom") + "State as \"cityState\" join " + tablePrefix("custom") + "City as \"cities\" on \"cityState\".id = \"cities\".\"cityState\" where \"cities\".id = 17;");
	}
	
	public void testGenerateSetRelated() throws CoreException {
		Class myClass = getClass("mypackage::MyClass1");
		List<String> stmts = generator.generateSetRelated(schema.getEntity(ref(myClass)).getRelationship("myClass4s"), 1, Arrays.<Long>asList(2L, 3L, 4L, 5L), true);
		check(stmts,
				"update " + tablePrefix("mypackage") + "MyClass4 set myClass1 = null where myClass1 = 1;",
				"update " + tablePrefix("mypackage") + "MyClass4 set myClass1 = 1 where id in (2, 3, 4, 5);");
	}
	
	public void testGenerateRemoveRelated() throws CoreException {
		Class myClass = getClass("mypackage::MyClass6");
		List<String> stmts = generator.generateRemoveRelated(schema.getEntity(ref(myClass)).getRelationship("myClass7s"), 1L, 2L);
		check(stmts,
				"delete from " + tablePrefix("mypackage") + "ManyToManyNavigableAssociation where myClass6s = 1 and myClass7s = 2;");
	}
	
	public void testGenerateRemoveRelated_OneToOneViaAssociationEnds() throws CoreException {
		buildOneToOneBiDi();
		
		Class associated1 = getClass("custom::Associated1");

		List<String> stmts = generator.generateRemoveRelated(schema.getEntity(ref(associated1)).getRelationship("end2"), 1L, 2L);
		check(stmts,
				"delete from " + tablePrefix("custom") + "OneToOne where end1 = 1 and end2 = 2;");
	}
	
	static public void compareStatements(boolean ignoreQuotes, String label, String expected, String actual) {
		if (ignoreQuotes) {
			expected = expected.replace("\"", "");
			actual = actual.replace("\"", "");
		}
		Assert.assertEquals(label, expected, actual);
	}

	static public void compareStatements(String expected, String actual) {
		compareStatements(true, null, expected, actual);
	}
	
	public void testGenerateDelete() throws CoreException {
		
		Class myClass = getClass("mypackage::MyClass1");
		
		List<String> stmts1 = generator.generateDelete(schema.getEntity(ref(myClass)), 45);
		assertEquals(1, stmts1.size());
		String expected1 = "delete from " + tablePrefix("mypackage") + "MyClass1 where id = 45;";
		compareStatements(expected1, stmts1.get(0));
	}
	
	public void testGenerateConstraints() throws CoreException {
		
		Class myClass1 = getClass("mypackage::MyClass1");
		Class myClass5 = getClass("mypackage::MyClass5");
		
		List<String> stmts = generator.generateConstraints(schema.getEntity(ref(myClass1)));
		assertTrue(stmts.size() >= 2);
		String expected2 = "alter table " + tablePrefix("mypackage") + "MyClass1 " +
		    "add constraint myClass2 " +
			"foreign key (myClass2) references " + tablePrefix("mypackage") + "MyClass2 (id) on delete no action deferrable initially deferred;";
		compareStatements(expected2, stmts.get(0));
		String expected3 = "alter table " + tablePrefix("mypackage") + "MyClass1 " +
			    "add constraint myClass3 " +
				"foreign key (myClass3) references " + tablePrefix("mypackage") + "MyClass3 (id) on delete set null deferrable initially deferred;";
		compareStatements(expected3, stmts.get(1));
		
		List<String> stmts2 = generator.generateConstraints(schema.getEntity(ref(myClass5)));
		assertEquals(1, stmts2.size());
		String expected5 = "alter table " + tablePrefix("mypackage") + "MyClass5 " +
			    "add constraint myClass1 " +
				"foreign key (myClass1) references " + tablePrefix("mypackage") + "MyClass1 (id) on delete cascade deferrable initially deferred;";
		compareStatements(expected5, stmts2.get(0));

	}
	
	public void testGenerateUniqueConstraints() throws CoreException {
		
		String model = "";
		model += "package custom;\n";
		model += "class City\n";
		model += "id attribute cityName : String;\n";
		model += "id attribute airportCode : String;\n";
		model += "end;\n";
		model += "end.";
		parseAndCheck(model);

		
		Class city = getClass("custom::City");
		
		List<String> stmts = generator.generateConstraints(schema.getEntity(ref(city)));
		assertTrue(stmts.size() >= 2);
		String expected2 = "alter table " + tablePrefix("custom") + "City " +
		    "add constraint custom_City_cityName_uk " +
			"unique (cityName);";
		compareStatements(expected2, stmts.get(0));
		String expected3 = "alter table " + tablePrefix("custom") + "City " +
		    "add constraint custom_City_airportCode_uk " +
			"unique (airportCode);";
		compareStatements(expected3, stmts.get(1));
	}

	
	private String tablePrefix(String packageName) {
		return "\"" + getName() + "\"." + packageName + "_";
	}

	private String tablePrefix(String packageName, String tableName) {
		return "\"" + getName() + "\".\"" + packageName + "_" + tableName + "\"";
	}
	
	public void testGenerateSchema() throws CoreException {
		List<String> statements = generator.generateCreateSchema();
		assertEquals(statements.size(), 2);
		compareStatements("create schema \"" + getName() + "\";", statements.get(0));
		compareStatements("create sequence \"" + getName() + "\".sequence;", statements.get(1));
	}

	public static Test suite() {
		return new TestSuite(SQLGeneratorTests.class);
	}

}
