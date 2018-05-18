package com.abstratt.kirra.tests.mdd.runtime

import com.abstratt.kirra.Instance
import com.abstratt.kirra.mdd.runtime.KirraActorSelector
import com.abstratt.mdd.core.util.AccessCapability
import org.eclipse.core.runtime.CoreException
import com.abstratt.kirra.TypeRef
import com.abstratt.kirra.TypeRef.TypeKind
import com.abstratt.kirra.InstanceManagement

class KirraMDDRuntimeActorTests extends AbstractKirraMDDRuntimeTests {

    new(String name) {
        super(name)
    }

    static String model = '''
        package todo;

        abstract role class AbstractUser
            attribute name : String;
        end;
        
        role class Admin specializes AbstractUser
        end;
        
        role class User specializes AbstractUser
            allow extent;
        end;
        
        role class AdvancedUser specializes User
        end;
        
        
        /* A role that should be allowed to do anything */
        role class PowerlessUser specializes AbstractUser
            allow extent;
        end; 
        
        class Bug
            allow User read, delete { self.creator == (System#user() as User) };
            attribute name : String;
            attribute creator : User;
        end;
        
        class Task
            allow Admin create, extent, delete, update;
            allow User create, read;
            allow User update { self.creator == (System#user() as User) };
            attribute description : String;
            private readonly attribute creator : User := { (System#user() as User) };
            
            operation assignTo(another : User);
            
            operation close()
                allow User call { self.creator == (System#user() as User) };
                
            static query openTasks() : Task[*];
                
            static operation deleteTasksFor(creator : User)
                allow Admin call;
        end;
        
        class AnonymousComment
            allow User extent, read;
            allow Admin all;
            allow create, read, call;
            attribute comment : Memo;
            operation reply(text : Memo);
            static operation newComment(text : Memo) : AnonymousComment;
        end;
        
        class AnonymousPost
            attribute text : Memo;
            operation publish();
            static operation writeAndPublish(text : Memo) : AnonymousPost;
        end;
    end.
      ''';

    override protected setupRuntime() throws CoreException {
        super.setupRuntime()
        
        val Instance newProfile0 = new Instance()
        newProfile0.setEntityName("UserProfile")
        newProfile0.setEntityNamespace("userprofile")
        newProfile0.setValue("username", "admin1@abstratt.com")
        newProfile0.setValue("password", "pass")
        val createdProfile0 = kirra.createInstance(newProfile0)
        
        val Instance newUser0 = new Instance()
        newUser0.setEntityName("Admin")
        newUser0.setEntityNamespace("todo")
        newUser0.setValue("name", "Administrator")
        newUser0.setRelated("userProfile", createdProfile0)
        val createdUser0 = kirra.createInstance(newUser0)
        
        val Instance newProfile1 = new Instance()
        newProfile1.setEntityName("UserProfile")
        newProfile1.setEntityNamespace("userprofile")
        newProfile1.setValue("username", "peter.jones@abstratt.com")
        newProfile1.setValue("password", "pass")
        val createdProfile1 = kirra.createInstance(newProfile1)
        
        val Instance newUser1 = new Instance()
        newUser1.setEntityName("User")
        newUser1.setEntityNamespace("todo")
        newUser1.setValue("name", "Peter")
        newUser1.setRelated("userProfile", createdProfile1)
        val createdUser1 = kirra.createInstance(newUser1)
        
        val Instance newProfile2 = new Instance()
        newProfile2.setEntityName("UserProfile")
        newProfile2.setEntityNamespace("userprofile")
        newProfile2.setValue("username", "john.ford@abstratt.com")
        newProfile2.setValue("password", "pass")
        val createdProfile2 = kirra.createInstance(newProfile2)
        
        val Instance newUser2 = new Instance()
        newUser2.setEntityName("User")
        newUser2.setEntityNamespace("todo")
        newUser2.setValue("name", "John")
        newUser2.setRelated("userProfile", createdProfile2)
        val createdUser2 = kirra.createInstance(newUser2)
        
        val Instance newProfile3 = new Instance()
        newProfile3.setEntityName("UserProfile")
        newProfile3.setEntityNamespace("userprofile")
        newProfile3.setValue("username", "mary.jones@abstratt.com")
        newProfile3.setValue("password", "pass")
        val createdProfile3 = kirra.createInstance(newProfile3)
        
        val Instance newUser3 = new Instance()
        newUser3.setEntityName("AdvancedUser")
        newUser3.setEntityNamespace("todo")
        newUser3.setValue("name", "Mary")
        newUser3.setRelated("userProfile", createdProfile3)
        val createdUser3 = kirra.createInstance(newUser3)
        
        val Instance newProfile4 = new Instance()
        newProfile4.setEntityName("UserProfile")
        newProfile4.setEntityNamespace("userprofile")
        newProfile4.setValue("username", "powerless@abstratt.com")
        newProfile4.setValue("password", "pass")
        val createdProfile4 = kirra.createInstance(newProfile4)
        
        val Instance newUser4 = new Instance()
        newUser4.setEntityName("PowerlessUser")
        newUser4.setEntityNamespace("todo")
        newUser4.setValue("name", "Powerless User")
        newUser4.setRelated("userProfile", createdProfile4)
        val createdUser4 = kirra.createInstance(newUser4)
        
        loginAs(newProfile1.getValue("username") as String)
        
    }

