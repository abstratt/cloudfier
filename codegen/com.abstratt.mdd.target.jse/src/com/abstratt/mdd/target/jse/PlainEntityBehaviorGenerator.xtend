package com.abstratt.mdd.target.jse

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.util.NamedElementUtils
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Classifier
import org.eclipse.uml2.uml.CreateObjectAction
import org.eclipse.uml2.uml.DestroyObjectAction
import org.eclipse.uml2.uml.NamedElement
import org.eclipse.uml2.uml.ReadExtentAction
import org.eclipse.uml2.uml.SendSignalAction

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.StateMachineUtils.*
import static extension com.abstratt.mdd.target.jse.KirraToJavaHelper.*

class PlainEntityBehaviorGenerator extends PlainJavaBehaviorGenerator {

    protected String applicationName

    new(IRepository repository) {
        super(repository)
    }
    
    override generateCreateObjectAction(CreateObjectAction action) {
        if (!action.classifier.entity)
            return super.generateCreateObjectAction(action)
        val javaClass = action.classifier.toJavaType
        '''«javaClass».objectCreated(«super.generateCreateObjectAction(action)»)'''
    }
    
    override generateDestroyObjectAction(DestroyObjectAction action) {
        if (!action.target.type.entity)
            return super.generateDestroyObjectAction(action)
        val javaClass = action.target.type.toJavaType
        '''«javaClass».objectDestroyed(«action.target»)'''
    }
    
    override generateReadExtentAction(ReadExtentAction action) {
        if (!action.classifier.entity)
            return '''Collections.<«action.classifier.toJavaType».emptyList()'''
        '''«action.classifier.toJavaType».extent()'''
    }
    
    override generateCallOperationAction(CallOperationAction action) {
        if (!action.operation.providerOperation)
            return super.generateCallOperationAction(action)
        val provider = action.operation.class_
        val entity = NamedElementUtils.findNearest(action.actionActivity, [ 
            NamedElement e | e instanceof Class && (e as Class).entity
        ]) as Class
        generateOperationCall(generateProviderReference(entity, provider), action)
    }
    
    override generateProviderReference(Classifier context, Classifier provider) {
        if (context == provider) 'this' else super.generateProviderReference(context, provider)
    }

    override def generateSendSignalAction(SendSignalAction action) {
        val signalName = action.signal.name
        
        // TODO - this is a temporary implementation
        val targetClassifier = action.target.type as Classifier
        if (targetClassifier.entity && !targetClassifier.findStateProperties.empty) {
            val stateMachine = targetClassifier.findStateProperties.head 
            '''«action.target.generateAction».handleEvent(«action.target.toJavaType».«stateMachine.name.toFirstUpper»Event.«signalName»)'''
        }
    }   
}    
