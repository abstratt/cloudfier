package com.abstratt.mdd.target.jse

import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.Enumeration
import org.eclipse.uml2.uml.Activity
import java.util.List
import org.eclipse.uml2.uml.Parameter

class EnumerationGenerator extends BehaviorlessClassGenerator {
    
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