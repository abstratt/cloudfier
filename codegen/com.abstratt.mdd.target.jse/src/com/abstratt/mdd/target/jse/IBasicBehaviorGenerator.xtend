package com.abstratt.mdd.target.jse

import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.Parameter
import java.util.List
import org.eclipse.uml2.uml.ActivityNode
import java.util.Arrays

import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import org.eclipse.uml2.uml.Action

public interface IBasicBehaviorGenerator {
    def CharSequence generateActivity(Activity activity)

    def CharSequence generateActivityAsExpression(Activity toGenerate, boolean asClosure, List<Parameter> parameters)
    
    def CharSequence generateActivityAsExpression(Activity toGenerate) {
        return this.generateActivityAsExpression(toGenerate, false, Arrays.<Parameter>asList())
    }

    def generateActivityAsExpression(Activity toGenerate, boolean asClosure) {
        generateActivityAsExpression(toGenerate, asClosure, toGenerate.closureInputParameters)
    }
    
    def CharSequence generateAction(Action action, boolean delegate)
    
    def CharSequence generateAction(Action action) {
        generateAction(action, true)
    }
}

public class DelegatingBehaviorGenerator implements IBasicBehaviorGenerator {
    
    IBasicBehaviorGenerator target
    
    new(IBasicBehaviorGenerator target) {
        this.target = target
    } 
    
    override generateAction(Action action, boolean delegate) {
        target.generateAction(action, delegate)
    }
    
    override generateActivity(Activity activity) {
        target.generateActivity(activity)
    }
    
    override generateActivityAsExpression(Activity toGenerate, boolean asClosure, List<Parameter> parameters) {
        target.generateActivityAsExpression(toGenerate, asClosure, parameters)
    }
    
}