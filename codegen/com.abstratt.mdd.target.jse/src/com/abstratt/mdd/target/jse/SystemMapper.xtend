package com.abstratt.mdd.target.jse

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.target.ITopLevelMapper
import org.eclipse.uml2.uml.Classifier

class SystemMapper implements ITopLevelMapper<Classifier> {
    
    override mapFileName(Classifier classifier) {
        '''src/main/java/system/System.java'''
        
    }
    
    override mapAll(IRepository repository) {
        val result = newLinkedHashMap()
        result.put(mapFileName(null), '')
        return result
    }    
}
