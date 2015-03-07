package com.abstratt.mdd.target.jse;

import java.util.Arrays;
import java.util.List;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Parameter;

public interface IBehaviorGenerator extends IBasicBehaviorGenerator {
    
    public interface IExecutionContext {
        public def CharSequence generateCurrentReference(); 
    }
    
    class SimpleContext implements IExecutionContext {
        private final CharSequence reference;

        public new(String reference) {
            this.reference = reference;
        }
        override CharSequence generateCurrentReference() {
            return reference;
        }
    }
    
    
    def void enterContext(IExecutionContext context);
    def void leaveContext(IExecutionContext context);
    def IExecutionContext getContext();
    
    def CharSequence generateAction(ActivityNode action);
}
