package com.abstratt.kirra.tests.mdd.runtime;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;

import com.abstratt.kirra.Instance;
import com.abstratt.kirra.KirraException;
import com.abstratt.kirra.Repository;
import com.abstratt.kirra.populator.DataPopulator;
import com.abstratt.mdd.core.IProblem;
import com.abstratt.mdd.core.tests.harness.FixtureHelper;

import junit.framework.TestCase;
import org.apache.commons.lang3.tuple.Pair;

public class KirraDataPopulatorTests extends AbstractKirraMDDRuntimeTests {

    private static String accountModel;
    static {
        KirraDataPopulatorTests.accountModel = "";
        KirraDataPopulatorTests.accountModel += "package banking;\n";
        KirraDataPopulatorTests.accountModel += "apply kirra;\n";
        KirraDataPopulatorTests.accountModel += "import base;\n";
        KirraDataPopulatorTests.accountModel += "enumeration Status Open; Closed; end;\n";
        KirraDataPopulatorTests.accountModel += "class Account\n";
        KirraDataPopulatorTests.accountModel += "  attribute number : String[0,1];\n";
        KirraDataPopulatorTests.accountModel += "  attribute balance : Double[0,1];\n";
        KirraDataPopulatorTests.accountModel += "  attribute status : Status[0,1];\n";
        KirraDataPopulatorTests.accountModel += "  readonly attribute owner : Person[0,1];\n";
        KirraDataPopulatorTests.accountModel += "end;\n";
        KirraDataPopulatorTests.accountModel += "class AccountService\n";
        KirraDataPopulatorTests.accountModel += "  attribute name : String;\n";
        KirraDataPopulatorTests.accountModel += "end;\n";
        KirraDataPopulatorTests.accountModel += "association AccountServices\n";
        KirraDataPopulatorTests.accountModel += "  navigable role accounts : Account[*];\n";
        KirraDataPopulatorTests.accountModel += "  navigable role services : AccountService[*];\n";
        KirraDataPopulatorTests.accountModel += "end;\n";
        KirraDataPopulatorTests.accountModel += "association AccountOwner\n";
        KirraDataPopulatorTests.accountModel += "  role Account.owner;\n";
        KirraDataPopulatorTests.accountModel += "  role Person.accounts;\n";
        KirraDataPopulatorTests.accountModel += "end;\n";
        KirraDataPopulatorTests.accountModel += "role class Person\n";
        KirraDataPopulatorTests.accountModel += "  attribute employer: Company[0,1];\n";
        KirraDataPopulatorTests.accountModel += "  attribute name : String;\n";
        KirraDataPopulatorTests.accountModel += "  readonly attribute accounts : Account[*];\n";
        KirraDataPopulatorTests.accountModel += "end;\n";
        KirraDataPopulatorTests.accountModel += "class Company\n";
        KirraDataPopulatorTests.accountModel += "  attribute name : String;\n";
        KirraDataPopulatorTests.accountModel += "  attribute employee: Person[*];\n";
        KirraDataPopulatorTests.accountModel += "  attribute manager : Person[0,1];\n";
        KirraDataPopulatorTests.accountModel += "end;\n";
        KirraDataPopulatorTests.accountModel += "association\n";
        KirraDataPopulatorTests.accountModel += "  role Company.employee;\n";
        KirraDataPopulatorTests.accountModel += "  role Person.employer;\n";
        KirraDataPopulatorTests.accountModel += "end;\n";
        KirraDataPopulatorTests.accountModel += "end.";
    }

    public KirraDataPopulatorTests(String name) {
        super(name);
    }

    public void testDataValidationValidData() throws CoreException {
        parseAndCheck(KirraDataPopulatorTests.accountModel);

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
        parseAndCheck(KirraDataPopulatorTests.accountModel);

        String contents = "";
        contents += "{\n";
        contents += "  banking: {\n";
        contents += "    Account: [\n";
        contents += "      { number: '119', balance: 20},\n";
        contents += "      { number: '120', balance: 20},\n";
        contents += "      { number: '121', balance: 20},\n";
        contents += "      { number: '122', balance: 'foobar'},\n";
        contents += "      { number: '123', balance: 20}\n";
        contents += "    ]\n";
        contents += "  }\n";
        contents += "}\n";

        IProblem[] result = parseData(contents);
        FixtureHelper.assertTrue(result, result.length == 1);

        Repository kirra = getKirra();
        DataPopulator populator = new DataPopulator(kirra);
        try {
            populator.populate(new ByteArrayInputStream(contents.getBytes()));
            TestCase.fail("KirraException expected");
        } catch (KirraException e) {
            TestCase.assertEquals(KirraException.Kind.VALIDATION, e.getKind());
        }
        // normally we would rollback the transaction but at this point we
        // expect the first 3 rows only
        List<Instance> instances = kirra.getInstances("banking", "Account", false);
        TestCase.assertEquals(3, instances.size());
    }

