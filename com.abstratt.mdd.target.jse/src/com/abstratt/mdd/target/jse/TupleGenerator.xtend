package com.abstratt.mdd.target.jse

import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.DataType

class TupleGenerator extends BehaviorlessClassGenerator {
    
    new(IRepository repository) {
        super(repository)
    }
    
    def generateTuple(DataType dataType) {
        '''
        package «dataType.packagePrefix»;
        
        import java.io.Serializable;
        import java.util.*;
        
        «dataType.generateDataType»
        '''
    }
    
}