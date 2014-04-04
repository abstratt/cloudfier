package com.abstratt.kirra.tests.mdd.runtime;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import com.abstratt.kirra.Instance;
import com.abstratt.kirra.KirraException;
import com.abstratt.kirra.Repository;
import com.abstratt.kirra.populator.DataPopulator;
import com.abstratt.mdd.core.tests.harness.FixtureHelper;
import com.abstratt.mdd.frontend.core.IProblem;

public class KirraDataPopulatorTests extends AbstractKirraMDDRuntimeTests {

    private static String accountModel;
    static {
        accountModel = "";
        accountModel += "package banking;\n";
        accountModel += "apply kirra;\n";
        accountModel += "import base;\n";
        accountModel += "enumeration Status Open, Closed end;\n";
        accountModel += "class Account\n";
        accountModel += "  attribute number : String[0,1];\n";
        accountModel += "  attribute balance : Double[0,1];\n";
        accountModel += "  attribute status : Status[0,1];\n";
        accountModel += "  attribute owner : Person[0,1];\n";        
        accountModel += "end;\n";
        accountModel += "association AccountOwner\n";
        accountModel += "  role Account.owner;\n";
        accountModel += "  role Person.accounts;\n";        
        accountModel += "end;\n";
        accountModel += "class Person\n";
        accountModel += "  attribute employer: Company[*];\n";        
        accountModel += "  attribute name : String;\n";
        accountModel += "  attribute accounts : Account[*];\n";        
        accountModel += "end;\n";
        accountModel += "class Company\n";
        accountModel += "  attribute employee: Person[*];\n";        
        accountModel += "  attribute name : String;\n";
        accountModel += "end;\n";    
        accountModel += "association\n";
        accountModel += "  role Company.employee;\n";
        accountModel += "  role Person.employer;\n";        
        accountModel += "end;\n";            
        accountModel += "end.";
    }

    public KirraDataPopulatorTests(String name) {
        super(name);
    }

    public void testSimple() throws CoreException {
        parseAndCheck(accountModel);
        Repository kirra = getKirra();

        String contents = "";
        contents += "{\n";
        contents += "  banking: {\n";
        contents += "    Account: [\n";
        contents += "      { number: '123', balance: 340.5, status: 'Closed'}\n";
        contents += "    ]\n";
        contents += "  }\n";
        contents += "}\n";
        
        FixtureHelper.assertCompilationSuccessful(parseData(contents));
        
        DataPopulator populator = new DataPopulator(kirra);
        boolean status = populator.populate(new ByteArrayInputStream(contents.getBytes()));
        assertTrue(status);

        List<Instance> instances = kirra.getInstances("banking", "Account", false);
        assertEquals(1, instances.size());
        assertEquals("123", instances.get(0).getValue("number"));
        assertEquals(340.5, instances.get(0).getValue("balance"));
        assertEquals("Closed", instances.get(0).getValue("status"));
    }
    
    public void testGraph() throws CoreException {
        parseAndCheck(accountModel);

        String contents = "";
        contents += "{\n";
        contents += "  banking: {\n";
        contents += "    Person: [\n";
        contents += "      { name: 'John'},\n";
        contents += "      { name: 'Mary'}\n";        
        contents += "    ],\n";
        contents += "    Account: [\n";
        contents += "      { number: '123'},\n";
        contents += "      { number: '456', owner: 'Person@1'},\n";
        contents += "      { number: 'ABC', owner: 'Person@2'},\n";
        contents += "      { number: 'DEF', owner: 'Person@2'}\n";
        contents += "    ]\n";
        contents += "  }\n";
        contents += "}\n";
        
        FixtureHelper.assertCompilationSuccessful(parseData(contents));
        
        Repository kirra = getKirra();
        
        DataPopulator populator = new DataPopulator(kirra);
        boolean status = populator.populate(new ByteArrayInputStream(contents.getBytes()));
        assertTrue(status);

        Map<String, Instance> accounts = toMap(kirra.getInstances("banking", "Account", false), "number");
        assertEquals(accounts.keySet(), new HashSet<String>(Arrays.asList("123", "456", "ABC", "DEF")));
        
        Map<String, Instance> persons = toMap(kirra.getInstances("banking", "Person", false), "name");
        assertEquals(persons.keySet(), new HashSet<String>(Arrays.asList("John", "Mary")));
        
        assertNull(accounts.get("123").getSingleRelated("owner"));
        assertEquals("John", accounts.get("456").getSingleRelated("owner").getValue("name"));
        assertEquals("Mary", accounts.get("ABC").getSingleRelated("owner").getValue("name"));
        assertEquals("Mary", accounts.get("DEF").getSingleRelated("owner").getValue("name"));
    }

    
	private IProblem[] parseData(String contents) throws CoreException {
		return new FixtureHelper(null).parseFiles(getBaseDir(), getRepository(), Collections.singletonMap("data.json", contents));
	}

