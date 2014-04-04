package com.abstratt.mdd.core.tests.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Operation;

import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.runtime.RuntimeClass;
import com.abstratt.mdd.core.runtime.RuntimeObject;
import com.abstratt.mdd.core.runtime.types.BasicType;
import com.abstratt.mdd.core.runtime.types.CollectionType;
import com.abstratt.mdd.core.runtime.types.GroupingType;
import com.abstratt.mdd.core.runtime.types.IntegerType;
import com.abstratt.mdd.core.runtime.types.NumberType;
import com.abstratt.mdd.core.runtime.types.RealType;

public class RuntimeCollectionTests extends AbstractRuntimeTests {

	private static String simpleModel = null;

	static {
		simpleModel = "";
		simpleModel += "model simple;\n";
		simpleModel += "import base;\n";
		simpleModel += "  class Account\n";
		simpleModel += "    attribute balance : Integer[0,1];\n";
		simpleModel += "    operation deposit(valor : Integer);\n";
		simpleModel += "    begin\n";
		simpleModel += "      self.balance := self.balance + valor;\n";
		simpleModel += "    end;\n";
		simpleModel += "    static operation newAccount(balance : Integer) : Account;\n";
		simpleModel += "    begin\n";
		simpleModel += "      var newAccount : Account;\n";
		simpleModel += "      newAccount := new Account;\n";
		simpleModel += "      newAccount.balance := balance;\n";
		simpleModel += "      return newAccount;\n";
		simpleModel += "    end;\n";
		simpleModel += "  end;\n";
		simpleModel += "end.";
	}

	public static Test suite() {
		return new TestSuite(RuntimeCollectionTests.class);
	}

	public RuntimeCollectionTests(String name) {
		super(name);
	}

	public void testLoopReadingLocalVar() throws CoreException {
		String behavior = "";
		behavior += "model tests;\n";
		behavior += "  import simple;\n";
		behavior += "  class TestDriver\n";
		behavior += "    static operation createAccounts();\n";
		behavior += "    begin\n";
		behavior += "      Account#newAccount(10);\n";
		behavior += "      Account#newAccount(20);\n";
		behavior += "      Account#newAccount(30);\n";
		behavior += "    end;\n";
		behavior += "    static operation batchDeposit(accounts: Account[*], amount : Integer);\n";
		behavior += "    begin\n";
		behavior += "      accounts.forEach((account : Account)  { account.deposit(amount); });\n";
		behavior += "    end;\n";
		behavior += "  end;\n";
		behavior += "end.\n";
		parseAndCheck(simpleModel, behavior);
		// create accounts
		runStaticOperation("tests::TestDriver", "createAccounts");
		RuntimeClass runtimeClass = getRuntimeClass("simple::Account");
		CollectionType allInstances = runtimeClass.getAllInstances();
		// obtain initial balances		
		Map<RuntimeObject, IntegerType> currentBalances = new HashMap<RuntimeObject, IntegerType>();
		for (BasicType account : allInstances.getBackEnd())
			currentBalances.put((RuntimeObject) account, (IntegerType) readAttribute((RuntimeObject) account, "balance"));
		// adjust balances 
		int depositedAmount = 50;
		runStaticOperation("tests::TestDriver", "batchDeposit", allInstances, new IntegerType(depositedAmount));
		// compare balances after adjustment
		for (BasicType account : allInstances.getBackEnd()) {
			IntegerType newBalance = (IntegerType) readAttribute((RuntimeObject) account, "balance");
			IntegerType originalBalance = currentBalances.get(account);
			assertEquals(new IntegerType(((long) originalBalance.asDouble()) + depositedAmount), newBalance);
		}
	}

	
	public void testLoopWritingLocalVar() throws CoreException {
		String behavior = "";
		behavior += "model tests;\n";
		behavior += "  import simple;\n";
		behavior += "  class TestDriver\n";
		behavior += "    static operation createAccounts();\n";
		behavior += "    begin\n";
		behavior += "      Account#newAccount(10);\n";
		behavior += "      Account#newAccount(20);\n";
		behavior += "      Account#newAccount(30);\n";
		behavior += "    end;\n";
		behavior += "    static operation sumBalances(accounts: Account[*]) : Integer;\n";
		behavior += "    begin\n";
		behavior += "      var sum : Integer;\n";
		behavior += "      sum := 0;\n";
		behavior += "      accounts.forEach((account : Account) { sum := sum + account.balance; });\n";
		behavior += "      return sum;\n";
		behavior += "    end;\n";
		behavior += "  end;\n";
		behavior += "end.\n";
		parseAndCheck(simpleModel, behavior);
		// create accounts
		runStaticOperation("tests::TestDriver", "createAccounts");
		RuntimeClass runtimeClass = getRuntimeClass("simple::Account");
		CollectionType allInstances = runtimeClass.getAllInstances();
		// obtain initial balances		
		double expectedSum = 0;
		for (BasicType account : allInstances.getBackEnd()) {
			IntegerType balance = (IntegerType) readAttribute((RuntimeObject) account, "balance");
			expectedSum += balance.asDouble();
		}
		// sum balances 
		IntegerType sum = (IntegerType) runStaticOperation("tests::TestDriver", "sumBalances", allInstances);
		// compare extracted
		assertNotNull(sum);
		assertEquals(expectedSum, sum.asDouble());
	}

