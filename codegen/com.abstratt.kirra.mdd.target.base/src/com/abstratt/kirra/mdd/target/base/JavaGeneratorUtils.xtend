package com.abstratt.kirra.mdd.target.base

import org.eclipse.uml2.uml.NamedElement
import org.eclipse.uml2.uml.Package

class JavaGeneratorUtils {
    def static String toJavaPackage(Package package_) {
        package_.qualifiedName.replace(NamedElement.SEPARATOR, ".")
    }
}