    def testCurrentUser() {
        parseAndCheck(model)
        assertEquals("peter.jones@abstratt.com", kirra.currentUser.getValue("username"))
    }
    
    def testCurrentUser_Roles() {
        parseAndCheck(model)
        
        val actualRoles = kirra.currentUserRoles.map[it.reference]
        val expectedUserRole = findUserByName("Peter", "User")
        
        assertEquals(#[expectedUserRole.reference], actualRoles)
    }
    
    def testCurrentUser_Roles_AdvancedUser() {
        parseAndCheck(model)
        
        loginAs("mary.jones")
        val expectedUserRole = findUserByName("Mary", "AdvancedUser")
        val actualRoles = kirra.currentUserRoles.map[it.reference]
        
        assertEquals(#[expectedUserRole.reference], actualRoles)
    }
    
    def testCurrentUser_AsRole() {
        parseAndCheck(model)

        val Instance newTask = new Instance()
        newTask.setEntityName("Task")
        newTask.setEntityNamespace("todo")
        newTask.setValue("description", "something to do")
        val Instance createdTask = kirra.createInstance(newTask)

        val expectedUserRole = findUserByName("Peter", "User")
        assertNotNull(expectedUserRole)
        assertNotNull(createdTask.getRelated("creator"))
        assertEquals(expectedUserRole.reference, createdTask.getRelated("creator").reference)
    }

    protected def Instance findUserByName(String name, String entityName) {
        kirra.filterInstances(#{"name" -> #[name as Object]}, "todo", entityName, InstanceManagement.DataProfile.Full).head
    }
    
    def testInstanceCRUDCapabilities_User() {
        parseAndCheck(model)

        val Instance newTask = new Instance()
        newTask.setEntityName("Task")
        newTask.setEntityNamespace("todo")
        newTask.setValue("description", "something to do")
        val Instance createdTask = kirra.createInstance(newTask)
        
        assertNotNull(createdTask.getRelated("creator"))
        
        val instanceCapabilities = kirra.getInstanceCapabilities(newTask.typeRef, createdTask.objectId)

        assertEquals(#{AccessCapability.Read.name(), AccessCapability.Update.name()}, instanceCapabilities.instance.toSet)
        
        loginAs("john.ford")
        val instanceCapabilities2 = kirra.getInstanceCapabilities(newTask.typeRef, createdTask.objectId)
        
        assertEquals(#{AccessCapability.Read.name() }, instanceCapabilities2.instance.toSet)
    }
    
    def testInstanceCRUDCapabilities_Anonymous() {
        parseAndCheck(model)

        val aUser = findUserByName("Peter", "User") 
        
        loginAs(null)
        
        val instanceCapabilities = kirra.getInstanceCapabilities(aUser.typeRef, aUser.objectId)

        // TODO-RC not sure what to expect here, used to be just #{Read}
        assertEquals(#{AccessCapability.Read, AccessCapability.Delete, AccessCapability.Update}.map[name()].toSet, instanceCapabilities.instance.toSet)
    }
    
    def testEntityCRUDCapabilities_Anonymous() {
        parseAndCheck(model)

        loginAs(null)
        
        val entityCapabilities = kirra.getEntityCapabilities(new TypeRef("todo::User", TypeKind.Entity))

        assertEquals(#{AccessCapability.List.name()}, entityCapabilities.entity.toSet)
    }    
    
    def testInstanceActionCapabilities_User() {
        parseAndCheck(model)

        val Instance newTask = new Instance()
        newTask.setEntityName("Task")
        newTask.setEntityNamespace("todo")
        newTask.setValue("description", "something to do")
        val Instance createdTask = kirra.createInstance(newTask)
        
        assertNotNull(createdTask.getRelated("creator"))
        
        val instanceCapabilities = kirra.getInstanceCapabilities(newTask.typeRef, createdTask.objectId)
        assertEquals(#[AccessCapability.Call.name()], instanceCapabilities.actions.get('close'))
        assertEquals(#[AccessCapability.Call.name()], instanceCapabilities.actions.get('assignTo'))
        
        loginAs("john.ford")
        
        val instanceCapabilities2 = kirra.getInstanceCapabilities(newTask.typeRef, createdTask.objectId)
        assertEquals(#[], instanceCapabilities2.actions.get('close'))
        assertEquals(#[AccessCapability.Call.name()], instanceCapabilities2.actions.get('assignTo'))
    }
    
    def testInstanceCRUDCapabilities_Admin() {
        parseAndCheck(model)

        val Instance newTask = new Instance()
        newTask.setEntityName("Task")
        newTask.setEntityNamespace("todo")
        newTask.setValue("description", "something to do")
        val Instance createdTask = kirra.createInstance(newTask)
        
        loginAs("admin")
        
        val instanceCapabilities = kirra.getInstanceCapabilities(newTask.typeRef, createdTask.objectId)

        assertEquals(#{AccessCapability.Read.name(), AccessCapability.Update.name(), AccessCapability.Delete.name()}, instanceCapabilities.instance.toSet)
    }
    
    def testInstanceActionCapabilities_Anonymous() {
        parseAndCheck(model)
        
        loginAs(null)
        
        val Instance newComment = new Instance()
        newComment.setEntityName("AnonymousComment")
        newComment.setEntityNamespace("todo")
        newComment.setValue("comment", "something to say")
        val Instance createdComment = kirra.createInstance(newComment)
        
        val instanceCapabilities = kirra.getInstanceCapabilities(newComment.typeRef, createdComment.objectId)
        assertEquals(#[AccessCapability.Call.name()], instanceCapabilities.actions.get('reply'))
    }
    
    def testInstanceActionCapabilities_Anonymous_NoConstraints() {
        parseAndCheck(model)
        
        loginAs(null)
        
        val Instance newPost = new Instance()
        newPost.setEntityName("AnonymousPost")
        newPost.setEntityNamespace("todo")
        newPost.setValue("text", "something to say")
        val Instance createdPost = kirra.createInstance(newPost)
        
        val instanceCapabilities = kirra.getInstanceCapabilities(newPost.typeRef, createdPost.objectId)
        assertEquals(#[AccessCapability.Call.name()], instanceCapabilities.actions.get('publish'))
    }

    
    protected def void loginAs(String username) {
        runtime.actorSelector = new KirraActorSelector() {
            override getUserMnemonic() {
                username
            }
        }
    }
    
    
    def testEntityCapabilities_User() {
        parseAndCheck(model)

        val entityCapabilities = kirra.getEntityCapabilities(new TypeRef("todo", "Task", TypeKind.Entity))

        assertEquals(#{ AccessCapability.Create.name() }, entityCapabilities.entity.toSet)
        assertEquals(#[], entityCapabilities.actions.get('deleteTasksFor'))
        
    }    

    def testEntityCapabilities_PowerlessUser() {
        parseAndCheck(model)

        loginAs("powerless")
        
        val taskEntityCapabilities = kirra.getEntityCapabilities(new TypeRef("todo", "Task", TypeKind.Entity))
        assertEquals(#[], taskEntityCapabilities.entity)
        taskEntityCapabilities.actions.forEach [ name, capabilities |
            assertEquals(#[], capabilities)
        ]
        
        val bugEntityCapabilities = kirra.getEntityCapabilities(new TypeRef("todo", "Bug", TypeKind.Entity))
        assertEquals(#[], taskEntityCapabilities.entity)
        taskEntityCapabilities.actions.forEach [ name, capabilities |
            assertEquals(#[], capabilities)
        ]
    }    

    
    def testEntityCapabilities_Admin() {
        parseAndCheck(model)

        loginAs("admin")
        
        val entityCapabilities = kirra.getEntityCapabilities(new TypeRef("todo", "Task", TypeKind.Entity))

        assertEquals(#{AccessCapability.Create.name(), AccessCapability.List.name() }, entityCapabilities.entity.toSet)
        
        assertEquals(#[AccessCapability.Call.name()], entityCapabilities.actions.get('deleteTasksFor'))
    }

    def testEntityCapabilities_Anonymous() {
        parseAndCheck(model)

        loginAs(null)
        
        val entityCapabilities = kirra.getEntityCapabilities(new TypeRef("todo", "AnonymousComment", TypeKind.Entity))

        assertEquals(#{AccessCapability.Create.name() }.toSet, entityCapabilities.entity.toSet)
        assertEquals(#[AccessCapability.Call.name()], entityCapabilities.actions.get('newComment'))
        
        loginAs("peter.jones");
        
        val entityCapabilities2 = kirra.getEntityCapabilities(new TypeRef("todo", "AnonymousComment", TypeKind.Entity))

        assertEquals(#{AccessCapability.Create.name(), AccessCapability.List.name() }, entityCapabilities2.entity.toSet)
        assertEquals(#[AccessCapability.Call.name()], entityCapabilities2.actions.get('newComment'))
    }
    
    def testEntityCapabilities_Anonymous_RestrictedEntity() {
        parseAndCheck(model)

        loginAs(null)
        
        val entityCapabilities = kirra.getEntityCapabilities(new TypeRef("todo", "Task", TypeKind.Entity))

        assertEquals(#{}, entityCapabilities.entity.toSet)
        assertEquals(#[], entityCapabilities.actions.get('deleteTasksFor'))
    }    
    
    def testEntityCapabilities_Anonymous_NoConstraints() {
        parseAndCheck(model)

        loginAs(null)
        
        val entityCapabilities = kirra.getEntityCapabilities(new TypeRef("todo", "AnonymousPost", TypeKind.Entity))

        assertEquals(#{AccessCapability.Create.name(), AccessCapability.List.name() }, entityCapabilities.entity.toSet)
        assertEquals(#[AccessCapability.Call.name()], entityCapabilities.actions.get('writeAndPublish'))
        
        loginAs("peter.jones");
        
        val entityCapabilities2 = kirra.getEntityCapabilities(new TypeRef("todo", "AnonymousPost", TypeKind.Entity))

        assertEquals(#{AccessCapability.Create.name(), AccessCapability.List.name() }, entityCapabilities2.entity.toSet)
        assertEquals(#[AccessCapability.Call.name()], entityCapabilities2.actions.get('writeAndPublish'))
        
    }
    
}
