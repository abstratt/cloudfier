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
	
	override generateCollectionSize(CallOperationAction action) {
		'''SELECT COUNT(«action.target.alias») «action.target.generateAction»'''
	}
	
	override generateCollectionMax(CallOperationAction action) {
		val projection = action.arguments.head.sourceClosure
		'''SELECT MAX(«projection.generateProjection») «action.target.generateAction»'''
	}
	
	override createFilterActionGenerator(IRepository repository) {
		new JPQLFilterActionGenerator(repository)
	}
	
	override createGroupByActionGenerator(IRepository repository) {
		throw new UnsupportedOperationException("TODO: auto-generated method stub")
	}
	
	override createGroupProjectionFilterActionGenerator(IRepository repository, StructuredActivityNode node) {
		throw new UnsupportedOperationException("TODO: auto-generated method stub")
	}
	
	override createJoinActionGenerator(IRepository repository) {
		throw new UnsupportedOperationException("TODO: auto-generated method stub")
	}
	
	override createProjectionActionGenerator(IRepository repository) {
		new JPQLProjectionActionGenerator(repository)
	}
	
}