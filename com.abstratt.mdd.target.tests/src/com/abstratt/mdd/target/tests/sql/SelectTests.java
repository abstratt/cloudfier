package com.abstratt.mdd.target.tests.sql;

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
import static com.abstratt.mdd.core.tests.harness.AssertHelper.trim;

/**
 * Tests generation of SQL select statements. 
 */
public class SelectTests extends AbstractRepositoryBuildingTests {

	private ILanguageMapper sqlMapper;

	public SelectTests(String name) {
		super(name);
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		ITargetPlatform platform = TargetCore
				.getBuiltInPlatform("sql");
		Assert.assertNotNull(platform);
		sqlMapper = (ILanguageMapper) platform.getMapper(null);
		Assert.assertNotNull(sqlMapper);
	}

	public void testExtent() throws CoreException {
		String source = "";
		source += "model simple;\n";
		source += "  class Account\n";
		source += "    static operation getAllAccounts() : Account[*];\n";
		source += "    begin\n";
		source += "      return Account extent;\n";
		source += "    end;\n";
		source += "  end;\n";
		source += "end.";
		parseAndCheck(source);
		Operation getAllAccountsOperation = getRepository().findNamedElement(
				"simple::Account::getAllAccounts",
				UMLPackage.Literals.OPERATION, null);
		String actual = sqlMapper.mapBehavior(getAllAccountsOperation);
		String expected = "select _account_.*from Account _account_";
		Assert.assertEquals(trim(expected), trim(actual));
	}
	
	public void testSelectByProperty() throws CoreException {
		String source = "";
		source += "model simple;\n";
		source += "  import base;\n";
		source += "  class Account\n";
		source += "    attribute balance : Integer;\n";
		source += "    static operation getGoodAccounts() : Account[*];\n";
		source += "    begin\n";
		source += "      return Account extent.select(\n ";
		source += "        (a : Account) : Boolean {\n";
		source += "          return a.balance >= 0;\n";
		source += "        }\n";
		source += "      );\n";		
		source += "    end;\n";
		source += "  end;\n";
		source += "end.";
		parseAndCheck(source);
		Operation getGoodAccountsOperation = getRepository().findNamedElement(
				"simple::Account::getGoodAccounts",
				UMLPackage.Literals.OPERATION, null);
		String actual = sqlMapper.mapBehavior(getGoodAccountsOperation);
		String expected = "select _account_.* from Account _account_ where _account_.balance >= 0";
		Assert.assertEquals(AssertHelper.trim(expected), AssertHelper.trim(actual));
	}
	
	public void testFindAnyByProperty() throws CoreException {
		String source = "";
		source += "model simple;\n";
		source += "  import base;\n";
		source += "  class Account\n";
		source += "    attribute number : String;\n";		
		source += "    attribute balance : Integer;\n";
		source += "    static operation findAccountByNumber(number : String) : Account[0,1];\n";
		source += "    begin\n";
		source += "      return Account extent.\\any(\n ";
		source += "        (a : Account) : Boolean {\n";
		source += "          return a.number = number;\n";
		source += "        }\n";
		source += "      );\n";		
		source += "    end;\n";
		source += "  end;\n";
		source += "end.";
		parseAndCheck(source);
		Operation findAccountByNumberOperation = getRepository().findNamedElement(
				"simple::Account::findAccountByNumber",
				UMLPackage.Literals.OPERATION, null);
		String actual = sqlMapper.mapBehavior(findAccountByNumberOperation);
		String expected = "select _account_.* from Account _account_ where _account_.number = ?";
		Assert.assertEquals(AssertHelper.trim(expected), AssertHelper.trim(actual));
	}

	
	public void testCollectAssociatedSQLGeneration() throws CoreException {
		buildAccountCustomerModel();
		Operation getCustomersWithAccountOperation = getRepository().findNamedElement(
				"simple::Customer::getCustomersWithAccount",
				UMLPackage.Literals.OPERATION, null);
		String actual = sqlMapper.mapBehavior(getCustomersWithAccountOperation);
		String expected = "select _owner_.* from Account _account_ inner join Customer _owner_ on _account_._ownerID_ = _owner_._customerID_";
		Assert.assertEquals(AssertHelper.trim(expected), AssertHelper.trim(actual));
	}

	private void buildAccountCustomerModel() throws CoreException {
		String source = "";
		source += "model simple;\n";
		source += "  import base;\n";
		source += "  class Account\n";
		source += "    attribute balance : Integer;\n";
		source += "  end;\n";
		source += "  class Customer\n";
		source += "    attribute name : String;\n";
		source += "    static operation getCustomersWithAccount() : Customer[*];\n";
		source += "    begin\n";
		source += "      return (Account extent.collect(\n ";
		source += "        (a : Account) : Customer {\n";
		source += "          return a<-AccountCustomer->owner;\n";
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
	}

	public static Test suite() {
		return new TestSuite(SelectTests.class);
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