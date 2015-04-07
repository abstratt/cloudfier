package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.target.jse.PlainEntityBehaviorGenerator
import com.abstratt.mdd.target.jse.PlainJavaBehaviorGenerator
import org.eclipse.uml2.uml.Action
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.AddVariableValueAction
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.Class
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
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import static com.abstratt.mdd.core.util.MDDExtensionUtils.isCast
import static extension com.abstratt.mdd.core.util.FeatureUtils.*
import static extension com.abstratt.mdd.target.jee.JPAHelper.*
import org.eclipse.uml2.uml.*
import org.eclipse.uml2.uml.UMLPackage.Literals
import java.util.List

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
            '''new «provider.name.toFirstLower»Service'''
    }

    override generateAddVariableValueActionCore(AddVariableValueAction action) {
        generateQueryExecution(action.value, action.variable)
    }
    
    def CharSequence generateQueryExecution(InputPin input, MultiplicityElement output) {
        val query = input.findUpstreamAction(Literals.READ_EXTENT_ACTION) != null;
        val core = input.generateAction
        if (query) {
            val parameters = input.actionActivity.activityInputParameters
            '''
            entityManager.createQuery(
                «core»
            )«parameters.map['''.setParameter("«name»", «name»)'''].join()».«generateQueryExecutionMethod(output)»
            '''
        } else
            core
    }
    
    override generateReadExtentAction(ReadExtentAction action) {
        val classifier = action.classifier
        if (!classifier.entity)
            return super.generateReadExtentAction(action)
        new QueryActionGenerator(repository).generateReadExtentAction(action)    
    }
    
    override generateJavaMethodBody(Activity activity) {
        val context = new SimpleContext("context")
        enterContext(context)
        try {
            return doGenerateJavaMethodBody(activity)
        } finally {
            leaveContext(context)
        }
    }
    
    def doGenerateJavaMethodBody(Activity activity) {
        
        val extents = activity.rootAction.findMatchingActions(Literals.READ_EXTENT_ACTION).filter(typeof(ReadExtentAction));
        // naively assume only one extent
        val sourceType = extents.head?.classifier
        val resultType = activity.closureReturnParameter?.type
        val resultTypeName = resultType.toJavaType
        val usedEntities = activity.rootAction.findMatchingActions(Literals.ACTION).map[inputs.map[type].filter[entity]].flatten.toSet
        '''
            «IF (sourceType != null)»
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<«resultTypeName»> cq = cb.createQuery(«resultTypeName».class);
            «usedEntities.map['''
            Root<«name»> «alias» = cq.from(«name».class);
            '''].join»
            
            «ENDIF»
            «super.generateJavaMethodBody(activity)»
        '''
    }
    
    override generateCollectionOperationCall(CallOperationAction action) {
        if (action.plainCollectionOperation) {
            return plainJavaBehaviorGenerator.generateCollectionOperationCall(action)
        }
        new QueryActionGenerator(repository).generateCollectionOperationCall(action)
    }
    
    override generateGroupingOperationCall(CallOperationAction action) {
        new QueryActionGenerator(repository).generateGroupingOperationCall(action)
    }
    
