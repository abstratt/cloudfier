package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.target.ITopLevelMapper
import java.util.List
import org.eclipse.uml2.uml.Classifier
import org.eclipse.uml2.uml.NamedElement
import org.eclipse.uml2.uml.Signal

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*

class EntityMapper implements ITopLevelMapper<Classifier> {
    
    override mapFileName(Classifier classifier) {
        switch (classifier) {
            case classifier.entity:
                '''src/main/java/«classifier.namespace.qualifiedName.replace(NamedElement.SEPARATOR, "/")»/entity/«classifier.name».java'''
            case classifier instanceof Signal:
                '''src/main/java/«classifier.namespace.qualifiedName.replace(NamedElement.SEPARATOR, "/")»/event/«classifier.name»Event.java'''                
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
        
        val signals = appPackages.tupleTypes.filter[it instanceof Signal]
        val signalGenerator = new SignalGenerator(repository)
        result.putAll(signals.toMap[mapFileName].mapValues[signalGenerator.generateSignal(it as Signal)])
        return result
    }    
}
