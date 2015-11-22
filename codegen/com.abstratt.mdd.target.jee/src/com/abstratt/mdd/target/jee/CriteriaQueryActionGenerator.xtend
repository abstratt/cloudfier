package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.target.jse.AbstractJavaBehaviorGenerator
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.Type
import org.eclipse.uml2.uml.Element
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.ReadLinkAction
import org.eclipse.uml2.uml.Action
import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import static com.abstratt.mdd.core.util.MDDExtensionUtils.isCast
import static extension com.abstratt.mdd.core.util.FeatureUtils.*
import static extension com.abstratt.mdd.target.jee.JPAHelper.*
import com.abstratt.mdd.target.jse.PlainJavaBehaviorGenerator
import org.eclipse.uml2.uml.ReadStructuralFeatureAction
import org.eclipse.uml2.uml.ReadExtentAction
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.AddVariableValueAction
import org.eclipse.uml2.uml.InputPin
import org.eclipse.uml2.uml.MultiplicityElement
import org.eclipse.uml2.uml.UMLPackage.Literals
import org.eclipse.uml2.uml.CallAction
import org.eclipse.uml2.uml.ReadSelfAction
import org.eclipse.uml2.uml.StructuredActivityNode

final class CriteriaQueryActionGenerator extends AbstractQueryActionGenerator {
    
    new(IRepository repository) {
        super(repository)
    }

    override unsupported(CharSequence message) {
        '''«super.unsupported(message)» - «class.simpleName»'''
    }
    
    override generateCollectionOperationCall(CallOperationAction action) {
        val operation = action.operation
        val core = switch (operation.name) {
            case 'max':
                generateCollectionMax(action)
			case 'min':
                generateCollectionMin(action)                
            case 'size':
                generateCollectionSize(action)
            case 'select':
                generateCollectionSelect(action)
            case 'exists':
                generateCollectionExists(action)
            case 'count':
                generateCollectionCount(action)
            case 'isEmpty':
                generateCollectionIsEmpty(action)
            case 'collect':
                generateCollectionCollect(action)                
            case 'any':
                generateCollectionSelect(action)
            //            case 'reduce': generateCollectionReduce(action)
            case 'groupBy': 
                generateCollectionGroupBy(action)
            default: '''«if(operation.getReturnResult != null) 'null' else ''» /*Unsupported Collection operation: «operation.
                name»*/'''
        }
        core
    }
    
    override generateCollectionCollect(CallOperationAction action) {
        // if the mapping returns a tuple, this is a projection
        // if the mapping returns an entity, this is a join, as defined by the traversal in the mapping
        // what other cases are there
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
            «action.target.sourceAction.generateAction».multiselect(
                «mapping.generateProjection»
            )
        '''
    }
    
    override generateCollectionExists(CallOperationAction action) {
        val predicate = action.arguments.head.sourceClosure
        ''' 
            «action.target.sourceAction.generateAction».«IF action.target.sourceAction.groupedUpstream»having(
                «predicate.generateHavingPredicate(action)» 
            )«ELSE»where(
                «predicate.generateSelectPredicate»
            ).multiselect(
				cb.selectCase()
					.when(cb.gt(cb.count(«action.target.alias»), cb.literal(0)), true)
					.otherwise(false)
			)«ENDIF»
        '''
    }
    
    override generateCollectionCount(CallOperationAction action) {
        val predicate = action.arguments.head.sourceClosure
        ''' 
            «action.target.sourceAction.generateAction».«IF action.target.sourceAction.groupedUpstream»having(
                «predicate.generateHavingPredicate(action)» 
            )«ELSE»where(
                «predicate.generateSelectPredicate»
            ).select(cb.count(«action.target.alias»))«ENDIF»
        '''
    }
    
    override generateCollectionIsEmpty(CallOperationAction action) {
    	/*
    	 * Two cases supported:
    	 * - checking the availability of elements in a stream or collection (!Stream#findAny().isPresent() or Collection#isEmpty())
    	 * - filtering based on the size of a related collection (JPA's isEmpty())
    	 * 
    	 * There is also a third case: we are downstream from a filter so we are basically doing an exists, but we won't support
    	 * that for now. Also, grouping may be its own case.
    	 */
 		'''
		(«action.target.sourceAction.generateAction».select(cb.count(«action.target.alias»)) == 0)
		'''
    }
    
    override generateCollectionSelect(CallOperationAction action) {
        val predicate = action.arguments.head.sourceClosure
        ''' 
            «action.target.sourceAction.generateAction».«IF action.target.sourceAction.groupedUpstream»having(
                «predicate.generateHavingPredicate(action)» 
            )«ELSE»where(
                «predicate.generateSelectPredicate»
            )«ENDIF»
        '''
    }
    
	override generateCollectionSize(CallOperationAction action) {
		'''
		«action.target.sourceAction.generateAction».select(cb.count(«action.target.alias»))
		'''
	}
	
	override generateCollectionMax(CallOperationAction action) {
		val projection = action.arguments.head.sourceClosure
		'''
		«action.target.sourceAction.generateAction».select(cb.max(«projection.generateProjection»))
		'''
	}
	
	override generateCollectionMin(CallOperationAction action) {
		val projection = action.arguments.head.sourceClosure
		'''
		«action.target.sourceAction.generateAction».select(cb.min(«projection.generateProjection»))
		'''
	}
    
    override generateCollectionGroupBy(CallOperationAction action) {
        val mapping = action.arguments.head.sourceClosure
        ''' 
            «action.target.sourceAction.generateAction».groupBy(
                «mapping.generateGroupByMapping»
            )
        '''
        
    }
    
    def private boolean isGroupedDownstream(Action action) {
        if (!action.collectionOperation)
            false
        else {
            val callOpAction = action as CallOperationAction
            callOpAction.operation.name == 'groupBy' || callOpAction.results.head.targetAction.groupedDownstream
        }
    }
    
    override generateReadExtentAction(ReadExtentAction action) {
        val isGrouped = action.result.targetAction.groupedDownstream
        // we do not issue a select here as it should only be done in some cases
        // and leaving it out seems to work 
        if (isGrouped) 'cq' else '''cq.distinct(true)'''
    }
    
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
    
    override generateGroupingGroupCollect(CallOperationAction action) {
        val collector = action.arguments.head.sourceClosure
        '''«action.target.generateAction».multiselect(«collector.generateGroupProjection»)'''
    }
    
	override createJoinActionGenerator(IRepository repository) {
		new CriteriaJoinActionGenerator(repository)
	}
    
	override createProjectionActionGenerator(IRepository repository) {
		new CriteriaProjectionActionGenerator(repository)
	}

	override createGroupByActionGenerator(IRepository repository) {
		new CriteriaGroupByActionGenerator(repository)
	}

	override createFilterActionGenerator(IRepository repository) {
		new CriteriaFilterActionGenerator(repository)
	}

	override createGroupProjectionFilterActionGenerator(IRepository repository, StructuredActivityNode projectingAction) {
		new CriteriaGroupProjectionFilterActionGenerator(repository, projectingAction)
	}
				
}