package com.abstratt.kirra.mdd.target.base

import com.abstratt.kirra.mdd.core.KirraHelper
import com.abstratt.mdd.core.IRepository
import java.util.Collection
import java.util.List
import org.eclipse.emf.ecore.EClass
import org.eclipse.uml2.uml.Action
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Classifier
import org.eclipse.uml2.uml.Enumeration
import org.eclipse.uml2.uml.NamedElement
import org.eclipse.uml2.uml.Package
import org.eclipse.uml2.uml.ReadLinkAction
import org.eclipse.uml2.uml.ReadStructuralFeatureAction
import org.eclipse.uml2.uml.StateMachine
import org.eclipse.uml2.uml.Type
import org.eclipse.uml2.uml.UMLPackage

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.target.base.GeneratorUtils.*
import com.abstratt.mdd.target.base.GeneratorUtils
import org.eclipse.uml2.uml.Namespace
import java.util.Properties

abstract class AbstractGenerator {
    protected IRepository repository

    protected String applicationName

    protected Iterable<Class> entities
    
    protected Collection<Package> appPackages
	
	protected Iterable<Classifier> enumerations
	protected Iterable<StateMachine> stateMachines
	
	protected Properties repositoryProperties
    
    new(IRepository repository) {
        this.repository = repository
        this.repositoryProperties = repository.properties
        if (repository != null) {
            val topLevelPackages = repository.getTopLevelPackages(null)
			this.appPackages = topLevelPackages.applicationPackages
            this.entities = appPackages.entities
            this.enumerations = getEnumerations(topLevelPackages)
            this.stateMachines = appPackages.getTypes(UMLPackage.Literals.CLASS).map[it as Class].map[it.ownedBehaviors.filter(StateMachine)].flatten
            this.applicationName = KirraHelper.getApplicationName(repository)
        }
    }
    
    def String toJavaPackage(NamedElement package_) {
        JavaGeneratorUtils.toJavaPackage(package_)
    }
    
    def String toJavaQName(NamedElement package_) {
        JavaGeneratorUtils.toJavaQName(package_)
    }
    
    
    def boolean isCollectionOperation(Action toCheck) {
        if (toCheck instanceof CallOperationAction)
            return toCheck.target != null && toCheck.target.multivalued
        return false
    }
    
    def <T extends Type> List<T> getTypes(Collection<Package> packages, EClass type) {
        val List<T> result = newArrayList()
        for (Package current : packages) {
            for (Type it : current.getOwnedTypes())
                if (it.eClass == type)
                    result.add(it as T)
            result.addAll(getTypes(current.getNestedPackages(), type))
        }
        return result
    }
    
    
    def boolean isOperation(Action toCheck, String typeName, String... operationNames) {
        if (toCheck instanceof CallOperationAction)
        	return operationNames.contains(toCheck.operation.name)
        return false
    } 
    
     def boolean isPlainCollectionOperation(Action action) {
        if (!action.collectionOperation)
            return false
        val asCallAction = action as CallOperationAction
        val sourceAction = asCallAction.target.sourceAction
        if (sourceAction instanceof ReadLinkAction || sourceAction instanceof ReadStructuralFeatureAction) {
            return true
        }
        return sourceAction.collectionOperation && sourceAction.plainCollectionOperation 
    }
    
    def static <I> CharSequence generateMany(Iterable<I> items, (I)=>CharSequence mapper) {
        generateMany(items, mapper, '\n')
    }

    def static <I> CharSequence generateMany(Iterable<I> items, (I)=>CharSequence mapper, String separator) {
    	generateMany(items, mapper, separator)
    }
    def static <I> CharSequence generateMany(Iterable<I> items, String separator, (I)=>CharSequence mapper) {
    	generateMany(items, separator, mapper)
    }    
}