package com.abstratt.mdd.target.mean

import com.abstratt.kirra.mdd.core.KirraHelper
import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.target.ITopLevelMapper
import com.abstratt.mdd.core.target.spi.TargetUtils
import java.util.Arrays
import java.util.List
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.UMLPackage

class ModelMapper implements ITopLevelMapper<Class> {
    
    override mapFileName(Class element) {
        '''models/«element.name».js'''
    }
    
    override map(Class toMap) {
        new ModelGenerator().generateEntity(toMap)
    }
    
    override mapAll(List<Class> toMap) {
        toMap.map[map].join('\n')
    }
    
    override canMap(Class element) {
        KirraHelper.isEntity(element)
    }
    
    override mapAll(IRepository repository) {
        return TargetUtils.map(repository, this, UMLPackage.Literals.CLASS, Arrays.<String>asList());
    }    
}
