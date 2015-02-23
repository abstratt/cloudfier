package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import java.util.List
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Constraint
import org.eclipse.uml2.uml.Feature
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.ParameterDirectionKind
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.SendSignalAction
import org.eclipse.uml2.uml.Signal
import org.eclipse.uml2.uml.StateMachine
import org.eclipse.uml2.uml.UMLPackage

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.ActivityUtils.*
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
        val actionOperations = entity.actions
        val attributes = entity.properties.filter[!derived]
        val relationships = entity.entityRelationships.filter[!derived && it.likeLinkRelationship]
        val attributeInvariants = attributes.map[findInvariantConstraints].flatten
        val derivedAttributes = entity.properties.filter[derived]
        val derivedRelationships = entity.entityRelationships.filter[derived]
        val privateOperations = entity.operations.filter[!action && !finder]
        val hasState = !entity.findStateProperties.empty
        val signals = entity.allOperations.filter[op | op.activity != null].map[op | 
            op.activity.bodyNode
                .findMatchingActions(UMLPackage.Literals.SEND_SIGNAL_ACTION)
                .map[sendSignal | 
                    (sendSignal as SendSignalAction).signal
                ]
        ].flatten
        val signalPackages = signals.map[ nearestPackage ].toSet

        '''
            package «entity.packagePrefix».entity;
            
            import java.util.*;
            import javax.persistence.*;
            import javax.inject.*;
            import javax.ejb.*;
            import javax.enterprise.event.*;
            
            «signalPackages.generateMany['''import «it.toJavaPackage».event.*;''']»
            
                
            «entity.generateComment»
            @Entity
            public class «entity.name» {
            
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
        '''public «if (attribute.static) 'static ' else ''»«attribute.type.toJavaType» «attribute.name»; /*TBD*/'''
    }
    
    def generateAttribute(Property attribute) {
        val defaultValue = if (attribute.defaultValue != null) 
                attribute.defaultValue.generateValue
            else if (attribute.required || attribute.type.enumeration)
                // enumeration covers state machines as well
                attribute.type.generateDefaultValue
        
        '''public «if (attribute.static) 'static ' else ''»«attribute.type.toJavaType» «attribute.name»«if (defaultValue != null) ''' = «defaultValue»'''»;'''
    }
    
    
    def generateRelationship(Property relationship) {
        if (relationship.multivalued)
        '''public Collection<«relationship.type.name»> «relationship.name» = new LinkedHashSet<«relationship.type.name»>();'''
        else
        '''public «if (relationship.static) 'static ' else ''»«relationship.type.name» «relationship.name»;'''
    }
    
    def generateDerivedRelationship(Property attribute) {
        '''«generateRelationship(attribute)» /*TBD*/'''
    }
    
    def generateActionOperation(Operation action) {
        val javaType = if (action.type == null) "void" else action.type.toJavaType
        val parameters = action.ownedParameters.filter[it.direction != ParameterDirectionKind.RETURN_LITERAL]
        val methodName = action.name
        '''
        «action.generateComment»
        «action.visibility.getName()» «if (action.static) 'static ' else ''»«javaType» «methodName»(«parameters.generateMany([ p | '''«p.type.toJavaType» «p.name»''' ], ', ')») «action.generateActionOperationBody»
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