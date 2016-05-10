package com.abstratt.mdd.target.simple

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.target.ITopLevelMapper
import java.util.List
import org.eclipse.uml2.uml.Class
import static extension com.abstratt.kirra.mdd.core.KirraHelper.*

class SimpleMapper implements ITopLevelMapper<Class> {
    
        
    override mapAll(IRepository repository) {
        val appPackages = repository.getTopLevelPackages(null).applicationPackages
        
        val topLevelEntities = appPackages.entities.filter[topLevel]
        
        return topLevelEntities.toMap[mapFileName(it)].mapValues[new SimpleClassGenerator().generate(it)]
    }
        
    
    override mapFileName(Class classElement) {
        '''«classElement.name».txt'''
    }
}
