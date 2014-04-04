package com.abstratt.mdd.target.tests.jpa;

import java.util.Properties;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.UMLPackage;

import com.abstratt.kirra.mdd.core.KirraMDDCore;
import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.target.ILanguageMapper;
import com.abstratt.mdd.core.target.ITargetPlatform;
import com.abstratt.mdd.core.target.TargetCore;
import com.abstratt.mdd.core.tests.harness.AbstractRepositoryBuildingTests;
import com.abstratt.mdd.core.tests.harness.AssertHelper;

public class JPAQueryTests extends AbstractRepositoryBuildingTests {

	private ILanguageMapper jpaMapper;

	public JPAQueryTests(String name) {
		super(name);
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		ITargetPlatform platform = TargetCore.getBuiltInPlatform("jpa");
		Assert.assertNotNull(platform);
		jpaMapper = (ILanguageMapper) platform.getMapper(null);
		Assert.assertNotNull(jpaMapper);
	}

	public static Test suite() {
		return new TestSuite(JPAQueryTests.class);
	}

	public void testExtent() throws CoreException {
		String source = "";
		source += "model simple;\n";
		source += "  class Account\n";
		source += "      static operation allAccounts() : Account[*]; { return Account extent };\n";
		source += "  end;\n";
		source += "end.";
		parseAndCheck(source);
		org.eclipse.uml2.uml.Class clazz = getRepository().findNamedElement(
				"simple::Account", UMLPackage.Literals.CLASS, null);
		// generate the entire class so we can check the annotations
		String actual = jpaMapper.map(clazz);
		String expected = "package simple; import java.util.Set; import javax.persistence.*; @Entity @NamedQuery(name=\"allAccounts\", query=\"select _account_ from simple.Account _account_\") public class Account { }";
		Assert.assertTrue(actual, AssertHelper.areEqual(expected, actual));
	}
	
	public void testNonQueryOperation() throws CoreException {
		String source = "";
		source += "model simple;\n";
		source += "  import base;\n";
		source += "  class Account\n";
		source += "    private attribute balance : Double;\n";
		source += "    operation withdraw(amount : Double);\n";
		source += "    begin\n";
		source += "        self.balance := self.balance - amount";
		source += "    end;\n";
		source += "  end;\n";
		source += "end.";
		parseAndCheck(source);
		org.eclipse.uml2.uml.Class clazz = getRepository().findNamedElement(
				"simple::Account", UMLPackage.Literals.CLASS, null);
		String actual = jpaMapper.map(clazz);
		String expected = "package simple; import javax.persistence.*; @Entity public class Account { private Double balance; public void withdraw(Double amount) { this.balance = this.balance - amount; } }";
		Assert.assertTrue(actual, AssertHelper.areEqual(expected, actual));
	}

	
	public void testSelectByProperty() throws CoreException {
		String source = "";
		source += "model simple;\n";
		source += "  import base;\n";
		source += "  class Account\n";
		source += "    private attribute balance : Double;\n";
		source += "    static operation getGoodAccounts() : Account[*];\n";
		source += "    begin\n";
		source += "      return Account extent.select((a : Account) : Boolean { return a.balance >= 0})";
		source += "    end;\n";
		source += "  end;\n";
		source += "end.";
		parseAndCheck(source);
		Operation getGoodAccountsOperation = getRepository().findNamedElement(
				"simple::Account::getGoodAccounts",
				UMLPackage.Literals.OPERATION, null);
		String actual = jpaMapper.mapBehavior(getGoodAccountsOperation);
		String expected = "select _account_ from simple.Account _account_ where _account_.balance >= 0";
		Assert.assertTrue(actual, AssertHelper.areEqual(expected, actual));
	}
	
	public void testSelectByPropertyWithParameter() throws CoreException {
		String source = "";
		source += "model simple;\n";
		source += "  import base;\n";
		source += "  class Account\n";
		source += "    private attribute balance : Double;\n";
		source += "    static operation getGoodAccounts(threshold : Double) : Account[*];\n";
		source += "    begin\n";
		source += "      return Account extent.select((a : Account) : Boolean { return a.balance >= threshold})";
		source += "    end;\n";
		source += "  end;\n";
		source += "end.";
		parseAndCheck(source);
		Operation getGoodAccountsOperation = getRepository().findNamedElement(
				"simple::Account::getGoodAccounts",
				UMLPackage.Literals.OPERATION, null);
		String actual = jpaMapper.mapBehavior(getGoodAccountsOperation);
		String expected = "select _account_ from simple.Account _account_ where _account_.balance >= :threshold";
		Assert.assertTrue(actual, AssertHelper.areEqual(expected, actual));
	}
	
