package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.target.base.IBasicBehaviorGenerator
import com.abstratt.mdd.target.jse.PlainJavaBehaviorGenerator
import org.eclipse.uml2.uml.Action
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.StructuredActivityNode

import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*

abstract class AbstractQueryActionGenerator extends PlainJavaBehaviorGenerator {
	new(IRepository repository) {
        super(repository)
    }
    
    def protected boolean isTrivialFlowDownstream(Action action) {
        if (!action.collectionOperation)
            true
        else {
            val callOpAction = action as CallOperationAction
            #{'select', 'any'}.contains(callOpAction.operation.name) && callOpAction.results.head.targetAction.trivialFlowDownstream
        }
    }	   
    
    def protected boolean isGroupedDownstream(Action action) {
        if (!action.collectionOperation)
            false
        else {
            val callOpAction = action as CallOperationAction
            callOpAction.operation.name == 'groupBy' || callOpAction.results.head.targetAction.groupedDownstream
        }
    }	    
	    
    def protected boolean isGroupedUpstream(Action action) {
        if (!(action instanceof CallOperationAction))
            false
        else {
            val callOpAction = action as CallOperationAction
            callOpAction.operation.name == 'groupBy' || callOpAction.target.sourceAction.groupedUpstream
        }
    }

    def generateHavingPredicate(Activity predicate, CallOperationAction action) {
        val upstreamGroupCollect = action.target.sourceAction.findUpstreamAction(
            [upstream | upstream instanceof CallOperationAction && (upstream as CallOperationAction).getOperation().getName().equals("groupCollect")]
        ) as CallOperationAction;
        val projector = upstreamGroupCollect.arguments.head.sourceAction.resolveBehaviorReference as Activity
        val projectingAction = projector.findSingleStatement.findUpstreamAction(
            [upstream | upstream instanceof StructuredActivityNode && (upstream as StructuredActivityNode).objectInitialization]
        ) as StructuredActivityNode
        createGroupProjectionFilterActionGenerator(repository, projectingAction).generateAction(predicate.findSingleStatement)
    }
	
	def abstract IBasicBehaviorGenerator createGroupProjectionFilterActionGenerator(IRepository repository, StructuredActivityNode node)


    def generateGroupProjection(Activity projection) {
        generateGroupCollectMapping(projection)
    }
    
    def generateJoin(Activity predicate) {
        createJoinActionGenerator(repository).generateAction(predicate.findSingleStatement)
    }
	
	def abstract IBasicBehaviorGenerator createJoinActionGenerator(IRepository repository)
    
    def generateProjection(Activity mapping) {
        createProjectionActionGenerator(repository).generateAction(mapping.findSingleStatement)
    }
	
	def abstract IBasicBehaviorGenerator createProjectionActionGenerator(IRepository repository)

    def generateGroupByMapping(Activity mapping) {
        createGroupByActionGenerator(repository).generateAction(mapping.findSingleStatement)
    }
    def generateGroupCollectMapping(Activity projection) {
        createGroupCollectActionGenerator(repository).generateAction(projection.findSingleStatement)
    }
	
	def IBasicBehaviorGenerator createGroupByActionGenerator(IRepository repository)
	def IBasicBehaviorGenerator createGroupCollectActionGenerator(IRepository repository)

    def generateSelectPredicate(Activity predicate) {
        createFilterActionGenerator(repository).generateAction(predicate.findSingleStatement)
    }
	
	def IBasicBehaviorGenerator createFilterActionGenerator(IRepository repository)
	
		
	override generateGroupingOperationCall(CallOperationAction action) {
        val operation = action.operation
        val core = switch (operation.name) {
            case 'groupCollect':
                generateGroupingGroupCollect(action)                
            default: '''«if(operation.getReturnResult != null) 'null' else ''» /*Unsupported Grouping operation: «operation.
                name»*/'''
        }
        core
    }
}