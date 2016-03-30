package com.abstratt.mdd.target.jse

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.target.ITopLevelMapper
import java.util.LinkedHashMap
import java.util.List
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Classifier
import org.eclipse.uml2.uml.DataType
import org.eclipse.uml2.uml.Enumeration
import org.eclipse.uml2.uml.Interface
import org.eclipse.uml2.uml.NamedElement
import org.eclipse.uml2.uml.Signal

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.DataTypeUtils.*
import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.FeatureUtils.*
import org.eclipse.uml2.uml.Constraint
import org.eclipse.uml2.uml.Operation
import com.abstratt.mdd.core.target.spi.TargetUtils

class EntityMapper implements ITopLevelMapper<Classifier> {
    
    override mapFileName(Classifier classifier) {
        switch (classifier) {
            case classifier instanceof Signal:
                generateSignalFileName(classifier).toString
            case classifier instanceof Enumeration:
                '''«generateEnumerationFileName(classifier)»'''
            case classifier instanceof Class:
                '''«generateEntityFileName(classifier)»'''
        }
    }
    
    def generateEnumerationFileName(Classifier classifier) {
        '''src/main/java/«classifier.namespace.qualifiedName.replace(NamedElement.SEPARATOR, "/")»/«classifier.name».java'''.toString
    }
    
    def generateInterfaceFileName(Classifier classifier) {
        '''src/main/java/«classifier.namespace.qualifiedName.replace(NamedElement.SEPARATOR, "/")»/«classifier.name».java'''.toString
    }
    
    def generateTupleFileName(Classifier classifier) {
        '''src/main/java/«classifier.namespace.qualifiedName.replace(NamedElement.SEPARATOR, "/")»/«classifier.name».java'''.toString
    }

    def generateEntityFileName(Classifier classifier) {
        '''src/main/java/«classifier.namespace.qualifiedName.replace(NamedElement.SEPARATOR, "/")»/«classifier.name».java'''.toString
    }
    
    def generateSignalFileName(Classifier classifier) {
        '''src/main/java/«classifier.namespace.qualifiedName.replace(NamedElement.SEPARATOR, "/")»/«classifier.name»Event.java'''.toString
    }
    
    def generateConstraintExceptionFileName(Constraint constraint) {
        val namespace = constraint.nearestPackage
        '''src/main/java/«namespace.qualifiedName.replace(NamedElement.SEPARATOR, "/")»/«constraint.name»Exception.java'''.toString
    }

    def generateServiceFileName(Classifier classifier) {
        '''src/main/java/«classifier.namespace.qualifiedName.replace(NamedElement.SEPARATOR, "/")»/«classifier.name»Service.java'''.toString
    }
    
    override map(Classifier toMap) {
        throw new UnsupportedOperationException
    }
    
    override mapAll(List<Classifier> toMap) {
        throw new UnsupportedOperationException
    }
    
    override canMap(Classifier element) {
        throw new UnsupportedOperationException
    }
    
    def PlainEntityGenerator createEntityGenerator(IRepository repository) {
        new PlainEntityGenerator(repository)
    }
    
    override mapAll(IRepository repository) {
        val appPackages = repository.getTopLevelPackages(null).applicationPackages
        val entities = appPackages.entities
        val applicationPackage = entities.filter[userVisible].head.package
	    val applicationName = applicationPackage.name

    	
        val result = new LinkedHashMap<String, CharSequence>()
        
        val entityGenerator = createEntityGenerator(repository)
        result.putAll(entities.toMap[generateEntityFileName].mapValues[entityGenerator.generateEntity(it)])

        val services = findEntitiesWithServices(entities)
        val serviceGenerator = createServiceGenerator(repository)
        result.putAll(services.toMap[generateServiceFileName].mapValues[serviceGenerator.generateService(it)])
        
        val interfaces = appPackages.map[ownedTypes.filter(typeof(Interface))].flatten
        val interfaceGenerator = new InterfaceGenerator(repository)
        result.putAll(interfaces.toMap[generateInterfaceFileName].mapValues[interfaceGenerator.generateInterface(it)])
        
        val tuples = appPackages.tupleTypes.filter(typeof(DataType))
        val tupleGenerator = new TupleGenerator(repository)
        result.putAll(tuples.filter[!anonymousDataType].toMap[generateTupleFileName].mapValues[tupleGenerator.generateTuple(it)])

        val enums = appPackages.map[ownedTypes.filter(typeof (Enumeration))].flatten
        val enumerationGenerator = new EnumerationGenerator(repository)
        result.putAll(enums.toMap[generateEnumerationFileName].mapValues[enumerationGenerator.generateEnumeration(it)])

        val signals = appPackages.tupleTypes.filter(typeof (Signal))
        val signalGenerator = new SignalGenerator(repository)
        result.putAll(signals.toMap[generateSignalFileName].mapValues[signalGenerator.generateSignal(it)])
        
        val invariants = appPackages.entities.map[ownedRules.filter[name != null]].flatten
        val preconditions = appPackages.entities.map[allOperations.filter[!query].map[ownedRules.filter[name != null]].flatten].flatten
        val constraints = invariants + preconditions
        val constraintExceptionGenerator = new ConstraintExceptionGenerator(repository)
        result.putAll(constraints.toMap[generateConstraintExceptionFileName].mapValues[constraintExceptionGenerator.generateConstraintException(it)])
        
        val templates = #{
        	'''src/main/java/«applicationName»/ConstraintViolationException.java'''.toString -> "/templates/src/main/java/application/ConstraintViolationException.java"
    	}
    	templates.forEach[targetPath, sourcePath|
    		result.put(
				targetPath,
					TargetUtils.merge(EntityMapper.getResourceAsStream(sourcePath), 
	                #{ 
	                    "applicationName" -> applicationName
	                }
	            )
    		)	
    	]
        
        return result
    }
    
    def findEntitiesWithServices(List<Class> entities) {
        entities.filter[ownedOperations.exists[public && static]]
    }
    
    def createServiceGenerator(IRepository repository) {
        new ServiceGenerator(repository)
    }    
}
