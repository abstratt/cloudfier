package com.abstratt.mdd.target.jse

import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Operation

class ServiceGenerator extends EntityGenerator {
    
    new(IRepository repository) {
        super(repository)
    }
    
    def generateService(Class entity) {
        val serviceOperations = entity.allOperations.filter[static]
        '''
        package «entity.packagePrefix»;

        «generateStandardImports»        
        
        «entity.generateImports»
        
        public class «entity.name»Service {
            «generateMany(findTriggerableSignals(serviceOperations), [generateSignal])»
            «entity.generateAnonymousDataTypes»
            «serviceOperations.generateMany[generateServiceOperation]»
        }
        '''
    }
    
    override generateProviderReference(Class context, Class provider) {
        if (context == provider) 'this' else super.generateProviderReference(context, provider)
    }
    
    def generateServiceOperation(Operation serviceOperation) {
        serviceOperation.generateJavaMethod(serviceOperation.visibility, false)
    }
}