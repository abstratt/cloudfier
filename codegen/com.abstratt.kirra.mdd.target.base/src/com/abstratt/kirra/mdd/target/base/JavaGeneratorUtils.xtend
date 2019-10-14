package com.abstratt.kirra.mdd.target.base

import org.eclipse.uml2.uml.NamedElement
import static extension com.abstratt.kirra.mdd.core.KirraHelper.*

class JavaGeneratorUtils {
    def static String toJavaPackage(NamedElement package_) {
        toJavaQName(package_)
    }
				
	def static String toJavaQName(NamedElement package_) {
		val asJavaQualifiedName = package_.qualifiedName.split(NamedElement.SEPARATOR).map[it.symbol].join(".")
		return asJavaQualifiedName
	}

    def static CharSequence wrapAsSafeStatement(CharSequence guardedRegion, CharSequence... guardConditions) {
        '''
        if («joinExpressions("&&", guardConditions.map[condition |
            ['''«condition» != null''']
        ])») {
            «guardedRegion»
        }
        '''
    }
    
    def static CharSequence joinExpressions(CharSequence operator, Iterable<()=>CharSequence> operands) {
        return if (operands.length == 1)
            // no parentheses for a single expression
            operands.get(0).apply
        else
            operands.map['''(«it.apply»)'''].join(''' «operator» ''')
    }
}