package com.abstratt.mdd.core.tests.runtime

import com.abstratt.mdd.core.runtime.Runtime
import com.abstratt.mdd.core.runtime.types.StringType

class RuntimeTests extends AbstractRuntimeTests {
	
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
	
	def void testCurrentUser() {
		parseAndCheck(model)
		
		val profile = runtime.getRuntimeClass("userprofile::Profile").newInstance
		writeAttribute(profile, "username", new StringType("peter.jones"))
		writeAttribute(profile, "email", new StringType("peter@abstratt.com"))
		writeAttribute(profile, "password", new StringType("pass"))
		runtime.setActorSelector([Runtime runtime | profile])
		runtime.saveContext(false)
		
		assertNotNull(runtime.currentActor)
		assertEquals(new StringType("peter.jones"), readAttribute(runtime.currentActor, "username"))
	}
	
	def void testUserAsRole() {
		parseAndCheck(model)
		val profile1 = runtime.getRuntimeClass("userprofile::Profile").newInstance
		writeAttribute(profile1, "username", new StringType("john.ford"))
		writeAttribute(profile1, "email", new StringType("john@abstratt.com"))
		writeAttribute(profile1, "password", new StringType("pass"))
		
		val profile2 = runtime.getRuntimeClass("userprofile::Profile").newInstance
		writeAttribute(profile2, "username", new StringType("john.ford"))
		writeAttribute(profile2, "email", new StringType("john@abstratt.com"))
		writeAttribute(profile2, "password", new StringType("pass"))
		
		val user = runtime.getRuntimeClass("todo::User").newInstance
		writeAttribute(user, "name", new StringType("John"))
		writeAttribute(user, "user", profile2)
		
		runtime.saveContext(false)
		
		val roleClass = getClass("todo::User")

		val roleInstance = runtime.getRoleForActor(profile2, roleClass)
		assertNotNull(roleInstance)
		assertEquals(user.objectId, roleInstance.objectId)
	}
}