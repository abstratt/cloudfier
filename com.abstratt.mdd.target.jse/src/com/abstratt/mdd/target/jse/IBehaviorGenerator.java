package com.abstratt.mdd.target.jse;

import java.util.Arrays;
import java.util.List;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Parameter;

public interface IBehaviorGenerator {
    
    public interface IExecutionContext {
        public CharSequence generateCurrentReference(); 
    }
    
    class SimpleContext implements IExecutionContext {
        private final CharSequence reference;

        public SimpleContext(String reference) {
            this.reference = reference;
        }
        @Override
        public CharSequence generateCurrentReference() {
            return reference;
        }
    }
    
    CharSequence generateAction(ActivityNode action);
    
    CharSequence generateActivity(Activity activity);

    CharSequence generateActivityAsExpression(Activity toGenerate, boolean asClosure, List<Parameter> parameters);

    default CharSequence generateActivityAsExpression(Activity toGenerate, boolean asClosure) {
        return this.generateActivityAsExpression(toGenerate, asClosure, Arrays.<Parameter> asList());
    }

    default CharSequence generateActivityAsExpression(Activity toGenerate) {
        return this.generateActivityAsExpression(toGenerate, false, Arrays.<Parameter> asList());
    }
    
    void enterContext(IExecutionContext context);
    void leaveContext(IExecutionContext context);
    IExecutionContext getContext();
}
