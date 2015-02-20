package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.target.ITopLevelMapper
import java.util.List
import org.eclipse.uml2.uml.Class

class FunctionalTestMapper implements ITopLevelMapper<Class> {

    override mapFileName(Class element) {
        '''src/test/java/test/«element.namespace.name»/«element.name».java'''
    }
    
    override mapAll(IRepository repository) {
        val generator = new FunctionalTestGenerator(repository)
        return generator.testClasses.toMap[mapFileName(it)].mapValues[
            generator.generateTestClass(it)
//          else      generator.generateSuiteHelper(it)
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