    public void testEmptyDataReturnsZero() throws CoreException {
        parseAndCheck(KirraDataPopulatorTests.accountModel);
        Repository kirra = getKirra();

        String contents = "";
        contents += "{\n";
        contents += "  banking: {\n";
        contents += "    Account: [\n";
        contents += "    ]\n";
        contents += "  }\n";
        contents += "}\n";

        IProblem[] result = parseData(contents);
        FixtureHelper.assertTrue(result, result.length == 0);

        DataPopulator populator = new DataPopulator(kirra);
        int status = populator.populate(new ByteArrayInputStream(contents.getBytes()));
        TestCase.assertEquals(0, status);

        List<Instance> instances = kirra.getInstances("banking", "Account", false);
        TestCase.assertEquals(0, instances.size());
    }

    public void testGraph() throws CoreException {
        parseAndCheck(KirraDataPopulatorTests.accountModel);

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
        int status = populator.populate(new ByteArrayInputStream(contents.getBytes()));
        TestCase.assertEquals(2 + 4, status);

        Map<String, Instance> accounts = toMap(kirra.getInstances("banking", "Account", false), "number");
        TestCase.assertEquals(accounts.keySet(), new HashSet<String>(Arrays.asList("123", "456", "ABC", "DEF")));

        Map<String, Instance> persons = toMap(kirra.getInstances("banking", "Person", false), "name");
        TestCase.assertEquals(persons.keySet(), new HashSet<String>(Arrays.asList("John", "Mary")));

        TestCase.assertNull(accounts.get("123").getSingleRelated("owner"));
        TestCase.assertEquals("John", accounts.get("456").getSingleRelated("owner").getValue("name"));
        TestCase.assertEquals("Mary", accounts.get("ABC").getSingleRelated("owner").getValue("name"));
        TestCase.assertEquals("Mary", accounts.get("DEF").getSingleRelated("owner").getValue("name"));
    }

    public void testGraph_ManyToMany() throws CoreException {
        parseAndCheck(KirraDataPopulatorTests.accountModel);

        String contents = "";
        contents += "{\n";
        contents += "  banking: {\n";
        contents += "    AccountService: [\n";
        contents += "      { name: 'Checking'},\n";
        contents += "      { name: 'Savings'},\n";
        contents += "      { name: 'Exchange'}\n";
        contents += "    ],\n";
        contents += "    Account: [\n";
        contents += "      { number: '456', services: ['AccountService@1']},\n";
        contents += "      { number: 'ABC', services: ['AccountService@2']},\n";
        contents += "      { number: 'DEF', services: ['AccountService@1','AccountService@3']}\n";
        contents += "    ]\n";
        contents += "  }\n";
        contents += "}\n";

        FixtureHelper.assertCompilationSuccessful(parseData(contents));

        Repository kirra = getKirra();

        DataPopulator populator = new DataPopulator(kirra);
        int status = populator.populate(new ByteArrayInputStream(contents.getBytes()));
        TestCase.assertEquals(3 + 3, status);

        Map<String, Instance> accounts = toMap(kirra.getInstances("banking", "Account", false), "number");
        TestCase.assertEquals(accounts.keySet(), new HashSet<String>(Arrays.asList("456", "ABC", "DEF")));

        Map<String, Instance> persons = toMap(kirra.getInstances("banking", "AccountService", false), "name");
        TestCase.assertEquals(persons.keySet(), new HashSet<String>(Arrays.asList("Checking", "Savings", "Exchange")));

        BiFunction<Instance, Pair<String, String>, List<?>> getRelatedValues = (Instance anchor, Pair<String, String> path) -> 
        	getKirraRepository()
        	.getRelatedInstances(anchor, path.getLeft(), false)
        	.stream()
        	.map(it -> it.getValue(path.getRight()))
        	.collect(Collectors.toList());
        
        TestCase.assertEquals(Arrays.asList("Checking"), getRelatedValues.apply(accounts.get("456"), Pair.of("services", "name")));
        TestCase.assertEquals(Arrays.asList("Savings"), getRelatedValues.apply(accounts.get("ABC"), Pair.of("services", "name")));
        TestCase.assertEquals(Arrays.asList("Checking", "Exchange"), getRelatedValues.apply(accounts.get("DEF"), Pair.of("services", "name")));
    }

