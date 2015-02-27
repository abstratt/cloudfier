package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.util.MDDUtil
import com.abstratt.mdd.core.util.NamedElementUtils
import java.util.List
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.query.conditions.eobjects.EObjectCondition
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Classifier
import org.eclipse.uml2.uml.Constraint
import org.eclipse.uml2.uml.CreateObjectAction
import org.eclipse.uml2.uml.DestroyObjectAction
import org.eclipse.uml2.uml.NamedElement
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.Port
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.ReadExtentAction
import org.eclipse.uml2.uml.SendSignalAction
import org.eclipse.uml2.uml.Signal
import org.eclipse.uml2.uml.StateMachine
import org.eclipse.uml2.uml.UMLPackage

import static com.abstratt.mdd.target.jee.AbstractJavaGenerator.*

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import static extension com.abstratt.mdd.core.util.StateMachineUtils.*
import org.eclipse.uml2.uml.Feature

class EntityGenerator extends AbstractJavaGenerator {

    protected IRepository repository

    protected String applicationName

    new(IRepository repository) {
        super(repository)
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
            .filter[isProviderOperation]
            .map[class_]
            .toSet
        
        if (!providers.empty)
        '''
        /*************************** PROVIDERS ***************************/
        
        «generateMany(providers, [generateProvider])»
        '''    
        
    }
    
