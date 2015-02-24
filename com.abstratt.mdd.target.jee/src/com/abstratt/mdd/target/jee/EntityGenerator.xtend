package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import java.util.List
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Constraint
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.ParameterDirectionKind
import org.eclipse.uml2.uml.Port
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.SendSignalAction
import org.eclipse.uml2.uml.Signal
import org.eclipse.uml2.uml.StateMachine
import org.eclipse.uml2.uml.UMLPackage

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.ConnectorUtils.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import static extension com.abstratt.mdd.core.util.StateMachineUtils.*

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
        
        val providers = entity.allOperations
            .filter[op | op.activity != null]
            .map[op | 
                findOperationCalls.apply(op.activity)
                    .filter[isProviderOperation]
                    .map[class_]
        ].flatten.toSet
        
        
        
        
        
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
        @Inject private «provider.toJavaType»Service «provider.name.toFirstLower»Service;
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
        '''this.«eventName».fire(new «signalName»Event(«action.arguments.generateMany([arg | arg.generateAction], ', ')»))'''
    }

    def generateDerivedAttribute(Property attribute) {
        '''public «if (attribute.static) 'static ' else ''»«attribute.toJavaType» «attribute.name»; /*TBD*/'''
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
        '''public «if (relationship.static) 'static ' else ''»«relationship.toJavaType» «relationship.name»;'''
    }
    
    def generateDerivedRelationship(Property attribute) {
        '''«generateRelationship(attribute)» /*TBD*/'''
    }
    
    def generateActionOperation(Operation action) {
        val javaType = if (action.getReturnResult == null) "void" else action.getReturnResult.toJavaType
        val parameters = action.ownedParameters.filter[it.direction != ParameterDirectionKind.RETURN_LITERAL]
        val methodName = action.name
        '''
        «action.generateComment»
        «action.visibility.getName()» «if (action.static) 'static ' else ''»«javaType» «methodName»(«parameters.generateMany([ p | '''«p.toJavaType» «p.name»''' ], ', ')») «action.generateActionOperationBody»
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
            this.«stateProperty.name».handleEvent(this, «stateMachine.name»Event.«actionOperation.name.toFirstUpper»);
            «ENDIF»
        }
        '''
    }
    
}