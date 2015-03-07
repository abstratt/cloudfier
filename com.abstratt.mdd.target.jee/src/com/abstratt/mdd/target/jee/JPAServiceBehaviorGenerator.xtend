package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.target.jse.PlainEntityBehaviorGenerator
import com.abstratt.mdd.target.jse.PlainJavaBehaviorGenerator
import org.eclipse.uml2.uml.Action
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.AddVariableValueAction
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.Classifier
import org.eclipse.uml2.uml.CreateObjectAction
import org.eclipse.uml2.uml.LiteralNull
import org.eclipse.uml2.uml.LiteralString
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.ReadExtentAction
import org.eclipse.uml2.uml.ReadLinkAction
import org.eclipse.uml2.uml.ReadStructuralFeatureAction
import org.eclipse.uml2.uml.ReadVariableAction
import org.eclipse.uml2.uml.TestIdentityAction
import org.eclipse.uml2.uml.ValueSpecification
import org.eclipse.uml2.uml.ValueSpecificationAction

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.ActivityUtils.*

class JPAServiceBehaviorGenerator extends JPABehaviorGenerator {
    PlainJavaBehaviorGenerator plainJavaBehaviorGenerator
    
    new(IRepository repository) {
        super(repository)
        this.plainJavaBehaviorGenerator = new PlainJavaBehaviorGenerator(repository)
    }
    
    override CharSequence generateProviderReference(Classifier context, Classifier provider) {
        if (context == provider)
            'this'
        else
            '''«provider.name.toFirstLower»Service'''
    }

    override generateReadExtentAction(ReadExtentAction action) {
        val core = '''cb.createQuery(«action.classifier.name».class)'''
        if (action.targetAction.collectionOperation) {
            return core
        }
        core
    }
    
    override generateCreateObjectAction(CreateObjectAction action) {
        val basicCreation = '''new «action.classifier.name»()'''
        if (action.classifier.entity)
            '''entityManager.merge(«basicCreation»)'''
        else
            basicCreation
    }
    
