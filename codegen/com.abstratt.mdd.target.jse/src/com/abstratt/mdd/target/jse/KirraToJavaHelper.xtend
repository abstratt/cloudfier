package com.abstratt.mdd.target.jse

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import org.eclipse.uml2.uml.Operation

class KirraToJavaHelper {
    static def isProviderOperation(Operation toCheck) {
        toCheck.static && toCheck.class_.entity  
    }
}