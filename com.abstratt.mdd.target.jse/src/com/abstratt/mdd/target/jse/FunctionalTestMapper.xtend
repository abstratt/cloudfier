package com.abstratt.mdd.target.jse

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.target.ITopLevelMapper
import java.util.List
import org.eclipse.uml2.uml.Class

class FunctionalTestMapper implements ITopLevelMapper<Class> {

    override mapFileName(Class element) {
        '''src/test/java/«element.namespace.name»/test/«element.name».java'''
    }
    
    override mapAll(IRepository repository) {
        val generator = new FunctionalTestGenerator(repository)
        val result = newLinkedHashMap()
        result.putAll(generator.testClasses.toMap[mapFileName(it)].mapValues[
            generator.generateTestClass(it)
        ])
        result.putAll(generator.testRelatedClasses.toMap[mapFileName(it)].mapValues[
            generator.generateTestHelperClass(it)
        ])
        return result
        
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
