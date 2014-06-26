//package com.abstratt.mdd.target.tests.query;
//
//import java.io.IOException;
//import java.util.List;
//
//import junit.framework.Test;
//import junit.framework.TestSuite;
//
//import org.eclipse.core.runtime.CoreException;
//import org.eclipse.uml2.uml.Action;
//import org.eclipse.uml2.uml.Activity;
//import org.eclipse.uml2.uml.AddVariableValueAction;
//import org.eclipse.uml2.uml.Association;
//import org.eclipse.uml2.uml.Classifier;
//import org.eclipse.uml2.uml.Operation;
//import org.eclipse.uml2.uml.Property;
//import org.eclipse.uml2.uml.UMLPackage;
//
//import com.abstratt.mdd.core.tests.harness.AbstractRepositoryTests;
//import com.abstratt.mdd.core.util.ActivityUtils;
//import com.abstratt.mdd.target.query.Join;
//import com.abstratt.mdd.target.query.Query;
//import com.abstratt.mdd.target.query.QueryCore;
//
//public class QueryTests extends AbstractRepositoryTests {
//
//	public QueryTests(String name) {
//		super(name);
//	}
//
//	public void testCollectAssociatedQuery() throws CoreException {
//		String source = "";
//		source += "model simple;\n";
//		source += "  import base;\n";
//		source += "  class Account\n";
//		source += "    attribute balance : Integer;\n";
//		source += "  end;\n";
//		source += "  class Customer\n";
//		source += "    attribute name : String;\n";
//		source += "    static operation getCustomersWithAccount() : Customer[*];\n";
//		source += "    begin\n";
//		source += "      return (Account extent.collect(\n ";
//		source += "        (a : Account) : Customer {\n";
//		source += "          return a<-AccountCustomer->owner;\n";
//		source += "        }\n";
//		source += "      ) as Customer);\n";
//		source += "    end;\n";
//		source += "  end;\n";
//		source += "  association AccountCustomer\n";
//		source += "    navigable role account : Account[*];\n";
//		source += "    navigable role owner : Customer;\n";
//		source += "  end;\n";
//		source += "end.";
//		parseAndCheck(source);
//		Operation getCustomersWithAccountOperation = getRepository().findNamedElement(
//				"simple::Customer::getCustomersWithAccount",
//				UMLPackage.Literals.OPERATION, null);
//	    Classifier accountClass = getRepository().findNamedElement("simple::Account", UMLPackage.Literals.CLASS, null);
//	    Classifier customerClass = getRepository().findNamedElement("simple::Customer", UMLPackage.Literals.CLASS, null);
//	    Association accountCustomerAssociation = getRepository().findNamedElement("simple::AccountCustomer", UMLPackage.Literals.ASSOCIATION, null);
//        Property fedEnd = accountCustomerAssociation.getMemberEnd("account", null);
//        Property openEnd = accountCustomerAssociation.getMemberEnd("owner", null);
//
//		assertNotNull(getCustomersWithAccountOperation);
//	    assertNotNull(accountClass);
//	    assertNotNull(customerClass);
//	    assertNotNull(accountCustomerAssociation);
//
//	    Query query = new QueryCore().transformActivityToQuery((Activity) getCustomersWithAccountOperation.getMethods().get(0));
//	    assertEquals(accountClass, query.getSourceType());
//	    assertEquals(customerClass, query.getTargetType());
//	    assertEquals(1, query.getJoins().size());
//
//	    Join join = query.getJoins().get(0);
//	    assertEquals(fedEnd, join.getSource());
//	    assertEquals(openEnd, join.getTarget());
//	    assertEquals(accountCustomerAssociation, join.getAssociation());
//	}
//
//	public void testCollectAssociatedQueryTwoLevels() throws CoreException {
//		String source = "";
//		source += "model simple;\n";
//		source += "  import base;\n";
//		source += "  class Account\n";
//		source += "  end;\n";
//		source += "  class Address\n";
//		source += "  end;\n";
//		source += "  class Customer\n";
//		source += "    static operation getAddressesOfCustomersWithAccount() : Address[*];\n";
//		source += "    begin\n";
//		source += "      return (Account extent.collect(\n ";
//		source += "        (a : Account) : Address {\n";
//		source += "          return a<-AccountCustomer->owner<-CustomerAddress->residence;\n";
//		source += "        }\n";
//		source += "      ) as Address);\n";
//		source += "    end;\n";
//		source += "  end;\n";
//		source += "  association AccountCustomer\n";
//		source += "    navigable role account : Account[*];\n";
//		source += "    navigable role owner : Customer;\n";
//		source += "  end;\n";
//		source += "  association CustomerAddress\n";
//		source += "    navigable role resident : Customer;\n";
//		source += "    navigable role residence : Address;\n";
//		source += "  end;\n";
//		source += "end.";
//		parseAndCheck(source);
//		Operation getAddressesOfCustomersWithAccountOperation = getRepository().findNamedElement(
//				"simple::Customer::getAddressesOfCustomersWithAccount",
//				UMLPackage.Literals.OPERATION, null);
//	    Classifier accountClass = getRepository().findNamedElement("simple::Account", UMLPackage.Literals.CLASS, null);
//	    Classifier addressClass = getRepository().findNamedElement("simple::Address", UMLPackage.Literals.CLASS, null);
//	    Classifier customerClass = getRepository().findNamedElement("simple::Customer", UMLPackage.Literals.CLASS, null);
//	    Association accountCustomerAssociation = getRepository().findNamedElement("simple::AccountCustomer", UMLPackage.Literals.ASSOCIATION, null);
//        Property accountCustomerAccount = accountCustomerAssociation.getMemberEnd("account", null);
//        Property accountCustomerOwner = accountCustomerAssociation.getMemberEnd("owner", null);
//
//	    Association customerAddressAssociation = getRepository().findNamedElement("simple::CustomerAddress", UMLPackage.Literals.ASSOCIATION, null);
//        Property customerAddressResident = customerAddressAssociation.getMemberEnd("resident", null);
//        Property customerAddressResidence = customerAddressAssociation.getMemberEnd("residence", null);
//
//		assertNotNull(getAddressesOfCustomersWithAccountOperation);
//	    assertNotNull(accountClass);
//	    assertNotNull(customerClass);
//	    assertNotNull(addressClass);
//	    assertNotNull(accountCustomerAssociation);
//	    assertNotNull(customerAddressAssociation);
//
//	    Query query = new QueryCore().transformActivityToQuery((Activity) getAddressesOfCustomersWithAccountOperation.getMethods().get(0));
//	    assertEquals(accountClass, query.getSourceType());
//	    assertEquals(addressClass, query.getTargetType());
//	    assertEquals(2, query.getJoins().size());
//
//	    Join accountToCustomer = query.getJoins().get(0);
//	    assertEquals(accountCustomerAccount, accountToCustomer.getSource());
//	    assertEquals(accountCustomerOwner, accountToCustomer.getTarget());
//	    assertEquals(accountCustomerAssociation, accountToCustomer.getAssociation());
//
//	    Join customerToAddress = query.getJoins().get(1);
//	    assertEquals(customerAddressResident, customerToAddress.getSource());
//	    assertEquals(customerAddressResidence, customerToAddress.getTarget());
//	    assertEquals(customerAddressAssociation, customerToAddress.getAssociation());
//
//	}
//
//
//	public void testReadExtent() throws CoreException {
//		String source = "";
//		source += "model simple;\n";
//		source += "  import base;\n";
//		source += "  class Customer\n";
//		source += "    static operation getCustomers() : Customer[*];\n";
//		source += "    begin\n";
//		source += "      return Customer extent";
//		source += "    end;\n";
//		source += "  end;\n";
//		source += "end.";
//		parseAndCheck(source);
//		Operation getCustomersOperation = getRepository().findNamedElement(
//				"simple::Customer::getCustomers",
//				UMLPackage.Literals.OPERATION, null);
//		assertNotNull(getCustomersOperation);
//
//		Classifier customerClass = getRepository().findNamedElement("simple::Customer", UMLPackage.Literals.CLASS, null);
//	    assertNotNull(customerClass);
//
//	    Query query = new QueryCore().transformActivityToQuery((Activity) getCustomersOperation.getMethods().get(0));
//	    assertEquals(customerClass, query.getSourceType());
//	    assertEquals(customerClass, query.getTargetType());
//	    assertEquals(0, query.getJoins().size());
//	}
//
//	/**
//	 * Exercises {@link QueryCore#buildQuery(Action)}.
//	 */
//    public void testActionBasedQuery() throws CoreException, IOException {
//        String source = "";
//        source += "model simple;\n";
//        source += "  import mdd_types;\n";
//        source += "  class Class1\n";
//        source += "      reference attr1 : Class2;\n";
//        source += "      static operation query1() : Class2[*];\n";
//        source += "      begin\n";
//        source += "          return (Class1 extent.select((a : Class1) : Boolean { return true }).select((b : Class1) : Boolean { return true }).collect((c : Class1) : Class2 { return c->attr1 }) as Class2).select((d : Class2) : Boolean { return true }).select((e : Class2) : Boolean { return true });\n";
//        source += "      end;\n";
//        source += "  end;\n";
//        source += "  class Class2\n";
//        source += "  end;\n";
//        source += "end.";
//        parseAndCheck(source);
//
//        Operation queryOperation = getRepository().findNamedElement(
//                "simple::Class1::query1",
//                UMLPackage.Literals.OPERATION, null);
//        assertNotNull(queryOperation);
//
//        List<Action> statements = ActivityUtils.findStatements(ActivityUtils.getRootAction(queryOperation));
//        assertEquals(statements.size(), 1);
//
//        assertTrue(statements.get(0) instanceof AddVariableValueAction);
//        Query query = new QueryCore().buildQuery(ActivityUtils.getSourceAction(statements.get(0)));
//        Classifier class1 = getRepository().findNamedElement("simple::Class1", UMLPackage.Literals.CLASS, null);
//        Classifier class2 = getRepository().findNamedElement("simple::Class2", UMLPackage.Literals.CLASS, null);
//        assertEquals(class1, query.getSourceType());
//        assertEquals(class2, query.getTargetType());
//        assertEquals(1, query.getJoins().size());
//        assertEquals(4, query.getFilters().size());
//    }
//
//	public static Test suite() {
//		return new TestSuite(QueryTests.class);
//	}
// }
