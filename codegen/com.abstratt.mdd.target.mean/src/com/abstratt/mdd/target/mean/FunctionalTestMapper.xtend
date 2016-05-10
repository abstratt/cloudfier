package com.abstratt.mdd.target.mean

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.target.ITopLevelMapper
import org.eclipse.uml2.uml.Class

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*

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
}
