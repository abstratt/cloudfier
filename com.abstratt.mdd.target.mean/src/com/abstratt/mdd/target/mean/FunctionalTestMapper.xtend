package com.abstratt.mdd.target.mean

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.target.ITopLevelMapper
import java.util.List
import org.eclipse.uml2.uml.Class

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.TemplateUtils.*

class FunctionalTestMapper implements ITopLevelMapper<Class> {

    override mapFileName(Class element) {
        '''test/«element.name».js'''
    }
    
    override mapAll(IRepository repository) {
        val generator = new FunctionalTestGenerator(repository)
        return generator.testRelatedClasses.toMap[mapFileName(it)].mapValues[
            if (it.hasStereotype('Test')) 
                generator.generateTestSuite(it)
            else
                generator.generateSuiteHelper(it)
        ]
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
}
