package com.abstratt.mdd.target.jse

import com.abstratt.kirra.mdd.core.KirraHelper
import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.util.MDDUtil
import com.abstratt.mdd.target.base.DelegatingBehaviorGenerator
import com.abstratt.mdd.target.base.IBehaviorGenerator
import com.abstratt.mdd.target.base.IBehaviorGenerator.SimpleContext
import java.util.List
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.query.conditions.eobjects.EObjectCondition
import org.eclipse.uml2.uml.Action
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Classifier
import org.eclipse.uml2.uml.Constraint
import org.eclipse.uml2.uml.Feature
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.Parameter
import org.eclipse.uml2.uml.ParameterDirectionKind
import org.eclipse.uml2.uml.Port
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.ReadSelfAction
import org.eclipse.uml2.uml.ReadStructuralFeatureAction
import org.eclipse.uml2.uml.SendSignalAction
import org.eclipse.uml2.uml.Signal
import org.eclipse.uml2.uml.StateMachine
import org.eclipse.uml2.uml.UMLPackage
import org.eclipse.uml2.uml.VisibilityKind

import static com.abstratt.mdd.target.jse.PlainJavaGenerator.*

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.ConstraintUtils.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import static extension com.abstratt.mdd.core.util.StateMachineUtils.*
import static extension com.abstratt.mdd.core.util.AccessControlUtils.*
import static extension com.abstratt.mdd.target.jse.KirraToJavaHelper.*
import com.abstratt.mdd.core.util.AccessCapability
import org.eclipse.uml2.uml.StructuredActivityNode
import com.abstratt.mdd.core.util.MDDExtensionUtils

class PlainEntityGenerator extends BehaviorlessClassGenerator {

    protected String applicationName

    protected StateMachineGenerator stateMachineGenerator
    
    IBehaviorGenerator behaviorGenerator
    
    new(IRepository repository) {
        super(repository)
        behaviorGenerator = createBehaviorGenerator()
        stateMachineGenerator = new StateMachineGenerator(repository, createBehaviorGenerator())
        applicationName = KirraHelper.getApplicationName(repository)
    }
    
    def IBehaviorGenerator createBehaviorGenerator() {
        return new PlainEntityBehaviorGenerator(repository) 
    }
    
