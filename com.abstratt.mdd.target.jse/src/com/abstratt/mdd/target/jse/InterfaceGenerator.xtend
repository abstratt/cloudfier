package com.abstratt.mdd.target.jse

import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.ParameterDirectionKind
import org.eclipse.uml2.uml.Activity

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import static extension com.abstratt.mdd.core.util.StateMachineUtils.*
import org.eclipse.uml2.uml.Interface

class InterfaceGenerator extends AbstractJavaGenerator {
    
    new(IRepository repository) {
        super(repository)
    }
    
    def generateInterface(Interface interface_) {
        '''
        package «interface_.packagePrefix»;
        
        import java.util.*;
        import java.util.stream.*;
        
        «interface_.generateImports»
        
        public interface «interface_.name» {
            «interface_.allOperations.generateMany[generateInterfaceOperation]»
        }
        '''
    }
    
    def generateInterfaceOperation(Operation interfaceOperation) {
        '''«interfaceOperation.generateJavaMethodSignature(interfaceOperation.visibility, interfaceOperation.static)»;'''
    }
    
}