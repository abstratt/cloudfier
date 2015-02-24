package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.target.ITopLevelMapper
import java.util.LinkedHashMap
import java.util.List
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Classifier
import org.eclipse.uml2.uml.Enumeration
import org.eclipse.uml2.uml.NamedElement
import org.eclipse.uml2.uml.Signal

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import org.eclipse.uml2.uml.Interface
import org.eclipse.uml2.uml.DataType
import org.eclipse.uml2.uml.VisibilityKind

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
    
    override mapAll(IRepository repository) {
        val appPackages = repository.getTopLevelPackages(null).applicationPackages
        val result = new LinkedHashMap<String, CharSequence>()
        
        val entities = appPackages.entities
        val entityGenerator = new EntityGenerator(repository)
        result.putAll(entities.toMap[generateEntityFileName].mapValues[entityGenerator.generateEntity(it)])

        val services = appPackages.entities.filter[ownedOperations.exists[public && static]]
        val serviceGenerator = new ServiceGenerator(repository)
        result.putAll(services.toMap[generateServiceFileName].mapValues[serviceGenerator.generateService(it)])
        
        val interfaces = appPackages.map[ownedTypes.filter(typeof(Interface))].flatten
        val interfaceGenerator = new InterfaceGenerator(repository)
        result.putAll(interfaces.toMap[generateInterfaceFileName].mapValues[interfaceGenerator.generateInterface(it)])
        
        val tuples = appPackages.tupleTypes.filter(typeof(DataType))
        val tupleGenerator = new TupleGenerator(repository)
        result.putAll(tuples.toMap[generateTupleFileName].mapValues[tupleGenerator.generateTuple(it)])

        val enums = appPackages.map[ownedTypes.filter(typeof (Enumeration))].flatten
        val enumerationGenerator = new EnumerationGenerator(repository)
        result.putAll(enums.toMap[generateEnumerationFileName].mapValues[enumerationGenerator.generateEnumeration(it)])

        val signals = appPackages.tupleTypes.filter(typeof (Signal))
        val signalGenerator = new SignalGenerator(repository)
        result.putAll(signals.toMap[generateSignalFileName].mapValues[signalGenerator.generateSignal(it)])
        
        return result
    }    
}
