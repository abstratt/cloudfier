package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.target.jse.AbstractGenerator

import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*

class SecurityHelperGenerator extends AbstractGenerator {
	
    new(IRepository repository) {
        super(repository)
    }

	def CharSequence generate() {
		val roleClasses = entities.filter[roleClass]
		'''
package util;

import java.util.List;
import java.util.ArrayList;

import userprofile.Profile;
import userprofile.ProfileService;
«roleClasses.map[
	'''
	import «it.package.name».«it.name»;
	import «it.package.name».«it.name»Service;
	'''
].join»

public class SecurityHelper {
    public static ThreadLocal<String> currentUsername = new ThreadLocal<>();
    
    public static Profile getCurrentProfile() {
        return new ProfileService().findByUsername(getCurrentUsername());
    }

    public static String getCurrentUsername() {
        return currentUsername.get();
    }

    public static void setCurrentUsername(String username) {
    	currentUsername.set(username);
    }
    
    public static List<String> getRoles(Profile user) {
		List<String> roles = new ArrayList<>();
		«roleClasses.map[ roleClass |
			'''
			if (as«roleClass.name»(user) != null) {
				roles.add(«roleClass.name».ROLE_ID);
			}
			'''
		].join()»
		return roles;
    }
    
    «roleClasses.map[ roleClass |
    '''
    public static «roleClass.name» as«roleClass.name»(Profile user) {
    	if (user == null) {
    		return null;
        }
    	«roleClass.name» asRole = new «roleClass.name»Service().findRoleByUser(user);
        return asRole;
    }
    
    public static «roleClass.name» getCurrent«roleClass.name»() {
        return as«roleClass.name»(getCurrentProfile());
    }
    '''	
    ].join»
}
		'''
	}
}