    def generateAnonymousDataTypes(Class context) {
        val allActivities = MDDUtil.findAllFrom(new EObjectCondition() {
            override isSatisfied(EObject eObject) {
                return UMLPackage.Literals.ACTIVITY.isInstance(eObject)
            }
        }, #{context})
        allActivities.map[(it as Activity).anonymousDataTypes].flatten.map[generateDataType].join
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
        import javax.persistence.*;
        import javax.inject.*;
        import javax.ejb.*;
        import javax.enterprise.event.*;
        '''
    }
    
    def generateEntity(Class entity) {
        val actionOperations = entity.actions.filter[!static]
        val attributes = entity.properties.filter[!derived]
        val relationships = entity.entityRelationships.filter[!derived]
        val attributeInvariants = attributes.map[findInvariantConstraints].flatten
        val derivedAttributes = entity.properties.filter[derived]
        val derivedRelationships = entity.entityRelationships.filter[derived]
        val privateOperations = entity.operations.filter[!action && !finder]
        val hasState = !entity.findStateProperties.empty
        val signals = findTriggerableSignals(actionOperations + privateOperations)
        
        val ports = entity.allAttributes.filter(typeof(Port))
        
        '''
            package «entity.packagePrefix»;
            
            «generateStandardImports»            
            «entity.generateImports»
            
            «entity.generateComment»
            @Entity
            public class «entity.name» {
                
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
                
                «entity.generateAnonymousDataTypes»

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
                
                «IF !relationships.empty»
                    /*************************** RELATIONSHIPS ***************************/
                    
                    «generateMany(relationships, [generateRelationship])»
                «ENDIF»
                
                
«««                «IF !attributeInvariants.empty»
«««                    /*************************** INVARIANTS ***************************/
«««                    
«««                    «generateAttributeInvariants(attributeInvariants)»
«««                «ENDIF»
«««                

                «IF !actionOperations.empty»
                    /*************************** ACTIONS ***************************/

                    «generateMany(actionOperations, [generateActionOperation])»
                «ENDIF»
«««                «IF !queryOperations.empty»
«««                    /*************************** QUERIES ***************************/
«««                    
«««                    «generateQueryOperations(entity.queries)»
«««                «ENDIF»

                «IF !derivedAttributes.empty»
                    /*************************** DERIVED PROPERTIES (TBD) ****************/
                    
                    «generateMany(derivedAttributes, [generateDerivedAttribute])»
                «ENDIF»

                «IF !derivedRelationships.empty»
                    /*************************** DERIVED RELATIONSHIPS (TBD) ****************/
                    
                    «generateMany(derivedRelationships, [generateDerivedRelationship])»
                «ENDIF»

                «IF !privateOperations.empty»
                    /*************************** PRIVATE OPS ***********************/
                    
                    «generateMany(privateOperations, [generatePrivateOperation])»
                «ENDIF»

                «IF hasState»
                    /*************************** STATE MACHINE ********************/
                    «entity.findStateProperties.map[it.type as StateMachine].head.generateStateMachine(entity)»
                    
                «ENDIF»
            }
        '''
    }
    
    def findTriggerableSignals(Iterable<Operation> operations) {
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
        @Inject private «provider.toJavaType»Service «provider.name.toFirstLower»Service = new «provider.toJavaType»Service();
        '''
    }
    
    def generatePort(Port port) {
        '''
        @Inject «port.requireds.head.toJavaType» «port.name.toFirstLower»;
        '''
    }
    
    def isProviderOperation(Operation toCheck) {
        toCheck.static && toCheck.class_.entity  
    }
    
    override generateCreateObjectAction(CreateObjectAction action) {
        if (!action.classifier.entity)
            return super.generateCreateObjectAction(action)
        val javaClass = action.classifier.toJavaType
        '''«javaClass».objectCreated(«super.generateCreateObjectAction(action)»)'''
    }
    
    override generateDestroyObjectAction(DestroyObjectAction action) {
        if (!action.target.type.entity)
            return super.generateDestroyObjectAction(action)
        val javaClass = action.target.type.toJavaType
        '''«javaClass».objectDestroyed(«action.target»)'''
    }
    
    override generateReadExtentAction(ReadExtentAction action) {
        if (!action.classifier.entity)
            return '''Collections.<«action.classifier.toJavaType».emptyList()'''
        '''«action.classifier.toJavaType».extent()'''
    }
    
    
    override protected generateCallOperationAction(CallOperationAction action) {
        if (!action.operation.providerOperation)
            return super.generateCallOperationAction(action)
        val provider = action.operation.class_
        val entity = NamedElementUtils.findNearest(action.actionActivity, [ 
            NamedElement e | e instanceof Class && (e as Class).entity
        ]) as Class
        generateOperationCall(generateProviderReference(entity, provider), action)
    }
    
    def generateProviderReference(Class context, Class provider) {
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


    def generateAttributeInvariants(Iterable<Constraint> constraints) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    def generateSignal(Signal signal) {
        '''
        @Inject
        Event<«signal.name»Event> «signal.name.toFirstLower»Event;
        '''
    }
    
    override def generateSendSignalAction(SendSignalAction action) {
        val eventName = '''«action.signal.name.toFirstLower»Event'''
        val signalName = action.signal.name
        //'''/* TBD */ //this.«eventName».fire(new «signalName»Event(«action.arguments.generateMany([arg | arg.generateAction], ', ')»))'''
        
        // TODO - this is a temporary implementation
        val targetClassifier = action.target.type as Classifier
        if (targetClassifier.entity && !targetClassifier.findStateProperties.empty) {
            val stateMachine = targetClassifier.findStateProperties.head 
            '''«action.target.generateAction».handleEvent(«action.target.toJavaType».«stateMachine.name.toFirstUpper»Event.«signalName»)'''
        }
    }
    
    def generateDerivedAttribute(Property attribute) {
        '''
        public «attribute.generateStaticModifier»«attribute.toJavaType» «attribute.generateAccessorName»() {
            «generateDerivedAttributeComputation(attribute)»
        }
        '''
    }
    
    def generateDerivedAttributeComputation(Property attribute) {
        if (attribute.defaultValue != null) 
            if (attribute.defaultValue.behaviorReference)
                (attribute.defaultValue.resolveBehaviorReference as Activity).generateActivity
            else 
            '''return «attribute.defaultValue.generateValue»;'''
        else
            '''return «attribute.type.generateDefaultValue»;'''
    }
    
    def generateAttribute(Property attribute) {
        val defaultValue = if (attribute.defaultValue != null) {
                if (attribute.defaultValue.behaviorReference)
                    (attribute.defaultValue.resolveBehaviorReference as Activity).generateActivityAsExpression 
                else
                    attribute.defaultValue.generateValue
            } 
            else if (attribute.required || attribute.type.enumeration)
                // enumeration covers state machines as well
                attribute.type.generateDefaultValue
        
        '''public «attribute.generateStaticModifier»«attribute.toJavaType» «attribute.name»«if (defaultValue != null) ''' = «defaultValue»'''»;'''
    }
    
    def generateStaticModifier(Feature feature) {
        if (feature.static) 'static ' else ''
    }
    
    
    def generateRelationship(Property relationship) {
        '''public «relationship.generateStaticModifier»«relationship.toJavaType» «relationship.name»«IF relationship.multivalued» = new «relationship.toJavaCollection»<>()«ENDIF»;'''
    }
    
    def generateDerivedRelationship(Property relationship) {
        val derivation = relationship.defaultValue.resolveBehaviorReference as Activity
        '''
        public «relationship.generateStaticModifier»«relationship.toJavaType» «relationship.generateAccessorName»() {
            return «derivation.generateActivityAsExpression»;
        }'''
    }
    
    def generateActionOperation(Operation action) {
        '''
        «action.generateJavaMethodSignature(action.visibility, action.static)» «action.generateActionOperationBody»
        '''
    }
    
    def generateActionOperationBody(Operation actionOperation) {
        val firstMethod = actionOperation.methods?.head
        val stateProperty = actionOperation.class_.findStateProperties.head
        val stateMachine = stateProperty?.type as StateMachine
        val isEventTriggering = stateMachine != null && !stateMachine.findTriggersForCalling(actionOperation).empty
        '''
        {
            «IF(firstMethod != null)»
            «generateActivity(firstMethod as Activity)»
            «ENDIF»
            «IF (isEventTriggering)»
            this.handleEvent(«stateMachine.name»Event.«actionOperation.name.toFirstUpper»);
            «ENDIF»
        }
        '''
    }
}