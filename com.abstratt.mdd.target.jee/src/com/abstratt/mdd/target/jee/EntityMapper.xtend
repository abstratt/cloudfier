package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.target.ITopLevelMapper
import java.util.List
import org.eclipse.uml2.uml.Classifier
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Enumeration
import org.eclipse.uml2.uml.NamedElement
import org.eclipse.uml2.uml.Signal

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*

class EntityMapper implements ITopLevelMapper<Classifier> {
    
    override mapFileName(Classifier classifier) {
        switch (classifier) {
            case classifier instanceof Signal:
                '''src/main/java/«classifier.namespace.qualifiedName.replace(NamedElement.SEPARATOR, "/")»/event/«classifier.name»Event.java'''
            case classifier instanceof Enumeration || classifier instanceof Class:
                '''src/main/java/«classifier.namespace.qualifiedName.replace(NamedElement.SEPARATOR, "/")»/entity/«classifier.name».java'''
                                
        }
        
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
        val result = newLinkedHashMap()
        
        val topLevelEntities = appPackages.entities.filter[topLevel]
        val entityGenerator = new EntityGenerator(repository)
        result.putAll(topLevelEntities.toMap[mapFileName].mapValues[entityGenerator.generateEntity(it)])

        val enums = appPackages.map[ownedTypes.filter(typeof (Enumeration))].flatten
        val enumerationGenerator = new EnumerationGenerator(repository)
        result.putAll(enums.toMap[mapFileName].mapValues[enumerationGenerator.generateEnumeration(it)])

        val signals = appPackages.tupleTypes.filter(typeof (Signal))
        val signalGenerator = new SignalGenerator(repository)
        result.putAll(signals.toMap[mapFileName].mapValues[signalGenerator.generateSignal(it)])
        return result
    }    
}
