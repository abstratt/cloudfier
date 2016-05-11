package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.util.AccessCapability
import com.abstratt.mdd.target.jse.AbstractGenerator
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Classifier
import org.eclipse.uml2.uml.Constraint
import org.eclipse.uml2.uml.NamedElement

import static extension com.abstratt.mdd.core.util.ConstraintUtils.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.ReadSelfAction
import java.util.Map
import org.eclipse.uml2.uml.Operation

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
			
		val capabilitiesPerRole = computeConstraintsPerRoleClass(allRoleClasses, accessConstraintContexts).mapValues[it.keySet]
		
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
	
	def private boolean isAllTautologies(Map<? extends Classifier, Map<AccessCapability, Constraint>> constraintsPerRole, AccessCapability requiredCapability) {
		val allTautologies = !constraintsPerRole.empty && constraintsPerRole.values.forall[it.get(requiredCapability)?.tautology]
		return allTautologies
	}
	
	def CharSequence generateSecurityContextParameter(Iterable<Class> allRoleClasses, AccessCapability requiredCapability, Iterable<NamedElement> accessConstraintContexts, CharSequence suffix) {
		val constraintsPerRole = computeConstraintsPerRoleClass(allRoleClasses, accessConstraintContexts)
		// note that here we only care for constraints explicitly provided, we ignore those roles without constraints
		if (!constraintsPerRole.isAllTautologies(requiredCapability))
			return '''@Context SecurityContext securityContext«suffix»'''
		return ''
	}


	def CharSequence generateInstanceAccessChecks(String current, AccessCapability requiredCapability,
		Iterable<Class> allRoleClasses, Iterable<NamedElement> accessConstraintContexts, CharSequence failStatement) {
		generateAccessChecks(current, requiredCapability, allRoleClasses, accessConstraintContexts, failStatement, false)
	}
	
	def CharSequence generateStaticAccessChecks(AccessCapability requiredCapability,
		Iterable<Class> allRoleClasses, Iterable<NamedElement> accessConstraintContexts, CharSequence failStatement, boolean exhaustive) {
		generateAccessChecks(null, requiredCapability, allRoleClasses, accessConstraintContexts, failStatement, true)
	}
	
	/**
	 * Generates a programmatic check for the constraints on the contexts that do define a condition.
	 * 
	 * Ignores constraints that do not define a condition.  
	 */
	def CharSequence generateAccessChecks(String current, AccessCapability requiredCapability,
		Iterable<Class> allRoleClasses, Iterable<NamedElement> accessConstraintContexts, CharSequence failStatement, boolean exhaustive) {
		val explicitConstraintsPerRole = computeConstraintsPerRoleClass(allRoleClasses, accessConstraintContexts)

		if (explicitConstraintsPerRole.empty) {
			return '/*explicitConstraintsPerRole.empty*/'
		}		
		
		val constraintsPerRole = if (exhaustive) allRoleClasses.toInvertedMap[ explicitConstraintsPerRole.getOrDefault(it, #{})] else explicitConstraintsPerRole 
		println('''«accessConstraintContexts.map[name].join(", ")» - can «requiredCapability»?''')
		constraintsPerRole.forEach[roleClass, constraints| println('''«roleClass.name» -> «constraints.keySet»''')]
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

	def Map<Classifier, Map<AccessCapability, Constraint>> computeConstraintsPerRoleClass(Iterable<Class> allRoleClasses, Iterable<NamedElement> accessConstraintContexts) {
		val accessConstraintLayers = accessConstraintContexts.map[it.findConstraints.filter[access]]
		val Map<Classifier, Map<AccessCapability, Constraint>> constraintsPerRole = newLinkedHashMap()
		accessConstraintLayers.forEach [ layer |
			layer.forEach [ constraint |
				val applicableRoles = if (constraint.accessRoles.empty) allRoleClasses else constraint.accessRoles 
				applicableRoles.forEach [ roleClass |
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
