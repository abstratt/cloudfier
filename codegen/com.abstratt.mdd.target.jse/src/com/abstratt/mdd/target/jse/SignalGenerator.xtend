package com.abstratt.mdd.target.jse

import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.Signal

class SignalGenerator extends BehaviorlessClassGenerator {
    
    new(IRepository repository) {
        super(repository)
    }
    
    def generateSignal(Signal signal) {
        '''
        package «signal.packagePrefix»;
        
        import java.io.Serializable;
        import java.util.*;
        
        public class «signal.name»Event implements Serializable {
            private static final long serialVersionUID = 1L;
            
            «signal.allAttributes.map['''
                public final «it.type.toJavaType» «it.name»;
            '''].join()»
            
            public «signal.name»Event(«signal.allAttributes.map[
                '''«type.toJavaType» «name»'''
            ].join(', ')») {
                «signal.allAttributes.map[
                  '''
                  this.«name» = «name»;
                  '''  
                ].join»
            }
             
        }
        '''
    }
}
