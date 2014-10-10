package com.abstratt.mdd.target.mean

import org.eclipse.uml2.uml.Element
import org.eclipse.uml2.uml.NamedElement

class Utils {

    def static CharSequence unsupportedElement(Element e) {
        unsupportedElement(e, if (e instanceof NamedElement) e.qualifiedName else null)
    }
    
    def static CharSequence unsupportedElement(Element e, String message) {
        '''<UNSUPPORTED: «e.eClass.name»> «if (message != null) '''(«message»)''' else ''»'''
    }
}
