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
import java.util.Collection
import java.util.LinkedHashSet
import java.util.Map
import java.util.LinkedHashMap

class JPAServiceBehaviorGenerator extends JPABehaviorGenerator {
	PlainJavaBehaviorGenerator plainJavaBehaviorGenerator

	new(IRepository repository) {
		super(repository)
		this.plainJavaBehaviorGenerator = new PlainJavaBehaviorGenerator(repository)
	}

	override CharSequence generateProviderReference(Classifier context, Classifier provider) {
		if (context == provider)
			'this'
		else '''new «provider.name.toFirstLower»Service'''
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
				getEntityManager().createQuery(
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

	override generateActivityRootAction(Activity activity) {
		ActivityContext.generateInNewContext(activity, null, [
			super.generateActivityRootAction(activity)
		])
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

	/**
	 * Returns the entity-related actions for any activities in the given activity or any activity
	 * invoked from it.
	 * 
	 * @return a map where the key is the activity and the value is a collection of entity types
	 * found in actions within the activity  
	 */
	public def Map<Activity, Collection<Type>> collectUsedEntities(Map<Activity, Collection<Type>> collected,
		Activity activity) {
		val Collection<Action> found = activity.rootAction.findMatchingActions(Literals.ACTION)
		if (activity.closure) {
			collected.put(activity, activity.inputParameters.map[type].filter[entity || tupleType].toSet)
		} else {
			collected.put(null, found.map[(inputs + outputs).map[type].filter[entity || tupleType]].flatten.toSet)
		}
		// now recurse into closures    
		found.filter[it instanceof ValueSpecificationAction].map[it as ValueSpecificationAction].map[value].filter [
			behaviorReference
		].map[resolveBehaviorReference as Activity].forEach[collectUsedEntities(collected, it)]
		return collected
	}

	/**
	 * Returns the entity-instance producing output pins for any activities in the given activity or any activity
	 * invoked from it.
	 * 
	 * @return a map where the key is the activity and the value is a collection of entity producing output pins
	 * found within the activity  
	 */
	public def Map<Activity, Collection<OutputPin>> collectEntityProducingOutputs(
		Map<Activity, Collection<OutputPin>> collected, Activity activity) {
		val context = if (activity.closure) activity else null 
		collected.put(context, JPAHelper.findInstanceProducingActions(activity)
			.map[outputs.findFirst[type.entity || type.tupleType]]
			.filter[it != null].toList)
		// now recurse into closures    
		activity.rootAction.findMatchingActions(Literals.VALUE_SPECIFICATION_ACTION).
			map[it as ValueSpecificationAction].map[value].filter[behaviorReference].map [
				resolveBehaviorReference as Activity
			].forEach[collectEntityProducingOutputs(collected, it)]
		return collected
	}

	def doGenerateJavaMethodBody(Activity activity) {
		if (activity.closure)
			throw new IllegalArgumentException
		val resultType = activity.closureReturnParameter?.type
		val isJPAQuery = resultType != null && (activity.operation == null || activity.operation.query)
		'''
			«IF (isJPAQuery)»
			«generateJPAQueryVariables(activity, resultType)»
			«ENDIF»
			«super.generateJavaMethodBody(activity)»
		'''
	}
	
	private def CharSequence generateJPAQueryVariables(Activity activity, Type resultType) {
		val resultTypeName = resultType?.toJavaType
		val entityProducingOutputs = collectEntityProducingOutputs(new LinkedHashMap<Activity, Collection<OutputPin>>, activity)
		// null is used for the current activity, which is not a closure
		val analyzer = new DataFlowAnalyzer
		val localEntityProducingOutputs = entityProducingOutputs.get(null).map[analyzer.findSource(it)].toSet
		val entityProducingOutputsFromSubqueries = entityProducingOutputs.entrySet.filter[key != null].map[value].flatten.map[analyzer.findSource(it)].toSet.filter[
			!localEntityProducingOutputs.contains(it)
		]
		val collectionOutputsFromSubqueries = entityProducingOutputsFromSubqueries.filter[multivalued]
		val Map<String, List<OutputPin>> subQueryPivots = collectionOutputsFromSubqueries.groupBy[output | '''«output.type.name.toFirstLower»Subquery''']
		val subqueryTypes = subQueryPivots.entrySet.map[value.head.type].toSet
		'''
			CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
			CriteriaQuery<«resultTypeName»> cq = cb.createQuery(«resultTypeName».class);
			«subqueryTypes.map[subType | 
			    '''Subquery<«subType.name»> «subType.name.toFirstLower»Subquery = cq.subquery(«subType.name».class);'''            	
            ].join»
			«localEntityProducingOutputs.map[output | '''
			    Root<«output.type.name»> «JPAHelper.alias(output)» = cq.from(«output.type.name».class);
	        '''].join»
			«subQueryPivots.entrySet.map[pivot |
				val subQuery = pivot.key
				val anyPivot = pivot.value.head
				''' 
				Root<«anyPivot.type.name»> «JPAHelper.alias(anyPivot)» = «subQuery».from(«anyPivot.type.name».class);
	            '''].join
            »
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

	override CharSequence generateJavaMethodParameter(Parameter parameter) {
		val parameterType = if (parameter.multivalued) '''CriteriaQuery<«parameter.type.toJavaType»>''' else
				parameter.type.toJavaType
		'''«parameterType» «parameter.name»'''
	}

	override generateOperationReturnType(Operation operation) {
		// methods returning collections will usually return lists (due to Query#getResultList())
		val result = operation.getReturnResult()
		if (result?.multivalued) 
			'''Collection<«result.type.toJavaType»>'''
		else
			super.generateOperationReturnType(operation)
	}
}
