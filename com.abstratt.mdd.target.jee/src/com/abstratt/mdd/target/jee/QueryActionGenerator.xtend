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

final class QueryActionGenerator extends PlainJavaBehaviorGenerator {
    
    new(IRepository repository) {
        super(repository)
    }

    override unsupported(CharSequence message) {
        '''«super.unsupported(message)» - «class.simpleName»'''
    }  
    
    override generateCollectionOperationCall(CallOperationAction action) {
        val operation = action.operation
        val core = switch (operation.name) {
            case 'size':
                generateCollectionSize(action)
            //            case 'includes': '''«generateAction(action.target)».contains(«action.arguments.head.generateAction»)'''
            //            case 'isEmpty': '''«generateAction(action.target)».isEmpty()'''
            //            case 'sum': generateCollectionSum(action)
            //            case 'one': generateCollectionOne(action)
            //            case 'asSequence' : '''«IF !action.target.ordered»new ArrayList<«action.target.type.toJavaType»>(«ENDIF»«action.target.generateAction»«IF !action.target.ordered»)«ENDIF»''' 
            //            case 'forEach': generateCollectionForEach(action)
            case 'select':
                generateCollectionSelect(action)
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
        val mapping = action.arguments.head.sourceClosure
        val sourceType = mapping.closureInputParameter.type
        val targetType = mapping.closureReturnParameter.type
        ''' 
            «action.target.sourceAction.generateAction».join(
                «mapping.generateJoin»
            )
        '''
    }
    
    override generateCollectionSelect(CallOperationAction action) {
        val predicate = action.arguments.head.sourceClosure
        ''' 
            «action.target.sourceAction.generateAction».where(
                «predicate.generateSelectPredicate»
            )
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
    
    def private boolean isGrouped(Action action) {
        if (!action.collectionOperation)
            false
        else {
            val callOpAction = action as CallOperationAction
            callOpAction.operation.name == 'groupBy' || callOpAction.results.head.targetAction.grouped
        }
    }
    
    override generateReadExtentAction(ReadExtentAction action) {
        val isGrouped = action.result.targetAction.grouped 
        if (isGrouped) 'cq' else '''cq.select(«action.classifier.alias  »).distinct(true)'''
    }
    
    override generateGroupingOperationCall(CallOperationAction action) {
//        if (action.plainGroupingOperation) {
//            return plainJavaBehaviorGenerator.generateGroupingOperationCall(action)
//        }
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
    
    def generateGroupProjection(Activity predicate) {
        generateGroupByMapping(predicate)
    }
    
    def generateJoin(Activity predicate) {
        val statementAction = predicate.rootAction.findStatements.head
        new JoinActionGenerator(repository).generateAction(statementAction)
    }

    def generateGroupByMapping(Activity mapping) {
        //TODO taking only first statement into account
        val statementAction = mapping.rootAction.findStatements.head
        new GroupByActionGenerator(repository).generateAction(statementAction)
    }

    def generateSelectPredicate(Activity predicate) {
        new FilterActionGenerator(repository).generateFilter(predicate, true)
    }
}