package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.Enumeration

class EnumerationGenerator extends AbstractJavaGenerator {
    
    new(IRepository repository) {
        super(repository)
    }
    
    def generateEnumeration(Enumeration enumeration) {
        '''
        package «enumeration.packagePrefix»;
        
        public enum «enumeration.name» {
            «enumeration.ownedLiterals.generateMany([name], ',\n')»
        }
        '''
    }
    
}