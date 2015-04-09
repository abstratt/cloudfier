package com.abstratt.mdd.target.jse

import com.abstratt.mdd.core.IRepository
import java.util.List
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.AnyReceiveEvent
import org.eclipse.uml2.uml.CallEvent
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Event
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.Pseudostate
import org.eclipse.uml2.uml.SignalEvent
import org.eclipse.uml2.uml.State
import org.eclipse.uml2.uml.StateMachine
import org.eclipse.uml2.uml.TimeEvent
import org.eclipse.uml2.uml.Transition
import org.eclipse.uml2.uml.Trigger
import org.eclipse.uml2.uml.Vertex

import static extension com.abstratt.mdd.core.util.StateMachineUtils.*
import org.eclipse.uml2.uml.Parameter
import com.abstratt.mdd.target.jse.IBehaviorGenerator.IExecutionContext
import com.abstratt.mdd.target.jse.IBehaviorGenerator.SimpleContext

class StateMachineGenerator extends BehaviorlessClassGenerator {
    
    IBehaviorGenerator behaviorGenerator
    
    new(IRepository repository, IBehaviorGenerator behaviorGenerator) {
        super(repository)
        this.behaviorGenerator = behaviorGenerator  
    }
    
    override generateActivity(Activity a) {
        behaviorGenerator.generateActivity(a)
    }
    
    override generateActivityAsExpression(Activity toGenerate, boolean asClosure, List<Parameter> parameters) {
        behaviorGenerator.generateActivityAsExpression(toGenerate, asClosure, parameters)
    }
    
    
    def dispatch generateState(State state, StateMachine stateMachine, Class entity) {
        '''
        «state.name» {
            «IF (state.entry != null)»
            @Override void onEntry(«entity.name» instance) {
                «(state.entry as Activity).generateActivity»
            }«
            ENDIF»
            «IF (state.exit != null)»
            @Override void onExit(«entity.name» instance) {
                «(state.exit as Activity).generateActivity»
            }
            «ENDIF»
            «state.generateStateEventHandler(stateMachine, entity)»
        }
        '''
    }
    
    def dispatch generateState(Pseudostate state, StateMachine stateMachine, Class entity) {
        '''
        «state.name» {
            «state.generateStateEventHandler(stateMachine, entity)»
        }
        ''' 
    }
    
    def generateStateEventHandler(Vertex state, StateMachine stateMachine, Class entity) {
        '''
        @Override void handleEvent(«entity.name» instance, «stateMachine.name»Event event) {
            «IF (!state.outgoings.empty)»
            switch (event) {
                «state.findTriggersPerEvent.entrySet.generateMany[pair |
                    val event = pair.key
                    val triggers = pair.value
                    '''
                    case «event.generateEventName» :
                        «triggers.map[generateTrigger].join()»
                        break;
                    ''' 
                ]»
                default : break; // unexpected events are silently ignored 
            }
            «ELSE»
            // this is a final state
            «ENDIF»     
        }                       
        '''
    }
    
    def generateTrigger(Trigger trigger) {
        val transition = trigger.eContainer as Transition
        '''
        «IF (transition.guard != null)»
        if («transition.guard.generatePredicate») {
            «transition.generateTransition»
            break;
        }
        «ELSE»
        «transition.generateTransition»
        «ENDIF»
        '''
    }
    
    def generateTransition(Transition transition) {
        '''
        «IF (transition.effect != null)»
        «(transition.effect as Activity).generateActivity»
        «ENDIF»
        doTransitionTo(instance, «transition.target.name»);
        '''
    }
    
    def generateStateMachine(StateMachine stateMachine, Class entity) {
        val context = new SimpleContext('instance') 
        behaviorGenerator.enterContext(context)
        try {
            doGenerateStateMachine(stateMachine, entity)
        } finally {
            behaviorGenerator.leaveContext(context)    
        }
    }
    
    private def doGenerateStateMachine(StateMachine stateMachine, Class entity) {
        val stateAttribute = entity.findStateProperties.head
        if (stateAttribute == null)
            return ''
        val triggersPerEvent = stateMachine.findTriggersPerEvent
        val eventNames = triggersPerEvent.keySet.map[it.generateEventName.toString]
        '''
        «stateMachine.generateComment»
        public enum «stateMachine.name» {
            «stateMachine.vertices.map[
                generateState(it, stateMachine, entity).toString.trim
            ].join(',\n')»;
            «generateBaseMethods(stateMachine, entity, stateAttribute)»
        }
        
        «generateStateMachineEventEnumeration(stateMachine, eventNames)»
        
        public void handleEvent(«stateMachine.name»Event event) {
            «stateAttribute.name».handleEvent(this, event);
        }
        '''
    }
    def generateStateMachineEventEnumeration(StateMachine stateMachine, Iterable<String> eventNames) {
    '''
        public enum «stateMachine.name»Event {
            «eventNames.join(',\n')»
        }
    '''        
    }
    
    
    def generateBaseMethods(StateMachine stateMachine, Class entity, Property stateAttribute) {
        '''
            void onEntry(«entity.name» instance) {
                // no entry behavior by default
            }
            void onExit(«entity.name» instance) {
                // no exit behavior by default
            }
            /** Each state implements handling of events. */
            abstract void handleEvent(«entity.name» instance, «stateMachine.name»Event event);
            /** 
                Performs a transition.
                @param instance the instance to update
                @param newState the new state to transition to 
            */
            final void doTransitionTo(«entity.name» instance, «stateMachine.name» newState) {
                instance.«stateAttribute.name».onExit(instance);
                instance.«stateAttribute.name» = newState;
                instance.«stateAttribute.name».onEntry(instance);
            }
        '''
    }

    def generateEvent(Class entity, Property stateAttribute, Event event, List<Trigger> triggers) {
        '''«event.generateEventName»'''
    }

    def generateEventName(Event e) {
        switch (e) {
            CallEvent : e.operation.name.toFirstUpper
            SignalEvent : e.signal.name
            TimeEvent : '_time'
            AnyReceiveEvent : '_any'
            default : unsupportedElement(e)
        }
    }
}
