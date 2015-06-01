package com.abstratt.mdd.target.jse

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.util.MDDUtil
import java.util.List
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.query.conditions.eobjects.EObjectCondition
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Classifier
import org.eclipse.uml2.uml.Constraint
import org.eclipse.uml2.uml.Feature
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.Parameter
import org.eclipse.uml2.uml.Port
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.SendSignalAction
import org.eclipse.uml2.uml.Signal
import org.eclipse.uml2.uml.StateMachine
import org.eclipse.uml2.uml.UMLPackage

import static com.abstratt.mdd.target.jse.PlainJavaGenerator.*

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import static extension com.abstratt.mdd.core.util.StateMachineUtils.*
import static extension com.abstratt.mdd.target.jse.KirraToJavaHelper.*
import com.abstratt.mdd.core.util.MDDExtensionUtils
import com.abstratt.mdd.target.jse.IBehaviorGenerator.IExecutionContext
import com.abstratt.mdd.target.jse.IBehaviorGenerator.SimpleContext
import org.eclipse.uml2.uml.ActivityNode
import org.eclipse.uml2.uml.ReadStructuralFeatureAction
import org.eclipse.uml2.uml.Action
import org.eclipse.uml2.uml.ReadSelfAction

class PlainEntityGenerator extends BehaviorlessClassGenerator {

    protected String applicationName

    protected StateMachineGenerator stateMachineGenerator
    
    IBehaviorGenerator behaviorGenerator
    
