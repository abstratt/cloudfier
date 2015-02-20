package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.target.ITopLevelMapper
import java.util.List
import org.eclipse.uml2.uml.Class

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import org.eclipse.uml2.uml.NamedElement

class EntityMapper implements ITopLevelMapper<Class> {
    
    override mapFileName(Class element) {
        '''src/main/java/entity/«element.namespace.qualifiedName.replace(NamedElement.SEPARATOR, "/")»/«element.name».java'''
    }
    
    override map(Class toMap) {
        throw new UnsupportedOperationException
    }
    
    override mapAll(List<Class> toMap) {
        throw new UnsupportedOperationException
    }
    
    override canMap(Class element) {
        throw new UnsupportedOperationException
    }
    
    override mapAll(IRepository repository) {
        val appPackages = repository.getTopLevelPackages(null).applicationPackages
        val topLevelEntities = appPackages.entities.filter[it.topLevel]
        val generator = new EntityGenerator(repository)
        val result = newLinkedHashMap()
        result.putAll(topLevelEntities.toMap[mapFileName(it)].mapValues[generator.generateEntity(it)])
        return result
    }    
}
