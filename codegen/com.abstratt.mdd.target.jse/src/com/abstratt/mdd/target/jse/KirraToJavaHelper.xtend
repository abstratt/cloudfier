package com.abstratt.mdd.target.jse

import org.eclipse.uml2.uml.NamedElement
import org.eclipse.uml2.uml.Operation

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*

class KirraToJavaHelper {
    static def isProviderOperation(Operation toCheck) {
        toCheck.static && toCheck.class_.entity  
    }
    
    
    static def String getSourcePath(NamedElement element) {
        getSourcePath(element, '')
    }

    static def String getSourcePath(NamedElement element, String nameSuffix) {
        '''src/main/java/«element.nearestPackage.qualifiedName.replace(NamedElement.SEPARATOR, "/")»/«element.name»«nameSuffix».java'''.toString
    }
}