    new(IRepository repository) {
        super(repository)
        behaviorGenerator = createBehaviorGenerator()
        stateMachineGenerator = new StateMachineGenerator(repository, createBehaviorGenerator())
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
            .toSet
        
        if (!providers.empty)
        '''
        /*************************** PROVIDERS ***************************/
        
        «generateMany(providers, [generateProvider])»
        '''    
        
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
        val actionOperations = entity.actions.filter[!static]
        val relationships = entity.entityRelationships.filter[!derived && navigable]
        val attributeInvariants = entity.properties.filter[!derived].map[findInvariantConstraints].flatten
        val derivedAttributes = entity.properties.filter[derived]
        val derivedRelationships = entity.entityRelationships.filter[derived]
        val privateOperations = entity.operations.filter[!action && !finder]
        val stateProperty = entity.findStateProperties.head
        val stateMachine = stateProperty?.type as StateMachine
        val ports = entity.getAllAttributes().filter(typeof(Port))
        val signals = findTriggerableSignals(actionOperations)
        
        '''
            package «entity.packagePrefix»;
            
            «generateStandardImports»
            «entity.generateImports»
            
            «entity.generateComment»
            «entity.generateEntityAnnotations»public class «entity.name» «entity.generateEntityGenealogy»{
                
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
                «entity.generateAttributes»
                «IF !relationships.empty»
                    /*************************** RELATIONSHIPS ***************************/
                    
                    «generateMany(relationships, [generateRelationship])»
                «ENDIF»
                «IF !actionOperations.empty»
                    /*************************** ACTIONS ***************************/
                    
                    «generateMany(actionOperations, [generateActionOperation])»
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
    
    def generateEntityGenealogy(Class class1) {
        ''
    }
    
    def generateEntityAnnotations(Class class1) {
        ''
    }
    
    def generateEntityId(Class entity) {
        ''
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
        // for now...
        operation.generateActionOperation
    }

    def generateDerivedRelationships(Iterable<Property> properties) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    def generateQueryOperations(List<Operation> operations) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
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
        behaviorGenerator.enterContext(newContext)
        try {
        '''
        if («generatePredicate(constraint, true)») {
            throw new «if (constraint.name?.length > 0) constraint.name else 'Runtime'»Exception();
        }
        '''
        } finally {
            behaviorGenerator.leaveContext(newContext)            
        }
    }
    
    
    def CharSequence generateSignal(Signal signal) {
        ''''''
    }

    def generateDerivedAttribute(Property attribute) {
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
    
    def generateAttributes(Class entity) {
        val attributes = entity.properties.filter[!derived]
        if (attributes.empty) return ''
        '''
            /*************************** ATTRIBUTES ***************************/
            
            «generateMany(attributes, [generateAttribute])»
        '''
    }
    
    def generateAttributeDefaultValue(Property attribute) {
        if (attribute.defaultValue != null) {
            if (attribute.defaultValue.behaviorReference)
                (attribute.defaultValue.resolveBehaviorReference as Activity).generateActivityAsExpression 
            else
                attribute.defaultValue.generateValue
        } else if (attribute.required || attribute.type.enumeration)
            // enumeration covers state machines as well
            attribute.type.generateDefaultValue
    }
    
    def generateAttribute(Property attribute) {
        val defaultValue = attribute.generateAttributeDefaultValue
        
        '''
        private «attribute.generateStaticModifier»«attribute.toJavaType» «attribute.name»«if (defaultValue != null) ''' = «defaultValue»'''»;
        
        public «attribute.generateStaticModifier»«attribute.toJavaType» get«attribute.name.toFirstUpper»() {
            return this.«attribute.name»;
        }
        
        public void set«attribute.name.toFirstUpper»(«attribute.generateStaticModifier»«attribute.toJavaType» new«attribute.name.toFirstUpper») {
            «generateAttributeInvariants(attribute)»            
            this.«attribute.name» = new«attribute.name.toFirstUpper»;
        }
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
    
    def generateRelationship(Property relationship) {
        '''
        private «relationship.generateStaticModifier»«relationship.generateRelationshipType» «relationship.name»«IF relationship.multivalued» = new «relationship.toJavaCollection»<>()«ENDIF»;
        
        public «relationship.generateStaticModifier»«relationship.generateRelationshipType» get«relationship.name.toFirstUpper»() {
            return this.«relationship.name»;
        }
        
        «IF relationship.multivalued»
        
        public «relationship.generateStaticModifier»void addTo«relationship.name.toFirstUpper»(«relationship.type.toJavaType» new«relationship.name.toFirstUpper») {
            this.«relationship.name».add(new«relationship.name.toFirstUpper»);
        }
        
        public «relationship.generateStaticModifier»void removeFrom«relationship.name.toFirstUpper»(«relationship.type.toJavaType» existing«relationship.name.toFirstUpper») {
            this.«relationship.name».remove(existing«relationship.name.toFirstUpper»);
        }
        «ELSE»
        public «relationship.generateStaticModifier»void set«relationship.name.toFirstUpper»(«relationship.generateRelationshipType» new«relationship.name.toFirstUpper») {
            this.«relationship.name» = new«relationship.name.toFirstUpper»;
        }
        «ENDIF»
        '''
    }
    
    def generateDerivedRelationship(Property relationship) {
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
    
    def generateActionOperation(Operation action) {
        '''
        «action.generateJavaMethodSignature(action.visibility, action.static)» {
            «action.generateActionOperationBody»
        }'''
    }
    
    def generatePrecondition(Operation operation, Constraint constraint) {
        val predicateActivity = constraint.specification.resolveBehaviorReference as Activity
        
        val parameterless = predicateActivity.closureInputParameters.empty
        val selfReference = predicateActivity.rootAction.findFirstMatchingAction([it instanceof ReadSelfAction]) != null 
        '''
        «IF parameterless && !selfReference»
        // TBD: support for global-data based preconditions
        /*
        «ENDIF»
        if («generatePredicate(constraint, true)») {
            throw new «if (constraint.name?.length > 0) constraint.name else 'Runtime'»Exception();
        }
        «IF parameterless && !selfReference»
        */
        «ENDIF»
        '''
    }
    
    def generateActionOperationBody(Operation actionOperation) {
        val firstMethod = actionOperation.methods?.head
        val stateProperty = actionOperation.class_.findStateProperties.head
        val stateMachine = stateProperty?.type as StateMachine
        val isEventTriggering = stateMachine != null && !stateMachine.findTriggersForCalling(actionOperation).empty
        '''
            «actionOperation.preconditions.map[generatePrecondition(actionOperation, it)].join()»
            «IF(firstMethod != null)»
            «generateActivity(firstMethod as Activity)»
            «ENDIF»
            «IF (isEventTriggering)»
            this.handleEvent(«stateMachine.name»Event.«actionOperation.name.toFirstUpper»);
            «ENDIF»
        '''
    }
}