    // N:N associations are not supported at this time
    public void _testGraphViaAssociation() throws CoreException {
        parseAndCheck(accountModel);
        Repository kirra = getKirra();

        String contents = "";
        contents += "{\n";
        contents += "  banking: {\n";
        contents += "    Person: [\n";
        contents += "      { name: 'John'},\n";
        contents += "      { name: 'Mary'},\n";
        contents += "      { name: 'Bill'}\n";                
        contents += "    ],\n";
        contents += "    Company: [\n";
        contents += "      { name: 'Microsoft', employee: ['Person@2', 'Person@1', 'Person@3']},\n";
        contents += "      { name: 'IBM', employee: ['Person@2']}\n";        
        contents += "    ]\n";
        contents += "  }\n";
        contents += "}\n";
        
        FixtureHelper.assertCompilationSuccessful(parseData(contents));
        
        DataPopulator populator = new DataPopulator(kirra);
        boolean status = populator.populate(new ByteArrayInputStream(contents.getBytes()));
        assertTrue(status);

        Map<String, Instance> persons = toMap(kirra.getInstances("banking", "Person", false), "name");
        assertEquals(persons.keySet(), new HashSet<String>(Arrays.asList("John", "Mary", "Bill")));
        
        Map<String, Instance> companies = toMap(kirra.getInstances("banking", "Company", false), "name");
        assertEquals(companies.keySet(), new HashSet<String>(Arrays.asList("IBM", "Microsoft")));

        Map<String, Instance> msEmployees = toMap(companies.get("Microsoft").getRelated("employee"), "name");
        assertEquals(msEmployees.keySet(), new HashSet<String>(Arrays.asList("Mary", "John", "Bill")));
        
        Map<String, Instance> ibmEmployees = toMap(companies.get("IBM").getRelated("employee"), "name");
        assertEquals(ibmEmployees.keySet(), new HashSet<String>(Arrays.asList("Mary")));
        
        Map<String, Instance> marysEmployers = toMap(persons.get("Mary").getRelated("employer"), "name");
        assertEquals(marysEmployers.keySet(), new HashSet<String>(Arrays.asList("Microsoft", "IBM")));
    }
    
    
    private <T> Map<T, Instance> toMap(List<Instance> instances, String indexProperty) {
        Map<T, Instance> map = new HashMap<T, Instance>();
        for (Instance instance : instances)
            assertNull(map.put((T) instance.getValue(indexProperty), instance));
        return map;
    }

    
    public void testDataValidationValidData() throws CoreException {
        parseAndCheck(accountModel);
        
        String contents = "";
        contents += "{\n";
        contents += "  banking: {\n";
        contents += "    Account: [\n";
        contents += "      { number: '123', balance: 340.5}\n";
        contents += "    ]\n";
        contents += "  }\n";
        contents += "}\n";

        FixtureHelper.assertCompilationSuccessful(parseData(contents));
    }
    
    public void testDataValidationWrongDataType() throws CoreException {
        parseAndCheck(accountModel);
        
        String contents = "";
        contents += "{\n";
        contents += "  banking: {\n";
        contents += "    Account: [\n";
        contents += "      { balance: 'foobar'}\n";
        contents += "    ]\n";
        contents += "  }\n";
        contents += "}\n";
        
        IProblem[] result = parseData(contents);
        FixtureHelper.assertTrue(result, result.length == 1);
    }

    public void testEmpty() throws CoreException {
        parseAndCheck(accountModel);
        Repository kirra = getKirra();

        String contents = "";
        contents += "{\n";
        contents += "  banking: {\n";
        contents += "    Account: [\n";
        contents += "      { foo: 100, balance: 20},\n";
        contents += "      { balance: 'bar'},\n";
        contents += "      { }\n";
        contents += "    ]\n";
        contents += "  }\n";
        contents += "}\n";
        
        IProblem[] result = parseData(contents);
        // invalid property, wrong type for balance
        FixtureHelper.assertTrue(result, result.length == 2);
        
        DataPopulator populator = new DataPopulator(kirra);
        try {
            populator.populate(new ByteArrayInputStream(contents.getBytes()));
            fail("population should have failed, there's an error in the data");
        } catch (KirraException e) {
        	// expected
        }
        List<Instance> instances = kirra.getInstances("banking", "Account", false);
        assertEquals(1, instances.size());
        assertEquals(Collections.singletonMap("balance", 20d), instances.get(0).getValues());

    }
    
    public void testNonExistingClass() throws CoreException {
        parseAndCheck(accountModel);
        Repository kirra = getKirra();

        String contents = "";
        contents += "{\n";
        contents += "  banking: { Account: [{number: '123'}], NonExistingClass: [{}] },\n";
        contents += "  nonExistingPackage: { }\n";
        contents += "}\n";
        
        IProblem[] result = parseData(contents);
        // invalid package, invalid class
        FixtureHelper.assertTrue(result, result.length == 1);
        
        DataPopulator populator = new DataPopulator(kirra);
        try {
        	populator.populate(new ByteArrayInputStream(contents.getBytes()));
        	fail("should have failed");
        } catch (KirraException e) {
        	// expected
        }
        // all or nothing
        List<Instance> instances = kirra.getInstances("banking", "Account", false);
        assertEquals(Collections.singletonMap("number", "123"), instances.get(0).getValues());
    }
    
    public void testInvalidRelationshipType() throws CoreException {
        parseAndCheck(accountModel);

        String contents = "";
        contents += "{\n";
        contents += "  banking: { Account: [{}, {owner: 'Account@1'}] }\n";
        contents += "}\n";
        
        IProblem[] result = parseData(contents);
        // invalid reference type
        FixtureHelper.assertTrue(result, result.length == 1);
        assertTrue(result[0].getMessage(), result[0].getMessage().contains("is not a kind of")); 
    }
}