    def generateProviders(Class entity) {
        val findOperationCalls = [ Activity activity | 
            activity.bodyNode
                .findMatchingActions(UMLPackage.Literals.CALL_OPERATION_ACTION)
                .map[callOperation | (callOperation  as CallOperationAction).operation ]
        ]
        
        val allActivities = MDDUtil.findAllFrom(new EObjectCondition() {
            override isSatisfied(EObject eObject) {
                return UMLPackage.Literals.ACTIVITY.isInstance(eObject)
            }
        }, #{entity})
        
        val providers = allActivities.map[findOperationCalls.apply(it as Activity)]
            .flatten
            .filter[providerOperation]
            .map[class_]
            .filter[it.isProviderFor(entity)]
            .toSet
        
        if (!providers.empty)
        '''
        /*************************** PROVIDERS ***************************/
        
        «generateMany(providers, [generateProvider])»
        '''    
        
    }
    
    def boolean isProviderFor(Class candidate, Class entity) {
    	true
    }
    
    override generateAction(Action action) {
        behaviorGenerator.generateAction(action)
    }
    
    override CharSequence generateActivity(Activity a) {
        behaviorGenerator.generateActivity(a)
    }
    
    override generateActivityAsExpression(Activity toGenerate, boolean asClosure, List<Parameter> parameters) {
        behaviorGenerator.generateActivityAsExpression(toGenerate, asClosure, parameters)
    }
    
    def findAnonymousDataTypes(Class context) {
        val allActivities = MDDUtil.findAllFrom(new EObjectCondition() {
            override isSatisfied(EObject eObject) {
                return UMLPackage.Literals.ACTIVITY.isInstance(eObject)
            }
        }, #{context})
        allActivities.map[(it as Activity).anonymousDataTypes].flatten.toSet
    }
    
    def generateAnonymousDataTypes(Class context) {
        context.findAnonymousDataTypes.map[generateDataType(false)].join
    }
    
    def Iterable<Activity> findActivities(Iterable<Operation> operations) {
        operations.filter[op | op.activity != null].map[op | 
            op.activity
        ]
    }
    
    def generateStandardImports() {
        '''
        import java.util.*;
        import java.util.stream.*;
        import java.util.function.*;
        import java.io.Serializable;
        '''
    }
    
    def generateEntity(Class entity) {
        val actionOperations = entity.actions.filter[!static && !isInherited(entity)]
        val relationships = entity.entityRelationships.filter[!relationshipDerived && navigable && !isInherited(entity)]
        val notOwned = entity.entityRelationships.filter[!relationshipDerived && navigable && !isInherited(entity)]
        val attributeInvariants = entity.properties.filter[!propertyDerived && !isInherited(entity)].map[findInvariantConstraints].flatten
        val derivedAttributes = entity.properties.filter[propertyDerived && !isInherited(entity)]
        val attributes = entity.properties.filter[!propertyDerived && !sequence && !isInherited(entity)]
        val sequenceAttributes = entity.properties.filter[sequence && !isInherited(entity)]
        val derivedRelationships = entity.entityRelationships.filter[relationshipDerived && !isInherited(entity)]
        val privateOperations = entity.operations.filter[!action && !finder && !isInherited(entity)]
        val stateProperty = entity.findStateProperties.head
        val stateMachine = stateProperty?.type as StateMachine
        val ports = entity.getAllAttributes().filter(typeof(Port)).filter[!isInherited(entity)]
        val signals = findTriggerableSignals(actionOperations)
        
        '''
            package «entity.packagePrefix»;
            
            «generateStandardImports»
            «entity.generateImports»
            
            «entity.generateComment»
            «entity.generateEntityAnnotations»public «IF entity.abstract»abstract «ENDIF»class «entity.name» «entity.generateEntityGenealogy»{
            	
                «entity.generatePrefix»
                
                «entity.generateAnonymousDataTypes»
                
                «entity.generateEntityId»
                «entity.generateProviders»
                
                «IF !ports.empty»
                    /*************************** PORTS ***************************/
                    
                    «generateMany(ports, [generatePort])»
                «ENDIF»
                «IF !signals.empty»
                    /*************************** SIGNALS ***************************/

                    «generateMany(signals, [generateSignal])»
                «ENDIF»
                «IF !attributes.empty»
                    /*************************** ATTRIBUTES ***************************/

                    «generateMany(attributes, [generateAttribute])»
        		«ENDIF»
                «IF !sequenceAttributes.empty»
                    /*************************** SEQUENCE ***************************/

                    «generateMany(sequenceAttributes, [generateSequenceAttribute])»
        		«ENDIF»        		
                «IF !relationships.empty»
                    /*************************** RELATIONSHIPS ***************************/
                    
                    «generateMany(relationships, [generateRelationship(entity)])»
                «ENDIF»
                «IF !actionOperations.empty»
                    /*************************** ACTIONS ***************************/
                    
                    «generateMany(actionOperations, [generateOperation])»
                    «generateMany(actionOperations, [generateActionEnablement])»
                «ENDIF»
                «IF !derivedAttributes.empty»
                    /*************************** DERIVED PROPERTIES ****************/
                    
                    «generateMany(derivedAttributes, [generateDerivedAttribute])»
                «ENDIF»
                «IF !derivedRelationships.empty»
                    /*************************** DERIVED RELATIONSHIPS ****************/
                    
                    «generateMany(derivedRelationships, [generateDerivedRelationship])»
                «ENDIF»
                «IF !privateOperations.empty»
                    /*************************** PRIVATE OPS ***********************/
                    
                    «generateMany(privateOperations, [generatePrivateOperation])»
                «ENDIF»
                «IF stateMachine != null»
                    /*************************** STATE MACHINE ********************/
                    
                    «stateMachineGenerator.generateStateMachine(stateMachine, entity)»
                «ENDIF»
                /*************************** PERMISSIONS ********************/
                «generatePermissions(entity)»
                
                «entity.generateSuffix»
            }
        '''
    }
    
    def CharSequence generateSuffix(Class entity) {
        '''
        private final static Collection<«entity.name»> allInstances = new LinkedList<«entity.name»>();
        
        public static Collection<«entity.name»> extent() {
            return Collections.unmodifiableCollection(allInstances);
        }
        
        public static «entity.name» objectCreated(«entity.name» created) {
            allInstances.add(created);
            return created;
        }
        
        public static «entity.name» objectDestroyed(«entity.name» destroyed) {
            allInstances.remove(destroyed);
            return destroyed;
        }
        
        public static void zap() {
            allInstances.clear();                    
        }
        '''
    }
    
    def CharSequence generatePrefix(Class class1) {
        ''
    }
    
    def generateEntityGenealogy(Class entityClass) {
    	val ancestors = entityClass.superClasses.filter[entity]
    	if (!ancestors.empty)
        '''extends «ancestors.map[toJavaType].join(', ')» '''
    }
    
    def generateEntityAnnotations(Class class1) {
        ''
    }
    
    def generateEntityId(Class entity) {
        '''
        public Integer getId() {
            return System.identityHashCode(this);
        }
        '''
    }
    
    def Iterable<Signal> findTriggerableSignals(Iterable<Operation> operations) {
        operations.filter[op | op.activity != null].map[op | 
            op.activity.bodyNode
                .findMatchingActions(UMLPackage.Literals.SEND_SIGNAL_ACTION)
                .map[sendSignal | 
                    (sendSignal as SendSignalAction).signal
                ]
        ].flatten
    }
    
    def generateProvider(Class provider) {
        '''
        private «provider.toJavaType»Service «provider.name.toFirstLower»Service = new «provider.toJavaType»Service();
        '''
    }
    
    def generatePort(Port port) {
        '''
        «port.requireds.head.toJavaType» «port.name.toFirstLower»;
        '''
    }
    
    def generateProviderReference(Classifier context, Classifier provider) {
        '''«provider.name.toFirstLower»Service'''
    }

    def generatePrivateOperation(Operation operation) {
        // for now, we do not honor visibility (it is really meant for the API)
        operation.generateOperation(VisibilityKind.PUBLIC_LITERAL)
    }

    def generateAttributeInvariants(Property attribute) {
        val constraints = attribute.findInvariantConstraints
        constraints.map[generateAttributeInvariant(attribute, it)].join()
    }
    
    def generateAttributeInvariant(Property attribute, Constraint constraint) {
        val delegate = new DelegatingBehaviorGenerator(behaviorGenerator) {
            
            override generateAction(Action action, boolean delegate) {
                if (action instanceof ReadStructuralFeatureAction && ((action as ReadStructuralFeatureAction).structuralFeature == attribute))
                    '''new«attribute.name.toFirstUpper»'''      
                else          
                    super.generateAction(action, false)
            }
            
        }
        val newContext = new SimpleContext(behaviorGenerator.context.generateCurrentReference.toString, delegate)
        behaviorGenerator.runInContext(newContext, [
	        '''
	        if («generatePredicate(constraint, true)») {
	            throw new «IF constraint.name?.length > 0»«constraint.name»Exception()«ELSE»ConstraintViolationException("«KirraHelper.getDescription(constraint).toLowerCase.toFirstUpper»")«ENDIF»;
	        }
	        '''
        ])
    }
    
    
    def CharSequence generateSignal(Signal signal) {
        ''''''
    }
    
    def generateSequenceAttribute(Property attribute) {
        '''
        public «attribute.toJavaType» «attribute.generateAccessorName»() {
        	«IF attribute.type.name == 'String'»
        	return "" + System.identityHashCode(this);
            «ELSE»
        	return System.identityHashCode(this);
            «ENDIF»
        }
        '''
    }

    def generateDerivedAttribute(Property attribute) {
        '''
        «generateDerivedAttributeGetter(attribute)»
        '''
    }
    
    def generateDerivedAttributeGetter(Property attribute) {
        '''
        public «attribute.generateStaticModifier»«attribute.toJavaType» «attribute.generateAccessorName»() {
            «generateDerivedAttributeComputation(attribute)»
        }
        '''
    }
    
    def generateDerivedAttributeComputationAsActivity(Property attribute, Activity activity) {
        activity.generateActivity()
    }
    
    def generateDerivedAttributeComputation(Property attribute) {
        if (attribute.defaultValue != null) 
            if (attribute.defaultValue.behaviorReference)
                generateDerivedAttributeComputationAsActivity(attribute, attribute.defaultValue.resolveBehaviorReference as Activity)
            else 
            '''return «attribute.defaultValue.generateValue»;'''
        else
            '''return «attribute.type.generateDefaultValue»;'''
    }
    
    def generateAttribute(Property attribute) {
        
        '''
		«generateAttributePerSe(attribute)»
		«generateAttributeGetter(attribute)»
		«generateAttributeSetter(attribute)»
        '''
    }
	
	def generateAttributeSetter(Property attribute) {
		'''
        public void set«attribute.name.toFirstUpper»(«attribute.generateStaticModifier»«attribute.toJavaType» new«attribute.name.toFirstUpper») {
            «generateAttributeInvariants(attribute)»            
            this.«generateAttributeNameAsJavaSymbol(attribute)» = new«attribute.name.toFirstUpper»;
        }
        '''
	}
	
	def generateAttributeGetter(Property attribute) {
		'''
        public «attribute.generateStaticModifier»«attribute.toJavaType» «attribute.generateAccessorName»() {
            return this.«generateAttributeNameAsJavaSymbol(attribute)»;
        }
		'''
	}
    
    def generateAttributePerSe(Property attribute) {
        val defaultValue = attribute.generateAttributeDefaultValue
    	'''
    	protected «attribute.generateStaticModifier»«attribute.toJavaType» «generateAttributeNameAsJavaSymbol(attribute)»«if (defaultValue != null) ''' = «defaultValue»'''»;
    	'''
    }
    
    def generateStaticModifier(Feature feature) {
        if (feature.static) 'static ' else ''
    }
    
    def generateRelationshipType(Property relationship) {
        if (relationship.multivalued)
            '''«if (relationship.unique) 'Set' else 'Collection'»<«relationship.type.toJavaType»>'''
        else
            relationship.type.toJavaType
    }
    
    def generateRelationship(Property relationship, Class entity) {
    	val isOverride = (relationship.class_ != null && relationship.class_.generals.exists[it.getAttribute(relationship.name, relationship.type) != null]) ||
    		(relationship.class_ == null && (#[entity] + entity.generals).filter[it.associations.exists[it.getMemberEnd(relationship.name, relationship.type) != null]].size > 1)
    	
    	if (isOverride) {
    		return generateInheritedRelationshipGetter(relationship)
    	}
    	  
        '''
        «generateRelationshipAttribute(relationship)»
        «generateRelationshipGetter(relationship)»
        «generateRelationshipSetter(relationship)»
        «IF relationship.multivalued»
		«generateRelationshipAdder(relationship)»
		«generateRelationshipRemover(relationship)»
        «ENDIF»
        '''
    }
	
	def generateRelationshipAttribute(Property relationship) {
		'''
		protected «relationship.generateStaticModifier»«relationship.generateRelationshipType» «generateAttributeNameAsJavaSymbol(relationship)»«IF relationship.multivalued» = new «relationship.toJavaCollection»<>()«ENDIF»;
		'''
	}

	def generateRelationshipSetter(Property relationship) {
		'''
        public «relationship.generateStaticModifier»void set«relationship.name.toFirstUpper»(«relationship.generateRelationshipType» new«relationship.name.toFirstUpper») {
            this.«generateAttributeNameAsJavaSymbol(relationship)» = new«relationship.name.toFirstUpper»;
        }
		'''
	}

	
	def generateRelationshipAdder(Property relationship) {
		'''
        public «relationship.generateStaticModifier»void addTo«relationship.name.toFirstUpper»(«relationship.type.toJavaType» new«relationship.name.toFirstUpper») {
            this.«generateAttributeNameAsJavaSymbol(relationship)».add(new«relationship.name.toFirstUpper»);
        }
		'''
	}
	
	def generateRelationshipRemover(Property relationship) {
		'''
        public «relationship.generateStaticModifier»void removeFrom«relationship.name.toFirstUpper»(«relationship.type.toJavaType» existing«relationship.name.toFirstUpper») {
            this.«generateAttributeNameAsJavaSymbol(relationship)».remove(existing«relationship.name.toFirstUpper»);
        }
		'''
	}
	
	def generateRelationshipGetter(Property relationship) {
		'''
    		public «relationship.generateStaticModifier»«relationship.generateRelationshipType» get«relationship.name.toFirstUpper»() {
    			return this.«generateAttributeNameAsJavaSymbol(relationship)»;
    		}
		'''
	}
	
    def generateInheritedRelationshipGetter(Property relationship) {
		'''
    		public «relationship.generateStaticModifier»«relationship.generateRelationshipType» get«relationship.name.toFirstUpper»() {
    			return super.get«relationship.name.toFirstUpper»();
    		}
		'''
    }
    
    def generateDerivedRelationship(Property relationship) {
    	'''
    	«generateDerivedRelationshipGetter(relationship)»
    	'''
    }
    
    def generateDerivedRelationshipGetter(Property relationship) {
        val derivation = relationship.derivation
        '''
        public «relationship.generateStaticModifier»«relationship.generateRelationshipAccessorType» «relationship.generateAccessorName»() {
            «generateRelationshipDerivationAsActivity(derivation, relationship)»
        }'''
    }
    
    
    def generateRelationshipDerivationAsActivity(Activity derivation, Property relationship) {
        '''return «derivation.generateActivityAsExpression»;'''
    }
    
    def CharSequence generateRelationshipAccessorType(Property relationship) {
        relationship.toJavaType
    }
    
    def generateOperation(Operation action) {
    	generateOperation(action, action.visibility)
    }
    
    def generateOperation(Operation action, VisibilityKind visibility) {
        '''
        «action.generateJavaMethodSignature(visibility, action.static)» {
            «action.generateOperationBody»
        }'''
    }
    
    def generateActionEnablement(Operation actionOperation) {
		'''
		/**
		 * Is the «actionOperation.name.toFirstUpper» action enabled at this time?
		 */
		 «generateActionEnablementGetter(actionOperation)»
		'''    	
    }

    
    def generateActionEnablementGetter(Operation actionOperation) {
    	
        val preconditions = actionOperation.preconditions
        val stateProperty = actionOperation.class_.findStateProperties.head
        val stateMachine = stateProperty?.type as StateMachine
        
        val sourceStates = if (stateMachine != null) stateMachine.findStatesForCalling(actionOperation) else #[]
		'''
		public boolean is«actionOperation.name.toFirstUpper»Enabled() {
			«IF sourceStates.size > 1»
			if (!EnumSet.of(«sourceStates.generateMany([ '''«stateProperty.type.toJavaType».«name»''' ], ', ')»).contains(«stateProperty.generateAccessorName»())) {
				return false;
			}
			«ELSEIF sourceStates.size == 1»
			if («stateProperty.generateAccessorName»() != «stateProperty.type.toJavaType».«sourceStates.head.name») {
				return false;
			}
			«ENDIF»
		    «preconditions.generateMany[ constraint |
            	val predicateActivity = constraint.specification.resolveBehaviorReference as Activity
            	val parameterless = predicateActivity.constraintParameterless
            	if (!parameterless) return ''
            	'''
            	if («generatePredicate(constraint, true)») {
            	    return false;
            	}
                '''
            ]»
		    return true;
		}
		'''
    }
    
    def generatePermissions(Class entity) {
    	val allRoleClasses = appPackages.entities.filter[ role ]
    	val instanceActions = entity.instanceActions
    	val delegate = new DelegatingBehaviorGenerator(behaviorGenerator) {
            override generateAction(Action action, boolean delegate) {
            	if (action instanceof CallOperationAction) {
            		if (action.operation.name == 'user' && action.operation.class_.qualifiedName == 'mdd_types::System') {
            			return 'subject'
            		}
            	}
            	if (action instanceof StructuredActivityNode) {
            		if (MDDExtensionUtils.isCast(action)) {
    					val targetClass = action.outputs.head.type as Classifier 
						val sourceClass = action.inputs.head.type as Classifier
						val profileToRoleClass = targetClass.roleClass && sourceClass.qualifiedName == SYSTEM_USER_CLASS
				        if (profileToRoleClass)
							return '''«action.inputs.head.generateAction»'''
            		}
            	}
            	return target.generateAction(action, false)
            }
        } 
    	
    	behaviorGenerator.runInContext(new SimpleContext('''target''', delegate), [
    	'''

        public static class Permissions {
            «allRoleClasses.map[roleClass |
    	    	'''
    	    	«generateInstancePermissions(entity, roleClass)»
    	    	«instanceActions.map[ action |
    	    		generateActionPermitted(entity, action, roleClass)
	    		].join»
    	    	'''
    	    ].join»
        }
    	'''
    	])
    }
    
    def generateInstancePermissions(Class entity, Class roleClass) {
    	'''
    	«generateInstancePermission(entity, roleClass, AccessCapability.Read)»
    	«generateInstancePermission(entity, roleClass, AccessCapability.Update)»
    	«generateInstancePermission(entity, roleClass, AccessCapability.Delete)»
    	'''
    }
    
    def generateInstancePermission(Class entity, Class roleClass, AccessCapability capability) {
    	val constraint = findAccessConstraint(#[entity], capability, roleClass)
    	'''
        public static boolean can«capability.name»(«roleClass.name» subject, «entity.name» target) {
            «IF constraint != null»
            return «generatePredicate(constraint, false)»;
            «ELSE»
            return «!hasAnyAccessConstraints(#[entity])»;
            «ENDIF»
        }'''
    }
    def generateActionPermitted(Class entity, Operation actionOperation, Class roleClass) {
    	val requiredCapability = if (actionOperation.static) AccessCapability.StaticCall else AccessCapability.Call
    	val accessConstraint = findAccessConstraint(#[actionOperation, actionOperation.class_], requiredCapability, roleClass)
    	if (accessConstraint == null)
    		return ''
    	
		'''
		/**
		 * Is the '«actionOperation.name»' action allowed for the given «roleClass.name»?
		 */
		«generateActionPermittedGetter(entity, actionOperation, roleClass, accessConstraint)»
		'''    	
    }

    
    def CharSequence generateActionPermittedGetter(Class entity, Operation actionOperation, Class roleClass, Constraint accessConstraint) {
		'''
        public static boolean is«actionOperation.name.toFirstUpper»AllowedFor(«roleClass.name» subject, «entity.name» target) {
            return «generatePredicate(accessConstraint, false)»;
        }
		'''
    }
    
    def generatePrecondition(Operation operation, Constraint constraint) {
        val predicateActivity = constraint.specification.resolveBehaviorReference as Activity
        
        val parameterless = predicateActivity.closureInputParameters.empty
        val optionalParameters = predicateActivity.closureInputParameters.filter[!required]
        val selfReference = predicateActivity.rootAction.findFirstMatchingAction([it instanceof ReadSelfAction]) != null
        val innerCore = '''
        if («generatePredicate(constraint, true)») {
            throw new «if (constraint.name?.length > 0) constraint.name else 'ConstraintViolation'»Exception();
        }
        '''
        
        val core = if (optionalParameters.empty) innerCore else '''
        if («optionalParameters.map['''«it.name» != null'''].join(' && ')») {
            «innerCore»
        }
        '''
         
        '''
        «IF parameterless && !selfReference»
        // TBD: support for global-data based preconditions
        /*
        «ENDIF»
        «core»
        «IF parameterless && !selfReference»
        */
        «ENDIF»
        '''
    }
    
    def generateOperationBody(Operation operation) {
        val firstMethod = operation.methods?.head
        val stateProperty = operation.class_.findStateProperties.head
        val stateMachine = stateProperty?.type as StateMachine
        val isEventTriggering = !operation.static && !operation.query && stateMachine != null && !stateMachine.findTriggersForCalling(operation).empty
        '''
            «generateParameterDefaults(operation)»
            «operation.preconditions.map[generatePrecondition(operation, it)].join()»
            «IF(firstMethod != null)»
            «generateActivity(firstMethod as Activity)»
            «ENDIF»
            «IF (isEventTriggering)»
            this.handleEvent(«stateMachine.name»Event.«operation.name.toFirstUpper»);
            «ENDIF»
        '''
    }
}