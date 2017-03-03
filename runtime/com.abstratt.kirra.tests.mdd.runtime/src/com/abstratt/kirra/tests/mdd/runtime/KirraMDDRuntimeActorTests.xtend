package com.abstratt.kirra.tests.mdd.runtime

import com.abstratt.kirra.Instance
import com.abstratt.kirra.mdd.runtime.KirraActorSelector
import org.eclipse.core.runtime.CoreException

class KirraMDDRuntimeActorTests extends AbstractKirraMDDRuntimeTests {

	new(String name) {
		super(name)
	}

	static String model = '''
		package todo;
		
		role class User
		    attribute name : String;
		end;
		
		class Task
		    attribute description : String;
		    private readonly attribute creator : User := { (System#user() as User) };
		end;
		
		end.
	  ''';

	override protected setupRuntime() throws CoreException {
		super.setupRuntime()
		
		val Instance newProfile = new Instance()
		newProfile.setEntityName("Profile")
		newProfile.setEntityNamespace("userprofile")
		newProfile.setValue("username", "peter.jones")
		newProfile.setValue("password", "pass")
		kirra.createInstance(newProfile)
		
		runtime.actorSelector = new KirraActorSelector() {
			override getUserMnemonic() {
				"peter.jones"
			}
		}
		
	}

	def testCurrentUser() {
		parseAndCheck(model)
		assertEquals("peter.jones", kirra.currentUser.getValue("username"))
	}
	
	def testCurrentUser_AsRole() {
		parseAndCheck(model)
		val Instance newUser = new Instance()
		newUser.setEntityName("User")
		newUser.setEntityNamespace("todo")
		newUser.setValue("name", "Peter")
		newUser.setRelated("userProfile", kirra.currentUser)
		val Instance createdUser = kirra.createInstance(newUser)

		val Instance newTask = new Instance()
		newTask.setEntityName("Task")
		newTask.setEntityNamespace("todo")
		newTask.setValue("description", "something to do")
		val Instance createdTask = kirra.createInstance(newTask)

		assertNotNull(createdTask.getRelated("creator"))
		assertEquals(createdUser.reference, createdTask.getRelated("creator").reference)
	}

}
