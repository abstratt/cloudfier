package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.util.MDDUtil
import java.util.List
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.query.conditions.eobjects.EObjectCondition
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Constraint
import org.eclipse.uml2.uml.CreateObjectAction
import org.eclipse.uml2.uml.DestroyObjectAction
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.ParameterDirectionKind
import org.eclipse.uml2.uml.Port
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.ReadExtentAction
import org.eclipse.uml2.uml.SendSignalAction
import org.eclipse.uml2.uml.Signal
import org.eclipse.uml2.uml.StateMachine
import org.eclipse.uml2.uml.Type
import org.eclipse.uml2.uml.UMLPackage

import static com.abstratt.mdd.target.jee.AbstractJavaGenerator.*

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import static extension com.abstratt.mdd.core.util.StateMachineUtils.*
import org.eclipse.uml2.uml.Classifier

class EntityGenerator extends AbstractJavaGenerator {

    protected IRepository repository

    protected String applicationName

    protected Iterable<Class> entities

    new(IRepository repository) {
        super(repository)
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
        
        val ports = entity.allAttributes.filter(typeof(Port))
        
        '''
            package «entity.packagePrefix»;
            
            import java.util.*;
            import java.util.stream.*;
            import javax.persistence.*;
            import javax.inject.*;
            import javax.ejb.*;
            import javax.enterprise.event.*;
            
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
                
                «IF !providers.empty»
                    /*************************** PROVIDERS ***************************/
                    
                    «generateMany(providers, [generateProvider])»
                «ENDIF»
                
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
        generateOperationCall('''«provider.name.toFirstLower»Service«»''', action)
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
        public «if (attribute.static) 'static ' else ''»«attribute.toJavaType» «attribute.generateAccessorName»() {
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
        val defaultValue = if (attribute.defaultValue != null) 
                attribute.defaultValue.generateValue
            else if (attribute.required || attribute.type.enumeration)
                // enumeration covers state machines as well
                attribute.type.generateDefaultValue
        
        '''public «if (attribute.static) 'static ' else ''»«attribute.toJavaType» «attribute.name»«if (defaultValue != null) ''' = «defaultValue»'''»;'''
    }
    
    
    def generateRelationship(Property relationship) {
        '''public «if (relationship.static) 'static ' else ''»«relationship.toJavaType» «relationship.name»«IF relationship.multivalued» = new «relationship.toJavaCollection»<>()«ENDIF»;'''
    }
    
    def generateDerivedRelationship(Property relationship) {
        '''
        public «relationship.toJavaType» «relationship.generateAccessorName»() {
            return «relationship.defaultValue.generateValue»;
        }'''
    }
    
    def generateActionOperation(Operation action) {
        val parameters = action.ownedParameters.filter[it.direction != ParameterDirectionKind.RETURN_LITERAL]
        val methodName = action.name
        '''
        «action.generateComment»
        «action.visibility.toJavaVisibility» «if (action.static) 'static ' else ''»«action.javaReturnType» «methodName»(«parameters.generateMany([ p | '''«p.toJavaType» «p.name»''' ], ', ')») «action.generateActionOperationBody»
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