//        override generateCollectionOperationCall(CallOperationAction action) {
////        if (action.plainCollectionOperation) {
////            return plainJavaBehaviorGenerator.generateCollectionOperationCall(action)
////        }
//        
//        val operation = action.operation
//        val core = switch (operation.name) {
//            case 'size':
//                generateCollectionSize(action)
//            //            case 'includes': '''«generateAction(action.target)».contains(«action.arguments.head.generateAction»)'''
//            //            case 'isEmpty': '''«generateAction(action.target)».isEmpty()'''
//            //            case 'sum': generateCollectionSum(action)
//            //            case 'one': generateCollectionOne(action)
//            //            case 'asSequence' : '''«IF !action.target.ordered»new ArrayList<«action.target.type.toJavaType»>(«ENDIF»«action.target.generateAction»«IF !action.target.ordered»)«ENDIF»''' 
//            //            case 'forEach': generateCollectionForEach(action)
//            case 'select':
//                generateCollectionSelect(action)
//            case 'collect':
//                generateCollectionCollect(action)                
//            case 'any':
//                generateCollectionSelect(action)
//            //            case 'reduce': generateCollectionReduce(action)
//            case 'groupBy': 
//                generateCollectionGroupBy(action)
//            default: '''«if(operation.getReturnResult != null) 'null' else ''» /*Unsupported Collection operation: «operation.
//                name»*/'''
//        }
//        core
//    }
//    
    
    override CharSequence generateJavaMethodParameter(Parameter parameter) {
        val parameterType = if (parameter.multivalued) '''CriteriaQuery<«parameter.type.toJavaType»>''' else
                parameter.type.toJavaType
        '''«parameterType» «parameter.name»'''
    }
    
    override generateOperationReturnType(Operation operation) {
        // methods returning collections will usually return lists (due to Query#getResultList())
        val result = operation.getReturnResult()
        if (result?.multivalued)
            '''Collection<«result.type.toJavaType»> '''
        else
            super.generateOperationReturnType(operation)
    }
    

    
//    
//    def dispatch CharSequence generateGroupProjectionAction(StructuredActivityNode action) {
//        if (action.objectInitialization) {
//            val outputType = action.structuredNodeOutputs.head.type as Classifier
//            val List<CharSequence> projections = newLinkedList()
//            outputType.allAttributes.forEach[attribute, i |
//                projections.add('''«attribute.type.alias».alias("«action.structuredNodeInputs.get(i).name»")''')
//            ]
//            '''«projections.join(', ')»'''
//        } else if (isCast(action)) {
//            action.inputs.head.sourceAction.generateGroupProjectionAction
//        } else
//            unsupportedElement(action)
//    }
//    
//    def dispatch CharSequence generateGroupProjectionAction(ReadStructuralFeatureAction action) {
////        val property = action.structuralFeature as Property
////        val classifier = action.object.type
////        '''«classifier.alias».get("«property.name»")'''
//        '''"«action.structuralFeature.name»"'''
//    }
//    
//    
//    
//    def dispatch CharSequence generateGroupProjectionAction(AddVariableValueAction action) {
//        if (action.variable.name == '')
//            generateGroupProjectionAction(action.value.sourceAction)
//        else
//            unsupportedElement(action)
//    }
    
    
//
//    def dispatch generateFilterAction(ValueSpecificationAction action) {
//        '''«generateFilterValue(action.value)»'''
//    }
//    
//    def dispatch CharSequence generateFilterAction(ReadSelfAction action) {
//        '''«context.generateCurrentReference».getId()'''
//    }
//    
//    def dispatch CharSequence generateJoinAction(ReadStructuralFeatureAction action) {
//        '''join("«action.structuralFeature.name»")'''
//    }
//    
//    def dispatch CharSequence generateJoinAction(ReadLinkAction action) {
//        '''join("«action.endData.get(0).end.name»")'''
//    }
//    
//    def dispatch CharSequence generateJoinAction(StructuredActivityNode action) {
//        if (action.objectInitialization) {
//            val outputType = action.structuredNodeOutputs.head.type as Classifier
//            val List<CharSequence> projections = newLinkedList()
//            outputType.allAttributes.forEach[attribute, i |
//                projections.add('''«attribute.type.alias».get("«action.structuredNodeInputs.get(i).name»")''')
//            ]
//            '''cq.multiselect(«projections.join(', ')»)'''
//        } else if (isCast(action)) {
//            action.inputs.head.sourceAction.generateJoinAction
//        } else
//            unsupportedElement(action)
//    }
//    
//    def dispatch CharSequence generateJoinAction(AddVariableValueAction action) {
//        action.value.sourceAction.generateJoinAction
//    }
    