	public void testSelectWithAllRelationalOperators() throws CoreException {
		String source = "";
		source += "model simple;\n";
		source += "  import base;\n";
		source += "  class Account\n";
		source += "    private attribute balance : Double;\n";
		source += "    static operation getGoodAccounts() : Account[*];\n";
		source += "    begin\n";
		source += "      return Account extent.select((a : Account) : Boolean { return (a.balance >= 0) or (a.balance > 0) or (a.balance = 0) or (a.balance < 0) or (a.balance <= 0)})";
		source += "    end;\n";
		source += "  end;\n";
		source += "end.";
		parseAndCheck(source);
		Operation getGoodAccountsOperation = getRepository().findNamedElement(
				"simple::Account::getGoodAccounts",
				UMLPackage.Literals.OPERATION, null);
		String actual = jpaMapper.mapBehavior(getGoodAccountsOperation);
		String expected = "select _account_ from simple.Account _account_ where _account_.balance >= 0 or _account_.balance > 0 or _account_.balance = 0 or _account_.balance < 0 or _account_.balance <= 0";
		Assert.assertTrue(actual, AssertHelper.areEqual(expected, actual));
	}
	
	public void testSelectWithAllLogicOperators() throws CoreException {
		String source = "";
		source += "model simple;\n";
		source += "  import base;\n";
		source += "  class Account\n";
		source += "    private attribute status : Boolean;\n";
		source += "    static operation getGoodAccounts() : Account[*];\n";
		source += "    begin\n";
		source += "      return Account extent.select((a : Account) : Boolean { return (a.status) or (a.status) and (a.status) and (not a.status)})";
		source += "    end;\n";
		source += "  end;\n";
		source += "end.";
		parseAndCheck(source);
		Operation getGoodAccountsOperation = getRepository().findNamedElement(
				"simple::Account::getGoodAccounts",
				UMLPackage.Literals.OPERATION, null);
		String actual = jpaMapper.mapBehavior(getGoodAccountsOperation);
		String expected = "select _account_ from simple.Account _account_ where _account_.status or _account_.status and _account_.status and not(_account_.status)";
		Assert.assertTrue(actual, AssertHelper.areEqual(expected, actual));
	}
	
	public void testSelectWithAllNumericOperators() throws CoreException {
		String source = "";
		source += "model simple;\n";
		source += "  import base;\n";
		source += "  class Account\n";
		source += "    private attribute balance : Double;\n";
		source += "    static operation getGoodAccounts() : Account[*];\n";
		source += "    begin\n";
		source += "      return Account extent.select((a : Account) : Boolean { return a.balance + 1 - 2 * 3 / 4 > 0})";
		source += "    end;\n";
		source += "  end;\n";
		source += "end.";
		parseAndCheck(source);
		Operation getGoodAccountsOperation = getRepository().findNamedElement(
				"simple::Account::getGoodAccounts",
				UMLPackage.Literals.OPERATION, null);
		String actual = jpaMapper.mapBehavior(getGoodAccountsOperation);
		String expected = "select _account_ from simple.Account _account_ where _account_.balance + 1 - 2 * 3 / 4 > 0";
		Assert.assertTrue(actual, AssertHelper.areEqual(expected, actual));
	}
	
