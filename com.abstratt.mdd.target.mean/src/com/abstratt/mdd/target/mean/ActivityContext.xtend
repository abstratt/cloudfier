package com.abstratt.mdd.target.mean

import java.util.LinkedHashMap
import org.eclipse.uml2.uml.Action
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.ActivityGroup

import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import java.util.List
import com.google.common.base.Function
import org.eclipse.uml2.uml.StructuredActivityNode
import org.eclipse.uml2.uml.ReadStructuralFeatureAction
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.ReadVariableAction
import org.eclipse.uml2.uml.AddVariableValueAction
import org.eclipse.uml2.uml.AddStructuralFeatureValueAction
import java.util.Collection
import org.eclipse.uml2.uml.Variable
import org.eclipse.uml2.uml.CreateObjectAction

/**
 * Some actions are better performed asynchronously, others synchronously.
 * Which ones are which depend on the nature of the action, their inputs, and target platform.
 * 
 * When generating code for actions in an activity, we need to first perform
 * an analysis and determine how actions need to be grouped in stages. A stage is a sequence of computations
 * that can be performed synchronously.  
 * 
 * Examples of actions that should be performed asynchronously:
 * 
 * 
 * - reading the extent - the results are available asynchronously
 * - traversing a link that is not an aggregation (otherwise the child objects may be available right away)
 * - calling an action operation (always, as it mutates the state of the object, which must be saved before returning)
 * - calling a query that invokes reading extent or traversing relationships, as the extent/related objects
 * are only going to be available at a later time
 * - saving an object
 * 
 * Examples of scenarios where it is fine (and desirable) that actions be executed synchronously:
 * - reading an attribute
 * - writing an attribute
 * - creating a link
 * - reading a variable 
 * 
 * Whenever an object flow originates from an action that is asynchronous:
 * - the code generated for the producing action must *return* a promise
 * - the code generated for the consuming action must *expect* a promise
 * 
 * That has a major impact on how the code looks like (using normal data flow feature in the language, vs. wrapping everything
 * with pipelined callbacks.
 * 
 * In order to simplify the code generation, we create the abstraction called a "stage". 
 * Stages are sets of actions that can be executed serially.
 * Stages can be as small as having a single action, but they can also have entire statements, or even sequences of statements.
 * Stages can have multiple incoming edges from other stages.
 * Stages form acyclic directed graphs (trees).
 */
class ActivityContext {

    static class Stage {
        public final Action rootAction
        public final String alias
        public final Stage parentStage
        public final List<Stage> substages = newLinkedList()
        boolean generated = false

        new(Stage parentStage, Action rootAction, String alias) {
            this.rootAction = rootAction
            this.alias = alias
            this.parentStage = parentStage
            if (parentStage != null)
                parentStage.substages.add(this)
        }
        
        def boolean isLastInParent() {
            root || parentStage.substages.last == this 
        }
        
        def boolean isLeaf() {
            substages.empty
        }
        
        def boolean isRoot() {
            parentStage == null
        }
        
        def Iterable<Stage> getAncestors() {
            if (root) #[] else #[parentStage] + parentStage.ancestors
        }
        
        def boolean isLastInPipeline(boolean mustBeLeaf) {
            (!mustBeLeaf || isLeaf) && (root || (lastInParent && parentStage.isLastInPipeline(false)))
        }

        def boolean isProducer() { 
            !this.rootAction.outputs.empty
        }
        
        def boolean isGenerated() { 
            this.generated
        }
        
        def void markGenerated() {
            this.generated = true
        }
        
        override String toString() {
            '''
            «alias»«IF !substages.empty» [
                «substages.map[toString].join('\n')»
            ]«ENDIF»
            '''
        }
    }

    private ApplicationContext application
    public final Activity activity
    public final LinkedHashMap<Action, Stage> stagedActions = newLinkedHashMap()
    public Stage rootStage
    public Stage currentStage

    /** 
     * @param application the application for this activity context
     * @param activity possibly null (if context has no behavior in the input model)
     */
    new(ApplicationContext application, Activity activity) {
        this.activity = activity
        this.application = application
    }

    def void stage(Action toStage, Function<?, ?> behavior) {
        if (isStaged(toStage)) {
            // do not execute the stage-sensitive behavior
            return
        }
        // start a stage if there are no staged actions or if the action is asynchronous or if it is a data sink or if the current stage has other substages
        val isNewStage = application.isAsynchronous(toStage) || isDataSink(toStage) || currentStage == null || !currentStage.substages.empty
        if (isNewStage)
            currentStage = newStage(toStage)
        // remember we already visited this action    
        stagedActions.put(toStage, currentStage)        
        try {
            behavior.apply(null)
        } finally {
            if (isNewStage)
                currentStage = currentStage.parentStage
        }
    }
    
    def newStage(Action action) {
        val alias = computeAlias(action)
        val newStage = new Stage(currentStage, action, alias)
        if (rootStage == null)
            rootStage = newStage
        return newStage
    }
    
    def String computeAlias(Action action) {
        switch (action) {
            ReadStructuralFeatureAction : '''«action.structuralFeature.name»'''
            AddStructuralFeatureValueAction : '''«action.structuralFeature.name»'''            
            ReadVariableAction : action.variable.name
            AddVariableValueAction : action.variable.name   
            CallOperationAction : '''«action.operation.name»Result'''
            CreateObjectAction : '''new«action.classifier.name»'''
            StructuredActivityNode : '''block'''            
            default : action.eClass.name.toFirstLower     
        }
    }
    
    private def boolean isStaged(Action toCheck) {
        stagedActions.containsKey(toCheck)
    }
    
    def Stage findStage(Action toCheck) {
        stagedActions.get(toCheck)
    }
    
    
    /**
     * Builds a pipeline of stages where actions that need to be asynchronous will lead
     * to new stages being generated.
     */
    def void buildPipeline(Action action) {
        // depth-first as producer stages should precede consumer ones
        stage(action, [
            // process inputs within the stage possibly created for the current action so nesting works as expected
            action.inputs.forEach[buildPipeline(it.sourceAction)]
            // if this is a container action, process children too!    
            if (action instanceof ActivityGroup)
                action.containedNodes.filter[it instanceof Action].forEach [
                    buildPipeline(it as Action)
                ]
            /* No return value */ 
            return null
        ])
    }
    
    def findVariables() {
        val Collection<Variable> found = newLinkedList()
        this.stagedActions.keySet.filter[it instanceof StructuredActivityNode].forEach[
            found.addAll((it as StructuredActivityNode).variables)
        ]
        return found
    }
    

}
