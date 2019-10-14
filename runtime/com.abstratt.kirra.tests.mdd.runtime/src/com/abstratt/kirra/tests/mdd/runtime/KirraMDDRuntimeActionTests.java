package com.abstratt.kirra.tests.mdd.runtime;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;

import com.abstratt.kirra.Instance;
import com.abstratt.kirra.Repository;

public class KirraMDDRuntimeActionTests extends AbstractKirraMDDRuntimeTests {

    private static String accountModel;

    static {
        KirraMDDRuntimeActionTests.accountModel = "";
        KirraMDDRuntimeActionTests.accountModel += "package banking;\n";
        KirraMDDRuntimeActionTests.accountModel += "import base;\n";
        KirraMDDRuntimeActionTests.accountModel += "class Account\n";
        KirraMDDRuntimeActionTests.accountModel += "  attribute number : String[0,1];\n";
        KirraMDDRuntimeActionTests.accountModel += "  attribute balance : Double[1] := 0;\n";
        KirraMDDRuntimeActionTests.accountModel += "  operation deposit(amount : Double)\n";
        KirraMDDRuntimeActionTests.accountModel += "  precondition (amount) {amount > 0};\n";
        KirraMDDRuntimeActionTests.accountModel += "  begin\n";
        KirraMDDRuntimeActionTests.accountModel += "    self.balance := self.balance + amount;\n";
        KirraMDDRuntimeActionTests.accountModel += "  end;\n";
        KirraMDDRuntimeActionTests.accountModel += "  operation withdraw(amount : Double)\n";
        KirraMDDRuntimeActionTests.accountModel += "  precondition (amount) {amount > 0}\n";
        KirraMDDRuntimeActionTests.accountModel += "  precondition (amount) {amount <= self.balance};\n";
        KirraMDDRuntimeActionTests.accountModel += "  begin\n";
        KirraMDDRuntimeActionTests.accountModel += "    self.balance := self.balance - amount;\n";
        KirraMDDRuntimeActionTests.accountModel += "  end;\n";
        KirraMDDRuntimeActionTests.accountModel += "  static operation getAccountsWithCustomers() : Account[*];\n";
        KirraMDDRuntimeActionTests.accountModel += "  begin\n";
        KirraMDDRuntimeActionTests.accountModel += "    return Account extent.select((a : Account) : Boolean { not (a<-AccountOwner->owner == null) });\n";
        KirraMDDRuntimeActionTests.accountModel += "  end;\n";
        KirraMDDRuntimeActionTests.accountModel += "end;\n";
        KirraMDDRuntimeActionTests.accountModel += "end.";
    }
    private static String ownerModel;

    static {
        KirraMDDRuntimeActionTests.ownerModel = "";
        KirraMDDRuntimeActionTests.ownerModel += "package banking;\n";
        KirraMDDRuntimeActionTests.ownerModel += "class Owner\n";
        KirraMDDRuntimeActionTests.ownerModel += "  attribute name : String[0,1];\n";
        KirraMDDRuntimeActionTests.ownerModel += "  attribute accounts : Account[*];\n";
        KirraMDDRuntimeActionTests.ownerModel += "end;\n";
        KirraMDDRuntimeActionTests.ownerModel += "association AccountOwner\n";
        KirraMDDRuntimeActionTests.ownerModel += "  navigable role Owner.accounts;\n";
        KirraMDDRuntimeActionTests.ownerModel += "  navigable role owner : Owner[0,1];\n";
        KirraMDDRuntimeActionTests.ownerModel += "end;\n";
        KirraMDDRuntimeActionTests.ownerModel += "end.";
    }

    public KirraMDDRuntimeActionTests(String name) {
        super(name);
    }

    public void testAction() throws CoreException {
        parseAndCheck(KirraMDDRuntimeActionTests.accountModel, KirraMDDRuntimeActionTests.ownerModel);
        Repository kirra = getKirra();

        Instance newInstance = new Instance();
        newInstance.setEntityName("Account");
        newInstance.setEntityNamespace("banking");
        newInstance.setValue("balance", 0d);
        Instance created = kirra.createInstance(newInstance);

        TestCase.assertEquals(0d, created.getValue("balance"));

        executeKirraOperation("banking", "Account", created.getObjectId(), "deposit", Arrays.asList(200d));

        TestCase.assertEquals(200d, kirra.getInstance("banking", "Account", created.getObjectId(), true).getValue("balance"));

        executeKirraOperation("banking", "Account", created.getObjectId(), "withdraw", Arrays.asList(50d));

        TestCase.assertEquals(150d, kirra.getInstance("banking", "Account", created.getObjectId(), true).getValue("balance"));
    }

    public void testFinder() throws CoreException {
        parseAndCheck(KirraMDDRuntimeActionTests.accountModel, KirraMDDRuntimeActionTests.ownerModel);
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
        TestCase.assertEquals(2, accounts.size());
    }
}
