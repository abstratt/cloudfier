package com.abstratt.mdd.target.jse

import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.Classifier

class ServiceGenerator extends PlainEntityGenerator {
    
    new(IRepository repository) {
        super(repository)
    }
    
    def generateService(Class entity) {
        '''
        package «entity.packagePrefix»;

        «generateStandardImports»        
        
        «entity.generateImports»
        
        «entity.generateJavaClass»
        '''
    }
    
    def generateJavaClass(Class entity) {
        val serviceOperations = entity.allOperations.filter[static]
        val signals = findTriggerableSignals(serviceOperations)
        '''
        public class «entity.name»Service {
            «entity.generateJavaClassPrefix»
            «generateMany(signals, [generateSignal])»
            «entity.generateAnonymousDataTypes»
            «serviceOperations.generateMany[generateServiceOperation]»
        }
        '''
    }
    
    def generateJavaClassPrefix(Class entity) {
        ''
    }
    
    override generateProviderReference(Classifier context, Classifier provider) {
        if (context == provider) 'this' else super.generateProviderReference(context, provider)
    }
    
    def generateServiceOperation(Operation serviceOperation) {
        serviceOperation.generateJavaMethod(serviceOperation.visibility, false)
    }
}