	public void testCollectAssociated() throws CoreException {
		String source = "";
		source += "model simple;\n";
		source += "  import base;\n";
		source += "  class Account\n";
		source += "  end;\n";
		source += "  class Customer\n";
		source += "    static operation getCustomersWithAccount() : Customer[*];\n";
		source += "    begin\n";
		source += "      return (Account extent.collect(\n ";
		source += "        (a : Account) : Customer {\n";
		source += "          return a<-AccountCustomer->owner\n";
		source += "        }\n";
		source += "      ) as Customer);\n";
		source += "    end;\n";
		source += "  end;\n";
		source += "  association AccountCustomer\n";
		source += "    navigable role account : Account[*];\n";
		source += "    navigable role owner : Customer;\n";		
		source += "  end;\n";		
		source += "end.";
		parseAndCheck(source);
		Operation getCustomersWithAccountOperation = getRepository().findNamedElement(
				"simple::Customer::getCustomersWithAccount",
				UMLPackage.Literals.OPERATION, null);
		String actual = jpaMapper.mapBehavior(getCustomersWithAccountOperation);
		String expected = "select _owner_ from simple.Account _account_ inner join _account_.owner as _owner_";
		Assert.assertEquals(AssertHelper.trim(expected), AssertHelper.trim(actual));
	}
	
	public void testSelectOnAssociatedProperty() throws CoreException {
		String source = "";
		source += "model simple;\n";
		source += "  import base;\n";
		source += "  class Account\n";
		source += "    attribute balance : Double;\n";
		source += "  end;\n";
		source += "  class Customer\n";
		source += "    reference account : Account[0,1];";
		source += "    static operation getGoodCustomers(threshold : Double) : Customer[*];\n";
		source += "    begin\n";
		source += "      return (Customer extent.select(\n ";
		source += "        (c : Customer) : Boolean {\n";
		source += "          return c->account.balance > threshold\n";
		source += "        }\n";
		source += "      ) as Customer);\n";
		source += "    end;\n";
		source += "  end;\n";
		source += "end.";
		parseAndCheck(source);
		Operation getGoodCustomersOperation = getRepository().findNamedElement(
				"simple::Customer::getGoodCustomers",
				UMLPackage.Literals.OPERATION, null);
		String actual = jpaMapper.mapBehavior(getGoodCustomersOperation);
		String expected = "select _customer_ from simple.Customer _customer_ where _customer_.account.balance > :threshold";
		Assert.assertEquals(AssertHelper.trim(expected), AssertHelper.trim(actual));
	}

	public void testSelectOnAssociatedPropertySecondLevel() throws CoreException {
		String source = "";
		source += "model simple;\n";
		source += "  import base;\n";
		source += "  class Account\n";
		source += "    attribute balance : Double;\n";
		source += "    reference branch : Branch[0,1];\n";
		source += "  end;\n";
		source += "  class Customer\n";
		source += "    reference account : Account[0,1];";
		source += "    static operation getLuckyCustomers() : Customer[*];\n";
		source += "    begin\n";
		source += "      return (Customer extent.select(\n ";
		source += "        (c : Customer) : Boolean {\n";
		source += "          return c->account->branch.isOpenOnWeekends\n";
		source += "        }\n";
		source += "      ) as Customer);\n";
		source += "    end;\n";
		source += "  end;\n";
		source += "  class Branch\n";
		source += "      attribute isOpenOnWeekends : Boolean;\n";
		source += "  end;\n";
		source += "end.";
		parseAndCheck(source);
		Operation getLuckyCustomersOperation = getRepository().findNamedElement(
				"simple::Customer::getLuckyCustomers",
				UMLPackage.Literals.OPERATION, null);
		String actual = jpaMapper.mapBehavior(getLuckyCustomersOperation);
		String expected = "select _customer_ from simple.Customer _customer_ where _customer_.account.branch.isOpenOnWeekends";
		Assert.assertEquals(AssertHelper.trim(expected), AssertHelper.trim(actual));
	}
	