	public void testCollectionSize() throws CoreException {
		String behavior = "";
		behavior += "model tests;\n";
		behavior += "  import simple;\n";
		behavior += "  class TestDriver\n";
		behavior += "    static operation countAccounts() : Integer;\n";
		behavior += "    begin\n";
		behavior += "      return Account extent.size();\n";
		behavior += "    end;\n";
		behavior += "  end;\n";
		behavior += "end.\n";
		parseAndCheck(simpleModel, behavior);
		// create accounts
		RuntimeClass accountClass = getRuntimeClass("simple::Account");
		assertEquals(new IntegerType(0), runStaticOperation("tests::TestDriver", "countAccounts"));
		List<RuntimeObject> instances = new ArrayList<RuntimeObject>();
		instances.add(accountClass.newInstance(true));
		instances.get(0).save();
		assertEquals(new IntegerType(1), runStaticOperation("tests::TestDriver", "countAccounts"));
		instances.add(accountClass.newInstance(true));
		instances.add(accountClass.newInstance(true));
		instances.get(1).save();
		instances.get(2).save();
		assertEquals(new IntegerType(3), runStaticOperation("tests::TestDriver", "countAccounts"));
		instances.get(0).destroy();
		assertEquals(new IntegerType(2), runStaticOperation("tests::TestDriver", "countAccounts"));
		instances.get(1).destroy();
		instances.get(2).destroy();
		assertEquals(new IntegerType(0), runStaticOperation("tests::TestDriver", "countAccounts"));
	}

	public void testExtent() throws CoreException {
		String behavior = "";
		behavior += "model tests;\n";
		behavior += "  import simple;\n";
		behavior += "  class TestDriver\n";
		behavior += "    static operation createInstances();\n";
		behavior += "    begin\n";
		behavior += "      Account#newAccount(0);\n";
		behavior += "      Account#newAccount(0);\n";
		behavior += "      Account#newAccount(0);\n";
		behavior += "    end;\n";		
		behavior += "    static operation getExtent() : Account[*];\n";
		behavior += "    begin\n";
		behavior += "      return Account extent;\n";
		behavior += "    end;\n";
		behavior += "  end;\n";
		behavior += "end.\n";
		parseAndCheck(simpleModel, behavior);
		runStaticOperation("tests::TestDriver", "createInstances");				
		CollectionType result = (CollectionType) runStaticOperation("tests::TestDriver", "getExtent");
		assertEquals(3, result.getBackEnd().size());
		for (Iterator i = result.getBackEnd().iterator(); i.hasNext();) {
			RuntimeObject current = (RuntimeObject) i.next();
			assertEquals("simple::Account", current.getRuntimeClass().getModelClassifier().getQualifiedName());
		}
	}
	
	public void testNoExtent() throws CoreException {
		String behavior = "";
		behavior += "model tests;\n";
		behavior += "  import simple;\n";
		behavior += "  class TestDriver\n";
		behavior += "    static operation getExtent() : Account[*];\n";
		behavior += "    begin\n";
		behavior += "      return Account extent;\n";
		behavior += "    end;\n";
		behavior += "  end;\n";
		behavior += "end.\n";
		parseAndCheck(simpleModel, behavior);
		CollectionType result = (CollectionType) runStaticOperation("tests::TestDriver", "getExtent");
		assertEquals(0, result.getBackEnd().size());
	}


	public void testIterator() throws CoreException {
		String behavior = "";
		behavior += "model tests;\n";
		behavior += "  import simple;\n";
		behavior += "  class TestDriver\n";
		behavior += "    static operation createAccounts();\n";
		behavior += "    begin\n";
		behavior += "      Account#newAccount(10);\n";
		behavior += "      Account#newAccount(20);\n";
		behavior += "      Account#newAccount(30);\n";
		behavior += "    end;\n";
		behavior += "    static operation batchDeposit(accounts: Account[*]);\n";
		behavior += "    begin\n";
		behavior += "      accounts.forEach((account : Account) { account.deposit(account.balance*2); });\n";
		behavior += "    end;\n";
		behavior += "  end;\n";
		behavior += "end.\n";
		parseAndCheck(simpleModel, behavior);
		// create accounts
		runStaticOperation("tests::TestDriver", "createAccounts");
		RuntimeClass runtimeClass = getRuntimeClass("simple::Account");
		CollectionType allInstances = runtimeClass.getAllInstances();
		// obtain initial balances		
		List<IntegerType> initialBalances = new ArrayList<IntegerType>();
		for (BasicType account : allInstances.getBackEnd()) {
			IntegerType balance = (IntegerType) readAttribute((RuntimeObject) account, "balance");
			initialBalances.add(balance);
		}
		// run batch deposits 
		runStaticOperation("tests::TestDriver", "batchDeposit", allInstances);
		// check new balances
		int index = 0;
		for (BasicType account : allInstances.getBackEnd()) {
			IntegerType initialBalance = initialBalances.get(index++);
			IntegerType newBalance = (IntegerType) readAttribute((RuntimeObject) account, "balance");
			assertEquals(initialBalance.asDouble() * 3, newBalance.asDouble());
		}
	}

