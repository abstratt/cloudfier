package com.abstratt.mdd.target.jse

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.target.ITopLevelMapper
import java.util.List
import org.eclipse.uml2.uml.Classifier

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*

class SystemMapper implements ITopLevelMapper<Classifier> {
    
    override mapFileName(Classifier classifier) {
        '''src/main/java/system/System.java'''
        
    }
    
    override map(Classifier toMap) {
        throw new UnsupportedOperationException
    }
    
    override mapAll(List<Classifier> toMap) {
        throw new UnsupportedOperationException
    }
    
    override canMap(Classifier element) {
        throw new UnsupportedOperationException
    }
    
    override mapAll(IRepository repository) {
        val result = newLinkedHashMap()
        result.put(mapFileName(null), '')
        return result
    }    
}
