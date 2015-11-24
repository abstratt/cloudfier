package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.ReadExtentAction

import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.target.jee.JPAHelper.*
import org.eclipse.uml2.uml.StructuredActivityNode

class JPQLQueryActionGenerator extends AbstractQueryActionGenerator {
	new(IRepository repository) {
        super(repository)
    }

	override generateReadExtentAction(ReadExtentAction action) {
         if (action.result.targetAction.trivialFlowDownstream) 
        	'''SELECT DISTINCT «action.result.alias» FROM «action.classifier.toJavaType» «action.result.alias»'''
    	 else
    	     '''FROM «action.classifier.toJavaType» «action.result.alias»'''		
	}
	
	override generateCollectionSelect(CallOperationAction action) {
		val predicate = action.arguments.head.sourceClosure
        ''' 
            «action.target.sourceAction.generateAction» «IF action.target.sourceAction.groupedUpstream»HAVING
                «predicate.generateHavingPredicate(action)» 
            «ELSE»WHERE
                «predicate.generateSelectPredicate»
            «ENDIF»
        '''
	}
	
	override generateCollectionCollect(CallOperationAction action) {
        // if the mapping returns a tuple, this is a projection
        // if the mapping returns an entity, this is a join, as defined by the traversal in the mapping
        // what other cases are there?
        val mapping = action.arguments.head.sourceClosure
        val sourceType = mapping.closureInputParameter.type
        val targetType = mapping.closureReturnParameter.type
//        if (targetType.entity)
//        ''' 
//            «action.target.sourceAction.generateAction».join(
//                «mapping.generateJoin»
//            )
//        '''
//        else
        '''
            SELECT «mapping.generateProjection» «action.target.sourceAction.generateAction»
        '''
	}
	
    override generateCollectionGroupBy(CallOperationAction action) {
        val mapping = action.arguments.head.sourceClosure
        ''' 
            «action.target.sourceAction.generateAction» GROUP BY
                «mapping.generateGroupByMapping»
        '''
    }
	
    override generateCollectionExists(CallOperationAction action) {
        val predicate = action.arguments.head.sourceClosure
        ''' 
            SELECT CASE WHEN COUNT(«action.target.alias») > 0 THEN TRUE ELSE FALSE END «action.target.generateAction» WHERE «predicate.generateSelectPredicate»
        '''
    }
    
    override generateCollectionIsEmpty(CallOperationAction action) {
        ''' 
            SELECT COUNT(«action.target.alias») = 0 «action.target.generateAction»
        '''
    }	
	
	override generateCollectionSize(CallOperationAction action) {
		'''SELECT COUNT(«action.target.alias») «action.target.generateAction»'''
	}
	
	override generateCollectionMax(CallOperationAction action) {
		generateAggregatorFunction(action, "MAX")
	}
	
	override generateCollectionMin(CallOperationAction action) {
		generateAggregatorFunction(action, "MIN")
	}

	override generateCollectionSum(CallOperationAction action) {
		generateAggregatorFunction(action, "SUM")
	}
	
	override generateCollectionAverage(CallOperationAction action) {
		generateAggregatorFunction(action, "AVG")
	}
    
    override generateGroupingGroupCollect(CallOperationAction action) {
        val collector = action.arguments.head.sourceClosure
        '''SELECT «collector.generateGroupProjection» «action.target.generateAction»'''
    }
	
	private def CharSequence generateAggregatorFunction(CallOperationAction action, String jpqlFunction) {
		val projection = action.arguments.head.sourceClosure
		'''SELECT «jpqlFunction»(«projection.generateProjection») «action.target.generateAction»'''
	}
	
	override createFilterActionGenerator(IRepository repository) {
		new JPQLFilterActionGenerator(repository)
	}
	
	override createGroupByActionGenerator(IRepository repository) {
		new JPQLGroupByActionGenerator(repository)
	}
	
	override createGroupProjectionFilterActionGenerator(IRepository repository, StructuredActivityNode node) {
		new JPQLGroupProjectionFilterActionGenerator(repository, node)
	}
	
	override createJoinActionGenerator(IRepository repository) {
		throw new UnsupportedOperationException("TODO: auto-generated method stub")
	}
	
	override createProjectionActionGenerator(IRepository repository) {
		new JPQLProjectionActionGenerator(repository)
	}
	
}