	public void testMap() throws CoreException {
		String behavior = "";
		behavior += "model tests;\n";
		behavior += "  import simple;\n";
		behavior += "  class TestDriver\n";
		behavior += "    static operation createAccounts();\n";
		behavior += "    begin\n";
		behavior += "      Account#newAccount(10);\n";
		behavior += "      Account#newAccount(5);\n";
		behavior += "      Account#newAccount(20);\n";
		behavior += "    end;\n";
		behavior += "    static operation extractBalances(accounts: Account[*]) : {:Double}[*];\n";
		behavior += "    begin\n";
		behavior += "      return (accounts.collect((account : Account) : {:Double} { {value := account.balance * 1.0} }) as {:Double});\n";
		behavior += "    end;\n";
		behavior += "  end;\n";
		behavior += "end.\n";
		parseAndCheck(simpleModel, behavior);
		// create accounts
		runStaticOperation("tests::TestDriver", "createAccounts");
		CollectionType allInstances = getRuntimeClass("simple::Account").getAllInstances();
		// obtain initial balances		
		List<Double> initialBalances = new ArrayList<Double>();
		for (BasicType account : allInstances.getBackEnd()) {
			IntegerType balance = (IntegerType) readAttribute((RuntimeObject) account, "balance");
			initialBalances.add(balance.asDouble());
		}
		// extract balances 
		CollectionType balances = (CollectionType) runStaticOperation("tests::TestDriver", "extractBalances", allInstances);
		// compare extracted
		assertEquals("sizes don't match", initialBalances.size(), balances.getBackEnd().size());
		List<Double> results = new ArrayList<Double>();
		for (BasicType value : balances.getBackEnd())
			results.add(((NumberType<?>) readAttribute((RuntimeObject) value, "value")).asDouble());	
		Collections.sort(initialBalances);
		Collections.sort(results);
		Assert.assertEquals(initialBalances, results);
	}

	public void testGroupBy() throws CoreException {
		String behavior = "";
		behavior += "model tests;\n";
		behavior += "  import simple;\n";
		behavior += "  class TestDriver\n";
		behavior += "    static operation createAccounts();\n";
		behavior += "    begin\n";
		behavior += "      Account#newAccount(10);\n";
		behavior += "      Account#newAccount(5);\n";
		behavior += "      Account#newAccount(20);\n";
		behavior += "      Account#newAccount(25);\n";
		behavior += "      Account#newAccount(15);\n";
		behavior += "    end;\n";
		behavior += "    static operation splitAccounts(accounts: Account[*]) : Grouping<Account>;\n";
		behavior += "    begin\n";
		behavior += "      return (accounts.groupBy((account : Account) : Double { account.balance / 10 }) as Grouping<Account>);\n";
		behavior += "    end;\n";
		behavior += "    static operation classTotals(accounts: Account[*]) : Double[*];\n";
		behavior += "    begin\n";
		behavior += "      return (TestDriver#splitAccounts(accounts).collect(\n";
		behavior += "          (grouped : Account[*]) : Double {\n";
		behavior += "              grouped.sum((a : Account) : Double { a.balance })\n";
		behavior += "          }\n";
		behavior += "      ) as Double);\n";
		behavior += "    end;\n";
		behavior += "  end;\n";
		behavior += "end.\n";
		parseAndCheck(simpleModel, behavior);
		// create accounts
		runStaticOperation("tests::TestDriver", "createAccounts");
		CollectionType allInstances = getRuntimeClass("simple::Account").getAllInstances();
		// obtain initial balances		
		List<Double> initialBalances = new ArrayList<Double>();
		for (BasicType account : allInstances.getBackEnd()) {
			IntegerType balance = (IntegerType) readAttribute((RuntimeObject) account, "balance");
			initialBalances.add(balance.asDouble());
		}
		// extract balances 
		GroupingType balances = (GroupingType) runStaticOperation("tests::TestDriver", "splitAccounts", allInstances);
		// compare extracted
		Map<BasicType, CollectionType> groups = balances.getBackEnd();
		assertEquals(groups.size(), 3);
		List<Long> keys = new ArrayList<Long>();
		for (BasicType key : groups.keySet())
			keys.add(((IntegerType) key).primitiveValue());	
		Collections.sort(keys);
		Assert.assertEquals(Arrays.asList(0l,1l,2l), keys);
		
		Collection<BasicType> group1 = groups.get(IntegerType.fromValue(1)).getBackEnd();
		List<Long> values = new ArrayList<Long>();
		for (BasicType account : group1)
			values.add(((IntegerType) readAttribute((RuntimeObject) account, "balance")).primitiveValue());
		Collections.sort(values);
		Assert.assertEquals(Arrays.asList(10l,15l), values);
		
		CollectionType aggregatedBalances = (CollectionType) runStaticOperation("tests::TestDriver", "classTotals", allInstances);
		List<Long> aggregatedValues = new ArrayList<Long>();
		for (BasicType aggregatedBalance : aggregatedBalances.getBackEnd())
			aggregatedValues.add(((IntegerType) aggregatedBalance).primitiveValue());
		Collections.sort(aggregatedValues);
		Assert.assertEquals(Arrays.asList(5l,25l,45l), aggregatedValues);
	}
	
