package com.abstratt.mdd.target.jse

import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.Constraint
import com.abstratt.kirra.mdd.core.KirraHelper

class ConstraintExceptionGenerator extends BehaviorlessClassGenerator {
    new(IRepository repository) {
        super(repository)
    }
    
    def generateConstraintException(Constraint constraint) {
        '''
        package «constraint.packagePrefix»;
        
        public class «constraint.name»Exception extends RuntimeException {
            private static final long serialVersionUID = 1L;
            public «constraint.name»Exception() {
                //TODO support for message bundles 
                super("«KirraHelper.getLabel(constraint)»");
            }
        }
        '''
    }
    
}