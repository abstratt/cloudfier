package com.abstratt.mdd.target.simple

import org.eclipse.uml2.uml.Class

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*

class SimpleClassGenerator {
    
    def generate(Class classElement) {
        '''
        This is class «classElement.name».
        
        #### Properties: 
            «classElement.properties.map [ property |
                '''- «property.name»''' 
            ].join('\n')»

        «IF !classElement.actions.empty»
        #### Actions: 
            «classElement.actions.map [ 
                '''- «it.name»''' 
            ].join('\n')»
        «ENDIF»
        '''
    }
    
}
