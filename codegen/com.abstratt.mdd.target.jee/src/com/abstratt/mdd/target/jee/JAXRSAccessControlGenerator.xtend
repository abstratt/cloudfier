package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.util.MDDExtensionUtils.AccessCapability
import com.abstratt.mdd.target.jse.AbstractGenerator
import java.util.Set
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Classifier
import org.eclipse.uml2.uml.Constraint
import org.eclipse.uml2.uml.NamedElement

import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.ConstraintUtils.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import com.abstratt.mdd.target.jse.PlainJavaBehaviorGenerator
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.ReadSelfAction
import java.util.Map
import java.util.Optional

class JAXRSAccessControlGenerator extends AbstractGenerator {
	
    new(IRepository repository) {
        super(repository)
    }


	/**
	 * Generates an endpoint-level access control annotation for the required capability.
	 */
	def CharSequence generateEndpointAnnotation(AccessCapability requiredCapability,
		Iterable<Class> allRoleClasses, Iterable<NamedElement> accessConstraintContexts) {
		if (allRoleClasses.empty)
			// no role classes in this app
			return '''@PermitAll'''
			
		val capabilitiesPerRole = computeConstraintsPerRoleClass(accessConstraintContexts).mapValues[it.keySet]
		
		if (capabilitiesPerRole.empty) {
			// no access constraints
			return '''@PermitAll'''
		}
		val capableRoles = if (requiredCapability == null)
		    capabilitiesPerRole.keySet
		else capabilitiesPerRole.filter [roleClass, allowedCapabilities |
			allowedCapabilities.contains(requiredCapability)
		].keySet
		if (capableRoles.empty)
			// no roles allowed to do anything here
			return '''@DenyAll'''
		'''
			@RolesAllowed({«capableRoles.map['''"«it.name»"'''].join(', ')»})
		'''
	}
	
	def CharSequence generateSecurityContextParameter(AccessCapability requiredCapability, Iterable<NamedElement> accessConstraintContexts, CharSequence suffix) {
		val constraintsPerRole = computeConstraintsPerRoleClass(accessConstraintContexts)
		if (constraintsPerRole.values.exists[values.exists[!tautology]])
			return '''@Context SecurityContext securityContext«suffix»'''
		return ''
	}

	/**
	 * Generates a programmatic check for the constraints on the contexts that do define a condition.
	 * 
	 * Ignores constraints that do not define a condition.  
	 */
	def CharSequence generateAccessChecks(String current, AccessCapability requiredCapability,
		Iterable<Class> allRoleClasses, Iterable<NamedElement> accessConstraintContexts, CharSequence failStatement) {
		val constraintsPerRole = computeConstraintsPerRoleClass(accessConstraintContexts)
		val checksPerRole = constraintsPerRole.entrySet.filter[value.keySet.contains(requiredCapability)].map[entry |
			val role = entry.key
			val constraint = entry.value.get(requiredCapability)
			return if (constraint.tautology) null else generateAccessChecksForRole(current, role, constraint, failStatement)
		].filter[it != null]
		if (checksPerRole.empty)
			return ''
			
		'''
		UserProfile user = new UserProfileService().findByUsername(securityContext.getUserPrincipal().getName());
		«checksPerRole.join()»
		'''
	}
	
	def CharSequence generateAccessChecksForRole(String current, Classifier classifier, Constraint constraint, CharSequence failStatement) {
		val castUser = '''as«classifier.name»'''	
		val condition = new PlainJavaBehaviorGenerator(repository) {
			override generateSystemUserCall(CallOperationAction action) {
				castUser
			}
			override generateReadSelfAction(ReadSelfAction action) {
				current
			}
		}.generatePredicate(constraint, true)
		'''
		if (securityContext.isUserInRole("«classifier.name»")) {
			«classifier.name» «castUser» = new «classifier.name»Service().findByUser(user);
			if («castUser» == null || «condition») {
				«failStatement»
		    }
		} 
		'''
	}

	def Map<Classifier, Map<AccessCapability, Constraint>> computeConstraintsPerRoleClass(Iterable<NamedElement> accessConstraintContexts) {
		val accessConstraintLayers = accessConstraintContexts.map[it.findConstraints.filter[access]]
		val Map<Classifier, Map<AccessCapability, Constraint>> constraintsPerRole = newLinkedHashMap()
		accessConstraintLayers.forEach [ layer |
			layer.forEach [ constraint |
				constraint.accessRoles.forEach [ roleClass |
					constraintsPerRole.remove(roleClass)
					val constraintsByCapabilities = newLinkedHashMap()
					constraintsPerRole.put(roleClass, constraintsByCapabilities)
					constraint.allowedCapabilities.forEach [ capability |
						constraintsByCapabilities.put(capability, constraint)
						capability.getImplied(false).forEach[ implied |
							constraintsByCapabilities.putIfAbsent(implied, constraint)
						]
					]
				]
			]
		]
		return constraintsPerRole
	}
}
