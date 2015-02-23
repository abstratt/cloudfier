package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.Signal

class SignalGenerator extends AbstractJavaGenerator {
    
    new(IRepository repository) {
        super(repository)
    }
    
    def generateSignal(Signal signal) {
        '''
        package «signal.packagePrefix».event;
        
        import java.io.Serializable;
        
        public class «signal.name»Event implements Serializable {
            «signal.allAttributes.generateMany['''
                public final «it.type.toJavaType» «it.name»;
            ''']»
            
            public «signal.name»Event(«signal.allAttributes.generateMany([
                '''«type.toJavaType» «name»'''
            ], ', ')») {
                «signal.allAttributes.generateMany([
                  '''this.«name» = «name»;'''  
                ])»
            }
             
        }
        '''
    }
    
}