    public void testGraph_WithCycle() throws CoreException {
        parseAndCheck(KirraDataPopulatorTests.accountModel);

        String contents = "";
        contents += "{\n";
        contents += "  banking: {\n";
        contents += "    Company: [\n";
        contents += "      { name: 'Acme Inc.'},\n";
        contents += "      { name: 'Acme Corp.', manager: 'Person@1'},\n";
        contents += "      { name: 'Acme LLC', manager: 'Person@2'}\n";
        contents += "    ],\n";
        contents += "    Person: [\n";
        contents += "      { name: 'John', employer: 'Company@2'},\n";
        contents += "      { name: 'Martha', employer: 'Company@3'},\n";
        contents += "      { name: 'Mary', employer: 'Company@3'},\n";
        contents += "      { name: 'Paul', employer: 'Company@1'}\n";
        contents += "    ]\n";        
        contents += "  }\n";
        contents += "}\n";

        FixtureHelper.assertCompilationSuccessful(parseData(contents));

        Repository kirra = getKirra();

        DataPopulator populator = new DataPopulator(kirra);
        int status = populator.populate(new ByteArrayInputStream(contents.getBytes()));
        TestCase.assertEquals(3 + 4, status);

        Map<String, Instance> companies = toMap(kirra.getInstances("banking", "Company", false), "name");
        TestCase.assertEquals(companies.keySet(), new HashSet<String>(Arrays.asList("Acme Inc.", "Acme Corp.", "Acme LLC")));

        Map<String, Instance> persons = toMap(kirra.getInstances("banking", "Person", false), "name");
        TestCase.assertEquals(persons.keySet(), new HashSet<String>(Arrays.asList("John", "Martha", "Mary", "Paul")));

        TestCase.assertNull(companies.get("Acme Inc.").getSingleRelated("manager"));
        TestCase.assertNotNull(companies.get("Acme Corp.").getSingleRelated("manager"));
        TestCase.assertEquals("John", companies.get("Acme Corp.").getSingleRelated("manager").getValue("name"));
        TestCase.assertNotNull(companies.get("Acme LLC").getSingleRelated("manager"));
        TestCase.assertEquals("Martha", companies.get("Acme LLC").getSingleRelated("manager").getValue("name"));
    }

    
    public void testIgnoreUnknownSlots() throws CoreException {
        parseAndCheck(KirraDataPopulatorTests.accountModel);
        Repository kirra = getKirra();

        String contents = "";
        contents += "{\n";
        contents += "  banking: {\n";
        contents += "    Account: [\n";
        contents += "      { foo: 100, balance: 20}\n";
        contents += "    ]\n";
        contents += "  }\n";
        contents += "}\n";

        IProblem[] result = parseData(contents);
        // invalid property, wrong type for balance
        FixtureHelper.assertTrue(result, result.length == 1);

        DataPopulator populator = new DataPopulator(kirra);
        int status = populator.populate(new ByteArrayInputStream(contents.getBytes()));
        TestCase.assertEquals(1, status);

        // data went through
        List<Instance> instances = kirra.getInstances("banking", "Account", false);
        TestCase.assertEquals(1, instances.size());
        TestCase.assertEquals(Collections.singletonMap("balance", 20d), instances.get(0).getValues());
    }

    public void testInvalidJSONThrowsException() throws CoreException {
        parseAndCheck(KirraDataPopulatorTests.accountModel);
        Repository kirra = getKirra();

        String contents = "";
        contents += "{ [ a : 1 ] }\n";

        IProblem[] result = parseData(contents);
        FixtureHelper.assertTrue(result, result.length == 1);

        DataPopulator populator = new DataPopulator(kirra);
        try {
            populator.populate(new ByteArrayInputStream(contents.getBytes()));
            TestCase.fail("KirraException expected");
        } catch (KirraException e) {
            TestCase.assertEquals(KirraException.Kind.VALIDATION, e.getKind());
        }
    }

    public void testInvalidRelationshipType() throws CoreException {
        parseAndCheck(KirraDataPopulatorTests.accountModel);

        String contents = "";
        contents += "{\n";
        contents += "  banking: { Account: [{}, {owner: 'Account@1'}] }\n";
        contents += "}\n";

        IProblem[] result = parseData(contents);
        // invalid reference type
        FixtureHelper.assertTrue(result, result.length == 1);
        TestCase.assertTrue(result[0].getMessage(), result[0].getMessage().contains("is not a kind of"));
    }

    public void testNonExistingClass() throws CoreException {
        parseAndCheck(KirraDataPopulatorTests.accountModel);
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
            TestCase.fail("should have failed");
        } catch (KirraException e) {
            // expected
        }
        // all or nothing
        List<Instance> instances = kirra.getInstances("banking", "Account", false);
        TestCase.assertEquals(Collections.singletonMap("number", "123"), instances.get(0).getValues());
    }

    public void testSimple() throws CoreException {
        parseAndCheck(KirraDataPopulatorTests.accountModel);
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
        int status = populator.populate(new ByteArrayInputStream(contents.getBytes()));
        TestCase.assertEquals(1, status);

        List<Instance> instances = kirra.getInstances("banking", "Account", false);
        TestCase.assertEquals(1, instances.size());
        TestCase.assertEquals("123", instances.get(0).getValue("number"));
        TestCase.assertEquals(340.5, instances.get(0).getValue("balance"));
        TestCase.assertEquals("Closed", instances.get(0).getValue("status"));
    }

    private IProblem[] parseData(String contents) throws CoreException {
        return new FixtureHelper(null).parseFiles(getBaseDir(), getRepository(), Collections.singletonMap("data.json", contents));
    }

    private <T> Map<T, Instance> toMap(List<Instance> instances, String indexProperty) {
        Map<T, Instance> map = new HashMap<T, Instance>();
        for (Instance instance : instances)
            TestCase.assertNull(map.put((T) instance.getValue(indexProperty), instance));
        return map;
    }
}
