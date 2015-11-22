package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.target.jse.PlainJavaBehaviorGenerator
import org.eclipse.uml2.uml.Action
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.StructuredActivityNode

import static extension com.abstratt.mdd.core.util.ActivityUtils.*

import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import static com.abstratt.mdd.core.util.MDDExtensionUtils.isCast
import com.abstratt.mdd.target.base.IBasicBehaviorGenerator

abstract class AbstractQueryActionGenerator extends PlainJavaBehaviorGenerator {
	new(IRepository repository) {
        super(repository)
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


    def generateGroupProjection(Activity predicate) {
        generateGroupByMapping(predicate)
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
	
	def IBasicBehaviorGenerator createGroupByActionGenerator(IRepository repository)

    def generateSelectPredicate(Activity predicate) {
        createFilterActionGenerator(repository).generateAction(predicate.findSingleStatement)
    }
	
	def IBasicBehaviorGenerator createFilterActionGenerator(IRepository repository)
	
}