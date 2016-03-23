package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.util.MDDExtensionUtils.AccessCapability
import java.util.Set
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.NamedElement

import static extension com.abstratt.mdd.core.util.ConstraintUtils.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*

class JAXRSAccessControlGenerator {
	/**
	 * Generates an endpoint-level access control annotation for the required capability.
	 */
	def CharSequence generateEndpointAnnotation(Set<AccessCapability> requiredCapabilities, Iterable<Class> allRoleClasses, Iterable<NamedElement> accessConstraintContexts) {
		val accessConstraintLayers = accessConstraintContexts.map[it.findConstraints.filter[access]]
		if (allRoleClasses.empty)
			// no role classes in this app
			return '''@PermitAll'''
		if (accessConstraintLayers.flatten.empty) {
			// no access constraints
			return '''@PermitAll'''
		}	
		val capabilitiesPerRole = newLinkedHashMap()
		accessConstraintLayers.forEach[ layer, layerIndex |
			layer.forEach[ constraint |
				constraint.accessRoles.forEach[roleClass |
					// remove before overriding so order is more predictable
					capabilitiesPerRole.remove(roleClass)
					val added = constraint.allowedCapabilities
					capabilitiesPerRole.put(roleClass, added)
				]
			]
		]
		val capableRoles = capabilitiesPerRole.filter[roleClass, allowedCapabilities | allowedCapabilities.containsAll(requiredCapabilities)].keySet  
        if (capableRoles.empty)
			// no roles allowed to do anything here
			return '''@DenyAll'''			
		'''
		@RolesAllowed({«capableRoles.map['''"«it.name»"'''].join(', ')»})
		'''
	}
}