//    def boolean isBoolean(Action toCheck) {
//        val outputs = toCheck.outputs
//        return (outputs.size == 1) && outputs.get(0).type.name == "Boolean"
//    }
//    
//
//    def dispatch CharSequence generateFilterAction(ReadLinkAction action) {
//        val property = action.endData.get(0).end.otherEnd
//        val targetType = action.endData.get(0).value.type
//        '''«targetType.alias».get("«property.name»")'''
//    }
//    
//    def dispatch CharSequence generateFilterAction(ReadStructuralFeatureAction action) {
//        val isCondition = action.result.type.name == 'Boolean'
//        val property = action.structuralFeature as Property
//        val classifier = action.object.type
//        if (isCondition) {
//            if (property.derived) {
//                val derivation = property.defaultValue.resolveBehaviorReference as Activity
//                derivation.generateFilter
//            } else '''cb.isTrue(«classifier.alias».get("«property.name»"))'''
//        } else
//            '''«classifier.alias».get("«property.name»")'''
//    }
//    
//    def dispatch CharSequence generateFilterAction(AddVariableValueAction action) {
//        if (action.variable.name == '')
//            generateFilterAction(action.value.sourceAction)
//        else
//            unsupportedElement(action)
//    }
//    
//    def dispatch CharSequence generateFilterAction(CallOperationAction action) {
//        if (action.operation.static) {
//            return switch (action.operation.owningClassifier.name) {
//                case 'Date' : switch (action.operation.name) {
//                    case 'today' : 'cb.currentDate()'
//                    case 'now' : 'cb.currentTime()'
//                    default : unsupportedElement(action, action.operation.name)    
//                }
//                default : unsupportedElement(action, action.operation.name)
//            }
//        }
//        
//        val asQueryOperator = action.operation.toQueryOperator
//        if (asQueryOperator != null)
//            return '''
//            cb.«action.operation.toQueryOperator»(
//                «action.inputs.map[sourceAction.generateFilterAction].join(',\n')»
//            )'''
//        else
//            unsupportedElement(action.operation)
//    }
//    
//    def dispatch CharSequence generateFilterAction(TestIdentityAction action) {
//        val left = generateFilterAction(action.first.sourceAction)
//        val right = generateFilterAction(action.second.sourceAction)
//        '''cb.equal(«left», «right»)'''
//    }
//    
//    def dispatch CharSequence generateFilterAction(ReadVariableAction action) {
//        '''cb.parameter(«action.variable.type.toJavaType».class, "«action.variable.name»")'''
//    }
//    
        
//    private def toQueryOperator(Operation operation) {
//        switch (operation.name) {
//            case 'and': 'and'
//            case 'or': 'or'
//            // workaround - not is mapped to ne(true)
//            case 'not': 'not'
//            case 'notEquals': 'notEqual'
//            case 'lowerThan': 'lessThan'
//            case 'greaterThan': 'greaterThan'
//            case 'lowerOrEquals': 'lessThanOrEqualTo'
//            case 'greaterOrEquals': 'greaterThanOrEqualTo'
//            case 'equals': 'equal'
//            case 'same': 'equal'
//            case 'size': 'size'
//            default: null
//        }
//    }

//    def generateFilterValue(ValueSpecification value) {
//        switch (value) {
//            // the TextUML compiler maps all primitive values to LiteralString
//            LiteralString:
//                '''cb.literal(«switch (value.type.name) {
//                    case 'String': '''"«value.stringValue»"'''
//                    default:
//                        value.stringValue
//                }»)'''
//            LiteralNull:
//                switch (value) {
//                    case value.isVertexLiteral : 
//                        '''«value.toJavaType».«value.resolveVertexLiteral.name»'''
//                    case (value.eContainer instanceof Action) && (value.eContainer as Action).nullValue : {
//                        val targetPin = (value.eContainer as Action).outputs.head.target
//                        val expectedType = targetPin.type
//                        val expectedJavaType = if (expectedType.name == 'NullType') 'null' else '''«expectedType.toJavaType».class''' 
//                        '''cb.nullLiteral(«expectedJavaType»)'''
//                    }
//                    default : unsupportedElement(value)
//                }
//            default:
//                unsupportedElement(value)
//        }
//    }

}
