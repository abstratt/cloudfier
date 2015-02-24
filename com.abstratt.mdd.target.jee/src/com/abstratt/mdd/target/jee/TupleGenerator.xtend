package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.DataType

class TupleGenerator extends AbstractJavaGenerator {
    
    new(IRepository repository) {
        super(repository)
    }
    
    def generateTuple(DataType dataType) {
        '''
        package «dataType.packagePrefix»;
        
        import java.io.Serializable;
        import java.util.*;
        
        public class «dataType.name» implements Serializable {
            «dataType.allAttributes.generateMany['''
                public final «it.type.toJavaType» «it.name»;
            ''']»
            
            public «dataType.name»(«dataType.allAttributes.generateMany([
                '''«type.toJavaType» «name»'''
            ], ', ')») {
                «dataType.allAttributes.generateMany([
                  '''this.«name» = «name»;'''  
                ])»
            }
             
        }
        '''
    }
    
}