    override generateCollectionOperationCall(CallOperationAction action) {
        if (action.plainCollectionOperation) {
            return plainJavaBehaviorGenerator.generateCollectionOperationCall(action)
        }
        
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
            case 'any':
                generateCollectionSelect(action)
            //            case 'collect': generateCollectionCollect(action)
            //            case 'reduce': generateCollectionReduce(action)
            //            case 'groupBy': generateCollectionGroupBy(action)
            default: '''«if(operation.getReturnResult != null) 'null' else ''» /*Unsupported Collection operation: «operation.
                name»*/'''
        }
        if (!action.results.empty && action.targetAction.collectionOperation)
            core
        else {
            // end of line
            val executeMethod = if (action.operation.getReturnResult() != null) {
                if (operation.getReturnResult().multivalued) 'getResultList' else 'getSingleResult'
            } else {
                'executeUpdate'
            }
            '''
            entityManager.createQuery(
                «core»
            ).«executeMethod»()
            '''
        }
    }
    
    def boolean isPlainCollectionOperation(Action action) {
        if (!action.collectionOperation)
            return false
        val asCallAction = action as CallOperationAction
        val sourceAction = asCallAction.target.sourceAction
        if (sourceAction instanceof ReadLinkAction || sourceAction instanceof ReadStructuralFeatureAction) {
//            val end = sourceAction.endData.get(0).end.otherEnd
//            val navigable = end != null && end.navigable
//            return navigable
            return true
        }
        return sourceAction.collectionOperation && sourceAction.plainCollectionOperation 
    }

    def generateCollectionSize(CallOperationAction action) {

        // two possibilities here: a simple in-memory collection traversal, or a query
        super.generateCollectionOperationCall(action)
    }

    override generateCollectionSelect(CallOperationAction action) {
        val predicate = action.arguments.head.sourceClosure
        ''' 
            «action.target.sourceAction.generateAction».where(
                «predicate.generateSelectPredicate»
            )
        '''
    }

    def generateSelectPredicate(Activity predicate) {

        //TODO taking only first statement into account
        val statementAction = predicate.rootAction.findStatements.head
        generateFilterAction(statementAction)
    }

    def dispatch generateFilterAction(ValueSpecificationAction action) {
        '''«generateFilterValue(action.value)»'''
    }

    def dispatch CharSequence generateFilterAction(AddVariableValueAction action) {
        if (action.variable.name == '')
            generateFilterAction(action.value.sourceAction)
        else
            unsupportedElement(action)
    }
    
    def boolean isBoolean(Action toCheck) {
        val outputs = toCheck.outputs
        return (outputs.size == 1) && outputs.get(0).type.name == "Boolean"
    }
    
    def dispatch CharSequence generateFilterAction(ReadLinkAction action) {
        val property = action.endData.get(0).end.otherEnd
        val classifier = property.class_
        '''cq.from(«classifier.name».class).get("«property.name»")'''
    }

    def dispatch CharSequence generateFilterAction(ReadStructuralFeatureAction action) {
        val isCondition = action.result.type.name == 'Boolean'
        val property = action.structuralFeature as Property
        val classifier = property.class_
        if (isCondition) {
            if (property.derived) {
                val derivation = property.defaultValue.resolveBehaviorReference as Activity
                derivation.generateFilter
            } else '''cb.isTrue(cq.from(«classifier.name».class).get("«property.name»"))'''
        } else
            '''cq.from(«classifier.name».class).get("«property.name»")'''
    }
    
    def dispatch CharSequence generateFilterAction(CallOperationAction action) {
        if (action.operation.static) {
            return switch (action.operation.class_.name) {
                case 'Date' : switch (action.operation.name) {
                    case 'today' : 'new Date()'
                    default : unsupportedElement(action, action.operation.name)    
                }
                default : unsupportedElement(action, action.operation.name)
            }
        }
        '''
        cb.«action.operation.toQueryOperator»(
            «action.inputs.map[sourceAction.generateFilterAction].join(',\n')»
        )'''    
    }
    
    def dispatch CharSequence generateFilterAction(TestIdentityAction action) {
        val left = generateFilterAction(action.first.sourceAction)
        val right = generateFilterAction(action.second.sourceAction)
        '''cb.equal(«left», «right»)'''
    }
    
    def dispatch CharSequence generateFilterAction(ReadVariableAction action) {
        '''cb.parameter(«action.variable.type.toJavaType».class, "«action.variable.name»")'''
    }
    
        
    private def toQueryOperator(Operation operation) {
        switch (operation.name) {
            case 'and': 'and'
            case 'or': 'or'
            // workaround - not is mapped to ne(true)
            case 'not': 'not'
            case 'notEquals': 'notEqual'
            case 'lowerThan': 'lessThan'
            case 'greaterThan': 'greaterThan'
            case 'lowerOrEquals': 'lessThanOrEqualTo'
            case 'greaterOrEquals': 'greaterThanOrEqualTo'
            case 'equals': 'equal'
            case 'same': 'equal'
            case 'size': 'size'
            default: '''/*unknown:«operation.name»*/«operation.name»'''
        }
    }
    
    

    def CharSequence generateFilter(Activity predicate) {
        //TODO taking only first statement into account
        val statementAction = predicate.rootAction.findStatements.head
        generateFilterAction(statementAction)
    }

    def generateFilterValue(ValueSpecification value) {
        switch (value) {
            // the TextUML compiler maps all primitive values to LiteralString
            LiteralString:
                '''cb.literal(«switch (value.type.name) {
                    case 'String': '''"«value.stringValue»"'''
                    default:
                        value.stringValue
                }»)'''
            LiteralNull: {
                val targetPin = (value.eContainer as ValueSpecificationAction).result.target
                val expectedType = targetPin.type
                val expectedJavaType = if (expectedType.name == 'NullType') 'null' else '''«expectedType.toJavaType».class''' 
                '''cb.nullLiteral(«expectedJavaType»)'''
            }
            default:
                unsupportedElement(value)
        }
    }

}
