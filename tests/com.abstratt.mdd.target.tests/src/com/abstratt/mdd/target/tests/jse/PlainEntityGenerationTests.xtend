package com.abstratt.mdd.target.tests.jse

import com.abstratt.mdd.core.tests.harness.AssertHelper
import com.abstratt.mdd.target.jse.PlainEntityGenerator
import com.abstratt.mdd.target.tests.AbstractGeneratorTest
import org.junit.Test

class PlainEntityGenerationTests extends AbstractGeneratorTest {
    new(String name) {
        super(name)
    }
    
    def void buildModel() {
        var source = '''
            model mymodel;
                class MyClass1
                    attribute anAttribute : Integer;
                    attribute status : StateMachine1;
                    operation op1();
                    operation op2();
                    statemachine StateMachine1
                
                        initial state State0
                            transition on call(op1) to State1;
                            transition on call(op2) to State2;
                        end;
                
                        state State1
                        end;
                
                        state State2
                        end;        
                    end;
                end;                    
            end.
         '''
        parseAndCheck(source)
    }
    
    @Test
    def void testStateMachine_Enumeration() {
        buildModel()
        val class1 = getClass('mymodel::MyClass1')
        
        val generated = new PlainEntityGenerator(repository).generateStateMachine(class1)
        AssertHelper.assertStringsEqual(
        '''
    /***************************STATE MACHINE********************/
    public enum StateMachine1 {
        State0 {
            @Override void handleEvent(MyClass1 instance, StateMachine1Event event) {
                switch (event) {
                    case Op1:
                        doTransitionTo(instance, State1);
                        break;
                    case Op2:
                        doTransitionTo(instance, State2);
                        break;
                    default:
                        break; /*unexpected events are silently ignored*/
                }
            }
        },
        State1 {
            @Override void handleEvent(MyClass1 instance, StateMachine1Event event) { 
                /*this is a final state*/
            }
        },
        State2 {
            @Override void handleEvent(MyClass1 instance, StateMachine1Event event) { 
                /*this is a final state*/
            }
        };
        void onEntry(MyClass1 instance) { 
            /*no entry behavior by default*/
        }
        void onExit(MyClass1 instance) { 
            /*no exit behavior by default*/
        } 
        /**Each state implements handling of events.*/
        abstract void handleEvent(MyClass1 instance, StateMachine1Event event);
        /** 
         *  Performs a transition.
         *  @param instance the instance to update
         *  @param newState the new state to transition to 
         */
        final void doTransitionTo(MyClass1 instance, StateMachine1 newState) {
            instance.status.onExit(instance);
            instance.status = newState;
            instance.status.onEntry(instance);
        }
    }
    public enum StateMachine1Event {
        Op1,
        Op2
    }
    public void handleEvent(StateMachine1Event event) {
        status.handleEvent(this, event);
    }
        ''', generated.toString)
        
    }
    
    
}
