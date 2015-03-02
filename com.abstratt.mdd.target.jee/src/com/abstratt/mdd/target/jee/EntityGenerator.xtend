package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.util.MDDUtil
import com.abstratt.mdd.core.util.NamedElementUtils
import java.util.List
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.query.conditions.eobjects.EObjectCondition
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Classifier
import org.eclipse.uml2.uml.Constraint
import org.eclipse.uml2.uml.CreateObjectAction
import org.eclipse.uml2.uml.DestroyObjectAction
import org.eclipse.uml2.uml.Feature
import org.eclipse.uml2.uml.NamedElement
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.Port
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.ReadExtentAction
import org.eclipse.uml2.uml.SendSignalAction
import org.eclipse.uml2.uml.Signal
import org.eclipse.uml2.uml.StateMachine
import org.eclipse.uml2.uml.UMLPackage

import static com.abstratt.mdd.target.jse.AbstractJavaGenerator.*

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import static extension com.abstratt.mdd.core.util.StateMachineUtils.*

class EntityGenerator extends com.abstratt.mdd.target.jse.EntityGenerator {

    protected IRepository repository

    protected String applicationName

    new(IRepository repository) {
        super(repository)
    }
    
    override generateStandardImports() {
        '''
        import java.util.*;
        import java.util.stream.*;
        import java.util.function.*;
        import java.io.Serializable;
        import javax.persistence.*;
        import javax.inject.*;
        import javax.ejb.*;
        import javax.enterprise.event.*;
        '''
    }

    override generateSignal(Signal signal) {
        '''
        @Inject
        Event<«signal.name»Event> «signal.name.toFirstLower»Event;
        '''
    }
    
    override generateProvider(Class provider) {
        '''
        @Inject «super.generateProvider(provider)»
        '''
    }
    
    override generatePort(Port port) {
        '''
        @Inject «super.generatePort(port)»
        '''
    }

    override def generateSendSignalAction(SendSignalAction action) {
        val eventName = '''«action.signal.name.toFirstLower»Event'''
        val signalName = action.signal.name
        '''this.«eventName».fire(new «signalName»Event(«action.arguments.generateMany([arg | arg.generateAction], ', ')»))'''
    }
}