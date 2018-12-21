package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.util.AccessCapability
import com.abstratt.mdd.target.jse.AbstractGenerator
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Classifier
import org.eclipse.uml2.uml.NamedElement
import org.eclipse.uml2.uml.Operation

import static extension com.abstratt.mdd.core.util.AccessControlUtils.*
import static extension com.abstratt.mdd.core.util.ConstraintUtils.*
import java.util.Collection

class JAXRSAccessControlGenerator extends AbstractGenerator {
	
    new(IRepository repository) {
        super(repository)
    }


	/**
	 * Generates an endpoint-level access control annotation for the required capability.
	 */
	def CharSequence generateEndpointAnnotation(AccessCapability requiredCapability,
		Collection<Class> allRoleClasses, Collection<NamedElement> accessConstraintContexts) {
		if (allRoleClasses.empty)
			// no role classes in this app
			return '''@PermitAll'''
			
		val capabilitiesPerRole = computeConstraintsPerRoleClass(
		    allRoleClasses,
		    accessConstraintContexts
		).mapValues[it.keySet]
		
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
			@RolesAllowed({«capableRoles.filter[it != null].map['''"«it.name»"'''].join(', ')»})
		'''
	}
	
	def CharSequence generateSecurityContextParameter(Collection<Class> allRoleClasses, AccessCapability requiredCapability, Collection<NamedElement> accessConstraintContexts, CharSequence suffix) {
		val constraintsPerRole = computeConstraintsPerRoleClass(allRoleClasses, accessConstraintContexts)
		// note that here we only care for constraints explicitly provided, we ignore those roles without constraints
		if (!constraintsPerRole.isAllTautologies(requiredCapability))
			return '''@Context SecurityContext securityContext«suffix»'''
		return ''
	}


	def CharSequence generateInstanceAccessChecks(String current, AccessCapability requiredCapability,
		Collection<Class> allRoleClasses, Collection<NamedElement> accessConstraintContexts, CharSequence failStatement) {
		generateAccessChecks(current, requiredCapability, allRoleClasses, accessConstraintContexts, failStatement, false)
	}
	
	def CharSequence generateStaticAccessChecks(AccessCapability requiredCapability,
		Collection<Class> allRoleClasses, Collection<NamedElement> accessConstraintContexts, CharSequence failStatement, boolean exhaustive) {
		generateAccessChecks(null, requiredCapability, allRoleClasses, accessConstraintContexts, failStatement, true)
	}
	
	/**
	 * Generates a programmatic check for the constraints on the contexts that do define a condition.
	 * 
	 * Ignores constraints that do not define a condition.  
	 */
	def CharSequence generateAccessChecks(String current, AccessCapability requiredCapability,
		Collection<Class> allRoleClasses, Collection<NamedElement> accessConstraintContexts, CharSequence failStatement, boolean exhaustive) {
		val explicitConstraintsPerRole = computeConstraintsPerRoleClass(allRoleClasses, accessConstraintContexts)

		if (explicitConstraintsPerRole.empty) {
			return '/*explicitConstraintsPerRole.empty*/'
		}		
		
		val constraintsPerRole = if (exhaustive) allRoleClasses.toInvertedMap[ explicitConstraintsPerRole.getOrDefault(it, #{})] else explicitConstraintsPerRole 
		println('''«accessConstraintContexts.map[name].join(", ")» - can «requiredCapability»?''')
		constraintsPerRole.filter[k, v | k != null].forEach[roleClass, constraints| println('''«roleClass.name» -> «constraints.keySet»''')]
		if (constraintsPerRole.isAllTautologies(requiredCapability))
			return '''/*«requiredCapability»: allTautologies*/'''
		
		
		val checksPerRole = constraintsPerRole.entrySet.filter[value.keySet.contains(requiredCapability)].map[entry |
			val role = entry.key
			val constraint = entry.value.get(requiredCapability)
			return if (constraint.tautology) generateFullAccessForRole(role) else generateInstanceAccessChecksForRole(accessConstraintContexts, current, role, requiredCapability, failStatement)
		]
			
		'''
		«IF !checksPerRole.empty»«checksPerRole.map[toString().trim()].join(' else ').trim» else {
			«failStatement»
		}«ELSE»
		/* nothing here */
		«ENDIF»
		'''
	}
	
	def CharSequence generateInstanceAccessChecksForRole(Iterable<NamedElement> accessConstraintContexts, String current, Classifier roleClass, AccessCapability capability, CharSequence failStatement) {
		val castUser = '''as«roleClass.name»'''
		val entity = accessConstraintContexts.filter(Class).head
		val operation = accessConstraintContexts.filter(Operation).head
//		val condition = new JPABehaviorGenerator(repository) {
//			override generateSystemUserCall(CallOperationAction action) {
//				castUser
//			}
//			override generateReadSelfAction(ReadSelfAction action) {
//				current
//			}
//		}.generatePredicate(constraint, true)
		'''
		if (securityContext.isUserInRole("«roleClass.name»")) {
			«roleClass.name» «castUser» = SecurityHelper.getCurrent«roleClass.name»();
			if (!«entity.name».Permissions.«IF capability == AccessCapability.Call»is«operation.name.toFirstUpper»AllowedFor«ELSE»can«capability.name()»«ENDIF»(«castUser», «current»)) {
				«failStatement»
		    }
		} 
		'''
	}
	
	def CharSequence generateFullAccessForRole(Classifier classifier) {
		'''
		if (securityContext.isUserInRole("«classifier.name»")) {
			// no further checks
		}
		'''
	}
}
