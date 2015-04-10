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
            
            «signal.getAllAttributes().map['''
                public final «it.type.toJavaType» «it.name»;
            '''].join()»
            
            public «signal.name»Event(«signal.getAllAttributes().map[
                '''«type.toJavaType» «name»'''
            ].join(', ')») {
                «signal.getAllAttributes().map[
                  '''
                  this.«name» = «name»;
                  '''  
                ].join»
            }
             
        }
        '''
    }
}
