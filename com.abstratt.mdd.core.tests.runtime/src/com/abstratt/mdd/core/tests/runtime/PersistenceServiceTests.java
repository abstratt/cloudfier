//package com.abstratt.mdd.core.tests.runtime;
//
//import com.abstratt.mdd.core.IRepository;
//import com.abstratt.mdd.core.runtime.RuntimeClass;
//import com.abstratt.mdd.core.runtime.RuntimeObject;
//import com.abstratt.mdd.core.runtime.RuntimeRunnable;
//import com.abstratt.mdd.core.runtime.persistence.NonPersistentClassException;
//import com.abstratt.mdd.core.runtime.persistence.PersistenceException;
//import com.abstratt.mdd.core.runtime.persistence.PersistenceService;
//import com.abstratt.mdd.core.runtime.types.CollectionType;
//import com.abstratt.mdd.core.runtime.types.IntegerType;
//import java.util.HashMap;
//import java.util.Map;
//import junit.framework.Test;
//import junit.framework.TestSuite;
//import org.eclipse.core.runtime.CoreException;
//import org.eclipse.uml2.uml.Association;
//import org.eclipse.uml2.uml.Class;
//import org.eclipse.uml2.uml.Property;
//
//public class PersistenceServiceTests extends AbstractRuntimeTests {
//
//	private PersistenceService persistenceService;
//
//	public static Test suite() {
//		return new TestSuite(PersistenceServiceTests.class);
//	}
//
//	public PersistenceServiceTests(String name) {
//		super(name);
//	}
//
//	@Override
//	public void setUp() throws Exception {
//		super.setUp();
//		persistenceService = (PersistenceService) runtime.getService(PersistenceService.ID);
//	}
//
//	@Override
//	protected void tearDown() throws Exception {
//		super.tearDown();
//		persistenceService.zap();
//	}
//
//	public void testBasic() {
//		PersistenceService ps = (PersistenceService) runtime.getService(PersistenceService.ID);
//		assertNotNull(ps);
//	}
//
//	public void testCreateAndRefresh() throws CoreException {
//		String simpleModel = "";
//		simpleModel += "model simple;\n";
//		simpleModel += "import base;\n";
//		simpleModel += "apply mdd_extensions;\n";
//		simpleModel += "  [Persistent] class Account\n";
//		simpleModel += "    attribute balance : Integer;\n";
//		simpleModel += "  end;\n";
//		simpleModel += "end.";
//		parseAndCheck(getRepository(), simpleModel);
//		final PersistenceService persistenceService = (PersistenceService) runtime.getService(PersistenceService.ID);
//		final Class accountClass = (Class) getRepository().findNamedElement("simple::Account", IRepository.PACKAGE.getClass_(), null);
//		assertNotNull(accountClass);
//		final Property balance = accountClass.getAttribute("balance", null);
//		runtime.runInRuntime(new RuntimeRunnable() {
//
//			public void run() {
//				RuntimeObject account1 = runtime.getRuntimeClass(accountClass).newInstance();
//				account1.setValue(balance, IntegerType.fromValue(100));
//				try {
//					persistenceService.create(account1);
//				} catch (PersistenceException e) {
//					fail(e.toString());
//				}
//				account1.setValue(balance, IntegerType.fromValue(200));
//				try {
//					persistenceService.refresh(account1);
//				} catch (PersistenceException e) {
//					fail(e.toString());
//				}
//				assertEquals(IntegerType.fromValue(100), account1.getValue(balance));
//			}
//		});
//	}
//
//	public void testCreateRelatedObject1x1() throws CoreException {
//		String simpleModel = "";
//		simpleModel += "model simple;\n";
//		simpleModel += "import base;\n";
//		simpleModel += "apply mdd_extensions;\n";
//		simpleModel += "  [Persistent] class Account\n";
//		simpleModel += "    attribute balance : Integer;\n";
//		simpleModel += "  end;\n";
//		simpleModel += "  [Persistent] class Person\n";
//		simpleModel += "    attribute name : String;\n";
//		simpleModel += "  end;\n";
//		simpleModel += "  association PersonAccount\n";
//		simpleModel += "    navigable role owner : Person;\n";
//		simpleModel += "    navigable role account : Account[1];\n";
//		simpleModel += "  end;\n";
//		simpleModel += "end.";
//		parseAndCheck(getRepository(), simpleModel);
//		final PersistenceService persistenceService = (PersistenceService) runtime.getService(PersistenceService.ID);
//		final Class accountClass = (Class) getRepository().findNamedElement("simple::Account", IRepository.PACKAGE.getClass_(), null);
//		final Class personClass = (Class) getRepository().findNamedElement("simple::Person", IRepository.PACKAGE.getClass_(), null);
//		final Association personAccountAssociation = (Association) getRepository().findNamedElement("simple::PersonAccount", IRepository.PACKAGE.getAssociation(), null);
//		final Property ownerProperty = personAccountAssociation.getNavigableOwnedEnd("owner", null);
//		final Property accountProperty = personAccountAssociation.getNavigableOwnedEnd("account", null);
//		assertNotNull(accountClass);
//		assertNotNull(personClass);
//		assertNotNull(personAccountAssociation);
//		runtime.runInRuntime(new RuntimeRunnable() {
//			public void run() {
//				final RuntimeObject account1 = runtime.getRuntimeClass(accountClass).newInstance();
//				final RuntimeObject account2 = runtime.getRuntimeClass(accountClass).newInstance();
//				RuntimeObject person1 = runtime.getRuntimeClass(personClass).newInstance();
//				RuntimeObject person2 = runtime.getRuntimeClass(personClass).newInstance();
//				Map<Property, RuntimeObject> ends = new HashMap<Property, RuntimeObject>(2);
//				ends.put(ownerProperty, person1);
//				ends.put(accountProperty, account1);
//				runtime.getRuntimeAssociation(personAccountAssociation).newLink(ends);
//				ends.put(ownerProperty, person2);
//				ends.put(accountProperty, account2);
//				runtime.getRuntimeAssociation(personAccountAssociation).newLink(ends);
//				try {
//					persistenceService.create(account1);
//				} catch (PersistenceException e) {
//					fail(e.toString());
//				}
//				CollectionType allPersistedAccounts = null;
//				try {
//					allPersistedAccounts = persistenceService.findAll(runtime.getRuntimeClass(accountClass));
//				} catch (PersistenceException e) {
//					fail(e.toString());
//				}
//				assertEquals(1, allPersistedAccounts.getBackEnd().size());
//				assertTrue(allPersistedAccounts.contains(account1));
//				assertFalse(allPersistedAccounts.contains(account2));
//				CollectionType allPersistedPersons = null;
//				try {
//					allPersistedPersons = persistenceService.findAll(runtime.getRuntimeClass(personClass));
//				} catch (PersistenceException e) {
//					fail(e.toString());
//				}
//				assertEquals(1, allPersistedPersons.getBackEnd().size());
//				assertTrue(allPersistedPersons.contains(person1));
//				assertFalse(allPersistedPersons.contains(person2));
//			}
//		});
//	}
//
//	public void testFindAll() throws CoreException {
//		String simpleModel = "";
//		simpleModel += "model simple;\n";
//		simpleModel += "apply mdd_extensions;\n";
//		simpleModel += "import base;\n";
//		simpleModel += "  [Persistent] class Account\n";
//		simpleModel += "    attribute balance : Integer;\n";
//		simpleModel += "  end;\n";
//		simpleModel += "end.";
//		parseAndCheck(getRepository(), simpleModel);
//		final Class accountClass = (Class) getRepository().findNamedElement("simple::Account", IRepository.PACKAGE.getClass_(), null);
//		assertNotNull(accountClass);
//		runtime.runInRuntime(new RuntimeRunnable() {
//			public void run() {
//				RuntimeClass runtimeClass = runtime.getRuntimeClass(accountClass);
//				try {
//					assertEquals(0, persistenceService.findAll(runtimeClass).getBackEnd().size());
//				} catch (PersistenceException e) {
//					e.printStackTrace();
//					fail(e.toString());
//				}
//				try {
//					persistenceService.create(runtimeClass.newInstance());
//					assertEquals(1, persistenceService.findAll(runtimeClass).getBackEnd().size());
//				} catch (PersistenceException e) {
//					e.printStackTrace();
//					fail(e.toString());
//				}
//				try {
//					persistenceService.create(runtimeClass.newInstance());
//					assertEquals(2, persistenceService.findAll(runtimeClass).getBackEnd().size());
//				} catch (PersistenceException e) {
//					e.printStackTrace();
//					fail(e.toString());
//				}
//				try {
//					persistenceService.create(runtimeClass.newInstance());
//					assertEquals(3, persistenceService.findAll(runtimeClass).getBackEnd().size());
//				} catch (PersistenceException e) {
//					e.printStackTrace();
//					fail(e.toString());
//				}
//			}
//		});
//	}
//
//	public void testNonPersistent() throws CoreException {
//		String simpleModel = "";
//		simpleModel += "model simple;\n";
//		simpleModel += "import base;\n";
//		simpleModel += "apply mdd_extensions;\n";
//		simpleModel += "  class Account\n";
//		simpleModel += "    attribute balance : Integer;\n";
//		simpleModel += "  end;\n";
//		simpleModel += "end.";
//		parseAndCheck(getRepository(), simpleModel);
//		final Class accountClass = (Class) getRepository().findNamedElement("simple::Account", IRepository.PACKAGE.getClass_(), null);
//		runtime.runInRuntime(new RuntimeRunnable() {
//			public void run() {
//				RuntimeObject account1 = runtime.getRuntimeClass(accountClass).newInstance();
//				try {
//					persistenceService.create(account1);
//					fail("Expected to fail (non-persistent class");
//				} catch (NonPersistentClassException e) {
//					// expected failure
//				} catch (PersistenceException e) {
//					fail(e.toString());
//				}
//			}
//		});
//	}
//
// }
