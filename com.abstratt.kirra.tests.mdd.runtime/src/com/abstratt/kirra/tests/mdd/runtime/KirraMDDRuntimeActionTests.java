package com.abstratt.kirra.tests.mdd.runtime;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import com.abstratt.kirra.Instance;
import com.abstratt.kirra.KirraException;
import com.abstratt.kirra.Operation;
import com.abstratt.kirra.Repository;
import com.abstratt.kirra.TopLevelElement;

public class KirraMDDRuntimeActionTests extends AbstractKirraMDDRuntimeTests {

	public KirraMDDRuntimeActionTests(String name) {
		super(name);
	}
	
	private static String accountModel;
	static {
		accountModel = "";
		accountModel += "package banking;\n";
		accountModel += "import base;\n";
		accountModel += "class Account\n";
		accountModel += "  attribute number : String[0,1];\n";
		accountModel += "  attribute balance : Double[0,1];\n";
		accountModel += "  operation deposit(amount : Double)\n";
		accountModel += "  precondition (amount) {amount > 0};\n";
		accountModel += "  begin\n";
		accountModel += "    self.balance := self.balance + amount;\n";
		accountModel += "  end;\n";
		accountModel += "  operation withdraw(amount : Double)\n";
		accountModel += "  precondition (amount) {amount > 0}\n";
		accountModel += "  precondition (amount) {amount <= self.balance};\n";
		accountModel += "  begin\n";
		accountModel += "    self.balance := self.balance - amount;\n";
		accountModel += "  end;\n";
		accountModel += "  static operation getAccountsWithCustomers() : Account[*];\n";
		accountModel += "  begin\n";
		accountModel += "    return Account extent.select((a : Account) : Boolean { not (a<-AccountOwner->owner == null) });\n";
		accountModel += "  end;\n";
		accountModel += "end;\n";
		accountModel += "end.";
	}
	
	private static String ownerModel;
	static {
		ownerModel = "";
		ownerModel += "package banking;\n";
		ownerModel += "class Owner\n";
		ownerModel += "  attribute name : String[0,1];\n";
		ownerModel += "  attribute accounts : Account[*];\n";
		ownerModel += "end;\n";
		ownerModel += "association AccountOwner\n";
		ownerModel += "  navigable role Owner.accounts;\n";
		ownerModel += "  navigable role owner : Owner[0,1];\n";
		ownerModel += "end;\n";
		ownerModel += "end.";
	}


	public void testAction() throws CoreException {
		parseAndCheck(accountModel, ownerModel);
		Repository kirra = getKirra();

		Instance newInstance = new Instance();
		newInstance.setEntityName("Account");
		newInstance.setEntityNamespace("banking");
		newInstance.setValue("balance", 0d);
		Instance created = kirra.createInstance(newInstance);

		assertEquals(0d, created.getValue("balance"));
		
		executeKirraOperation("banking", "Account", created.getObjectId(), "deposit", Arrays.asList(200d));
		
		assertEquals(200d, kirra.getInstance("banking", "Account", created.getObjectId(), true).getValue("balance"));

		executeKirraOperation("banking", "Account", created.getObjectId(), "withdraw", Arrays.asList(50d));
		
		assertEquals(150d, kirra.getInstance("banking", "Account", created.getObjectId(), true).getValue("balance"));
	}
	
	public void testFinder() throws CoreException {
		parseAndCheck(accountModel, ownerModel);
		Repository kirra = getKirra();
		
		kirra.getEntity("banking", "Owner");
		kirra.getEntity("banking", "Account");

		Instance owner = kirra.createInstance(kirra.newInstance("banking", "Owner"));
		Instance account1 = kirra.newInstance("banking", "Account");
		account1.setSingleRelated("owner", owner);
		account1 = kirra.createInstance(account1);
		Instance account2 = kirra.newInstance("banking", "Account");
		account2.setSingleRelated("owner", owner);
		account2 = kirra.createInstance(account2);
		Instance account3 = kirra.newInstance("banking", "Account");
		account3 = kirra.createInstance(account3);
		
		kirra.saveContext();
		
		List<?> accounts = executeKirraOperation("banking", "Account", null, "getAccountsWithCustomers", Collections.emptyList());
		assertEquals(2, accounts.size());
	}
}
