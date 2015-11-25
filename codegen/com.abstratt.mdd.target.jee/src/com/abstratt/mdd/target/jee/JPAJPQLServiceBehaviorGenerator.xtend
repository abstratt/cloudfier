package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.target.jse.PlainJavaBehaviorGenerator
import java.util.Collection
import java.util.Map
import java.util.function.Supplier
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.AddVariableValueAction
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.Classifier
import org.eclipse.uml2.uml.InputPin
import org.eclipse.uml2.uml.MultiplicityElement
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.OutputPin
import org.eclipse.uml2.uml.Parameter
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.ReadExtentAction
import org.eclipse.uml2.uml.UMLPackage.Literals
import org.eclipse.uml2.uml.ValueSpecificationAction

import static com.abstratt.mdd.target.jee.JPAHelper.*

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*

class JPAJPQLServiceBehaviorGenerator extends JPABehaviorGenerator {
	PlainJavaBehaviorGenerator plainJavaBehaviorGenerator

	new(IRepository repository) {
		super(repository)
		this.plainJavaBehaviorGenerator = new PlainJavaBehaviorGenerator(repository)
	}

	override CharSequence generateProviderReference(Classifier context, Classifier provider) {
		if (context == provider)
			'this'
		else '''new «provider.name.toFirstUpper»Service()'''
	}
	
	override generateJavaMethodBody(Activity activity) {
		return runInContext(new SimpleContext("context"),[ super.generateJavaMethodBody(activity) ])
	}

	override generateAddVariableValueActionCore(AddVariableValueAction action) {
		generateQueryExecution(action.value, action.variable)
	}
	
	private def CharSequence generateQueryExecution(InputPin input, MultiplicityElement output) {
		runInContext(new SimpleContext("context"), [ doGenerateQueryExecution(input, output) ])
	}

	def CharSequence doGenerateQueryExecution(InputPin input, MultiplicityElement output) {
		val query = input.findUpstreamAction(Literals.READ_EXTENT_ACTION) != null;
		val core = input.generateAction
		if (query) {
			val parameters = newLinkedHashMap()
			parameters.putAll(input.actionActivity.activityInputParameters.map[name].toInvertedMap[it])
	    	val activity = input.actionActivity
			val hasSelf = (activity.specification != null && !activity.specification.static) || (activity.derivation && activity.derivationContext instanceof Property && !(activity.derivationContext as Property).static)
			if (hasSelf) {
				parameters.put("context", "context")				
			}
			'''
				getEntityManager().createQuery(
				    "«core.toString().trim().replaceAll('[\n\t ]+', ' ')»", «input.type.toJavaType».class
				)«parameters.entrySet.map['''.setParameter("«it.key»", «it.value»)'''].join()».«generateQueryExecutionMethod(output)»
			'''
		} else
			core
	}

	override generateReadExtentAction(ReadExtentAction action) {
		val classifier = action.classifier
		if (!classifier.entity)
			return super.generateReadExtentAction(action)
		new JPQLQueryActionGenerator(repository).generateReadExtentAction(action)
	}

//	override generateActivityRootAction(Activity activity) {
//		val hasSelf = (activity.specification != null && !activity.specification.static) || (activity.derivation && activity.derivationContext instanceof Property && !(activity.derivationContext as Property).static)
//		val CharSequence self = if (hasSelf) 'context' else null
//		ActivityContext.generateInNewContext(activity, [ self ] as Supplier<CharSequence>, [
//			super.generateActivityRootAction(activity)
//		])
//	}
	
	override generateCollectionOperationCall(CallOperationAction action) {
		if (action.plainCollectionOperation) {
			return plainJavaBehaviorGenerator.generateCollectionOperationCall(action)
		}
		new JPQLQueryActionGenerator(repository).generateCollectionOperationCall(action)
	}

	override generateGroupingOperationCall(CallOperationAction action) {
		new JPQLQueryActionGenerator(repository).generateGroupingOperationCall(action)
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
