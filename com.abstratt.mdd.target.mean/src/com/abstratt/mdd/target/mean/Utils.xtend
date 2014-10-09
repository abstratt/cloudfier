package com.abstratt.mdd.target.mean

import org.eclipse.uml2.uml.Element

class Utils {

    def static CharSequence unsupportedElement(Element e) {
        '''<UNSUPPORTED: «e.eClass.name»>'''
    }
}
