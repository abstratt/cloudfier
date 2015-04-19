package com.abstratt.mdd.target.jse

import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.Classifier
import static extension com.abstratt.kirra.mdd.core.KirraHelper.*

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
    
    def boolean isServiceOperation(Operation op) {
        op.static
    }
    
    def generateJavaClass(Class entity) {
        val serviceOperations = entity.allOperations.filter[serviceOperation]
        val signals = findTriggerableSignals(serviceOperations)
        '''
        public class «entity.name»Service {
            «entity.generateJavaClassPrefix»
            «entity.generateRelated»
            «generateMany(signals, [generateSignal])»
            «entity.generateAnonymousDataTypes»
            «serviceOperations.generateMany[generateServiceOperation]»
        }
        '''
    }
    
    def generateRelated(Classifier entity) {
        val nonNavigableRelationships = getRelationships(entity).filter[!derived].map[otherEnd].filter[!navigable]
        nonNavigableRelationships.generateMany[ relationship |
            val otherEnd = relationship.otherEnd
        '''
            public List<«relationship.type.name»> find«relationship.name.toFirstUpper»By«otherEnd.name.toFirstUpper»(«otherEnd.type.name» «otherEnd.name») {
                return «relationship.type.name».extent().stream().filter(
                    candidate -> candidate.get«otherEnd.name.toFirstUpper»() == «otherEnd.name» 
                ).collect(Collectors.toList());
            }
        '''
        ]
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