	public void testSelect() throws CoreException {
		String source = "";
		source += "model tests;\n";
		source += "  import simple;\n";
		source += "  class TestDriver\n";
		source += "    static operation selection() : Account[*];\n";
		source += "    begin\n";
		source += "    return Account extent.select((account : Account) : Boolean {\n";
		source += "        account.balance > 10\n";
		source += "      }).asSet();\n";
		source += "    end;\n";
		source += "  end;\n";
		source += "end.";
		parseAndCheck(simpleModel, source);
		Classifier accountClass = (Classifier) getRepository().findNamedElement("simple::Account", IRepository.PACKAGE.getClassifier(), null);
		Classifier testDriverClass = (Classifier) getRepository().findNamedElement("tests::TestDriver", IRepository.PACKAGE.getClassifier(), null);
		Operation selectionOperation = testDriverClass.getOperation("selection", null, null);
		Operation newAccountOperation = accountClass.getOperation("newAccount", null, null);
		assertNotNull(selectionOperation);
		assertNotNull(newAccountOperation);

		CollectionType result = (CollectionType) getRuntime().runOperation(null, selectionOperation);
		assertEquals(0, result.getBackEnd().size());

		getRuntime().runOperation(null, newAccountOperation, IntegerType.fromValue(15));
		result = (CollectionType) getRuntime().runOperation(null, selectionOperation);
		assertEquals(1, result.getBackEnd().size());

		getRuntime().runOperation(null, newAccountOperation, IntegerType.fromValue(8));
		result = (CollectionType) getRuntime().runOperation(null, selectionOperation);
		assertEquals(1, result.getBackEnd().size());

		getRuntime().runOperation(null, newAccountOperation, IntegerType.fromValue(20));
		result = (CollectionType) getRuntime().runOperation(null, selectionOperation);
		assertEquals(2, result.getBackEnd().size());
	}
	
   public void testReduce() throws CoreException {
        String source = "";
        source += "model tests;\n";
        source += "  import simple;\n";
        source += "  class TestDriver\n";
        source += "    static operation reduction() : Double;\n";
        source += "    begin\n";
        source += "    return (Account extent.reduce((account : Account, partial : Double) : Double {\n";
        source += "        account.balance + partial\n";
        source += "      }, 0.0) as Double);\n";
        source += "    end;\n";
        source += "  end;\n";
        source += "end.";
        parseAndCheck(simpleModel, source);
        Classifier accountClass = (Classifier) getRepository().findNamedElement("simple::Account", IRepository.PACKAGE.getClassifier(), null);
        Classifier testDriverClass = (Classifier) getRepository().findNamedElement("tests::TestDriver", IRepository.PACKAGE.getClassifier(), null);
        Operation reductionOperation = testDriverClass.getOperation("reduction", null, null);
        Operation newAccountOperation = accountClass.getOperation("newAccount", null, null);
        assertNotNull(reductionOperation);
        assertNotNull(newAccountOperation);

        RealType result = (RealType) getRuntime().runOperation(null, reductionOperation);
        assertEquals(0.0, result.asDouble());

        getRuntime().runOperation(null, newAccountOperation, IntegerType.fromValue(15));
        result = (RealType) getRuntime().runOperation(null, reductionOperation);
        assertEquals(15d, result.asDouble());

        getRuntime().runOperation(null, newAccountOperation, IntegerType.fromValue(8));
        result = (RealType) getRuntime().runOperation(null, reductionOperation);
        assertEquals(23d, result.asDouble());
    }
}
