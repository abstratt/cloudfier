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

import userprofile.UserProfile;
import userprofile.UserProfileService;
«roleClasses.map[
	'''
	import «it.package.name».«it.name»;
	import «it.package.name».«it.name»Service;
	'''
].join»

public class SecurityHelper {
    public static ThreadLocal<String> currentUsername = new ThreadLocal<>();
    
    public static UserProfile getCurrentProfile() {
        return new UserProfileService().findByUsername(getCurrentUsername());
    }

    public static String getCurrentUsername() {
        return currentUsername.get();
    }

    public static void setCurrentUsername(String username) {
    	currentUsername.set(username);
    }
    
    public static List<String> getRoles(UserProfile user) {
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
    public static «roleClass.name» as«roleClass.name»(UserProfile userProfile) {
    	if (userProfile == null) {
    		return null;
        }
    	«roleClass.name» asRole = new «roleClass.name»Service().findRoleAs«roleClass.name»ByUserProfile(userProfile);
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
