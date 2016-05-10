package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.target.ITopLevelMapper
import com.abstratt.mdd.target.jse.TestHelperGenerator
import java.util.List
import org.eclipse.uml2.uml.Class

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.target.jse.TestUtils.*

class FunctionalTestMapper implements ITopLevelMapper<Class> {

    override mapFileName(Class element) {
        '''src/test/java/«element.namespace.name»/test/«element.name».java'''
    }
    
    override mapAll(IRepository repository) {
        
        val testPackages = repository.getTopLevelPackages(null).testPackages
        
        val testGenerator = new JPAFunctionalTestGenerator(repository)
        val result = newLinkedHashMap()
        
        result.putAll(testPackages.testClasses.toMap[mapFileName(it)].mapValues[
            testGenerator.generateTestClass(it)
        ])
        result.putAll(testPackages.testHelperClasses.toMap[mapFileName(it)].mapValues[
            testGenerator.generateTestHelperClass(it)
        ])
        return result
        
    }
}