	public void testSelectWithExists() throws CoreException {
		String source = "";
		source += "model simple;\n";
		source += "  import base;\n";
		source += "  class Account\n";
		source += "    attribute balance : Double;\n";
		source += "    reference branch : Branch[0,1];\n";
		source += "  end;\n";
		source += "  class Customer\n";
		source += "    reference account : Account[*];";
		source += "    static operation getLuckyCustomers() : Customer[*];\n";
		source += "    begin\n";
		source += "      return (Customer extent.select(\n ";
		source += "        (c : Customer) : Boolean {\n";
		source += "          return c->account.exists((a:Account):Boolean { return a->branch.isOpenOnWeekends})\n";
		source += "        }\n";
		source += "      ) as Customer);\n";
		source += "    end;\n";
		source += "  end;\n";
		source += "  class Branch\n";
		source += "      attribute isOpenOnWeekends : Boolean;\n";
		source += "  end;\n";
		source += "end.";
		parseAndCheck(source);
		Operation getLuckyCustomersOperation = getRepository().findNamedElement(
				"simple::Customer::getLuckyCustomers",
				UMLPackage.Literals.OPERATION, null);
		String actual = jpaMapper.mapBehavior(getLuckyCustomersOperation);
		String expected = "select _customer_ from simple.Customer _customer_ where exists (select _account_ from Account _account inner join Branch _branch_ where _account_.owner = _customer and _branch_.isOpenOnWeekends)";
		//TODO need to make this pass...
		Assert.assertEquals(AssertHelper.trim(expected), AssertHelper.trim(actual));
	}

	
	public void testCollectAssociatedViaAnonymousAssociation() throws CoreException {
		String source = "";
		source += "model simple;\n";
		source += "  import base;\n";
		source += "  class Account\n";
		source += "    reference owner : Customer;\n";
		source += "  end;\n";
		source += "  class Customer\n";
		source += "    static operation getCustomersWithAccount() : Customer[*];\n";
		source += "    begin\n";
		source += "      return (Account extent.collect(\n ";
		source += "        (a : Account) : Customer {\n";
		source += "          return a->owner;\n";
		source += "        }\n";
		source += "      ) as Customer);\n";
		source += "    end;\n";
		source += "  end;\n";
		source += "end.";
		parseAndCheck(source);
		Operation getCustomersWithAccountOperation = getRepository().findNamedElement(
				"simple::Customer::getCustomersWithAccount",
				UMLPackage.Literals.OPERATION, null);
		String actual = jpaMapper.mapBehavior(getCustomersWithAccountOperation);
		String expected = "select _owner_ from simple.Account _account_ inner join _account_.owner as _owner_";
		Assert.assertEquals(AssertHelper.trim(expected), AssertHelper.trim(actual));
	}
	
	public void testCollectAssociatedSecondLevel() throws CoreException {
		String source = "";
		source += "model simple;\n";
		source += "  import base;\n";
		source += "  class Account\n";
		source += "  end;\n";
		source += "  class Address\n";
		source += "  end;\n";
		source += "  class Customer\n";
		source += "    static operation getAddressesOfCustomersWithAccount() : Address[*];\n";
		source += "    begin\n";
		source += "      return (Account extent.collect(\n ";
		source += "        (a : Account) : Address {\n";
		source += "          return a<-AccountCustomer->owner<-CustomerAddress->residence;\n";
		source += "        }\n";
		source += "      ) as Address);\n";
		source += "    end;\n";
		source += "  end;\n";
		source += "  association AccountCustomer\n";
		source += "    navigable role account : Account[*];\n";
		source += "    navigable role owner : Customer;\n";		
		source += "  end;\n";
		source += "  association CustomerAddress\n";
		source += "    navigable role resident : Customer;\n";
		source += "    navigable role residence : Address;\n";		
		source += "  end;\n";		
		source += "end.";
		parseAndCheck(source);
		Operation getAddressesOfCustomersWithAccountOperation = getRepository().findNamedElement(
				"simple::Customer::getAddressesOfCustomersWithAccount",
				UMLPackage.Literals.OPERATION, null);
		Assert.assertNotNull(getAddressesOfCustomersWithAccountOperation);
		String actual = jpaMapper.mapBehavior(getAddressesOfCustomersWithAccountOperation);
		String expected = "select _residence_ from simple.Account _account_ inner join _account_.owner as _owner_ inner join _owner_.residence as _residence_";
		Assert.assertEquals(AssertHelper.trim(expected), AssertHelper.trim(actual));
	}
	
	@Override
	protected Properties createDefaultSettings() {
		Properties defaultSettings = super.createDefaultSettings();
		// so the kirra profile is available as a system package (no need to load)
		defaultSettings.setProperty("mdd.enableKirra", Boolean.TRUE.toString());
		// so kirra stereotypes are automatically applied
		defaultSettings.setProperty(IRepository.WEAVER, KirraMDDCore.WEAVER);
		// so classes extend Object by default (or else weaver ignores them)
		defaultSettings.setProperty(IRepository.EXTEND_BASE_OBJECT, Boolean.TRUE.toString());
		return defaultSettings;
	}

}
