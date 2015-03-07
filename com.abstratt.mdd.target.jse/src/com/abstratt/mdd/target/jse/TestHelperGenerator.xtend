package com.abstratt.mdd.target.jse

import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.VisibilityKind

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import static extension com.abstratt.mdd.core.util.TemplateUtils.*
import org.eclipse.uml2.uml.Namespace

class TestHelperGenerator extends PlainEntityGenerator {

    new(IRepository repository) {
        super(repository)
    }
    
    def CharSequence generateHelperMethod(Operation helperOperation) {
        helperOperation.generateJavaMethod(VisibilityKind.PACKAGE_LITERAL, helperOperation.static)
    }

    def generateTestHelperClass(Class testHelperClass) {
        val testedPackages = appPackages.filter [
            it.ownedTypes.exists  [
                it.entity
            ]
        ] 
        val operations = testHelperClass.operations
        '''
            package «testHelperClass.packagePrefix».test;
            
            «generateStandardImports»
            
            «testedPackages.generateMany[p|
            '''
            import «p.toJavaPackage».*;
            '''
            ]»
            
            public class «testHelperClass.name» {
                «testHelperClass.generateTestHelperClassPrefix»
                «operations.generateMany[generateHelperMethod]»
            } 
        '''
    }
    
    override generateImports(Namespace namespaceContext) {
        super.generateImports(namespaceContext)
    }
    
    def CharSequence generateTestHelperClassPrefix(Class helperClass) {
        ''
    }

}
