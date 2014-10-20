package com.abstratt.mdd.target.mean

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.target.ITopLevelMapper
import java.util.List
import org.eclipse.uml2.uml.Class

class TestMapper implements ITopLevelMapper<Class> {

    override mapFileName(Class element) {
        throw new UnsupportedOperationException
    }
    
    override mapAll(IRepository repository) {
        val generator = new FunctionalTestGenerator(repository)
        #{ 'test/index.js' -> generator.generateIndex }
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
