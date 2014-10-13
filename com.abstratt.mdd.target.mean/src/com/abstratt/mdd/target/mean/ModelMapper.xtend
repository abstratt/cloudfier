package com.abstratt.mdd.target.mean

import com.abstratt.mdd.core.target.ITopLevelMapper
import java.util.List
import org.eclipse.uml2.uml.Class
import com.abstratt.kirra.mdd.core.KirraHelper

class ModelMapper implements ITopLevelMapper<Class> {
    
    override mapFileName(Class element) {
        '''models/«element.name».js'''
    }
    
    override map(Class toMap) {
        new DomainModelGenerator().generateEntity(toMap)
    }
    
    override mapAll(List<Class> toMap) {
        toMap.map[map].join('\n')
    }
    
    override canMap(Class element) {
        KirraHelper.isEntity(element)
    }
    
}
