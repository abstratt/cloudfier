package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Port
import org.eclipse.uml2.uml.SendSignalAction
import org.eclipse.uml2.uml.Signal
import org.eclipse.uml2.uml.Property

class EntityGenerator extends com.abstratt.mdd.target.jse.EntityGenerator {
    protected IRepository repository

    protected String applicationName

    new(IRepository repository) {
        super(repository)
    }
    
    override generateEntityAnnotations(Class class1) {
        '''
        @Entity
        '''
    }
    
    override generateRelationship(Property relationship) {
        '''«relationship.toJpaRelationshipAnnotation» «super.generateRelationship(relationship)»'''
    }
    
    def toJpaRelationshipAnnotation(Property relationship) {
        val multivalued = relationship.multivalued
        val thisSideMultivalue = relationship.otherEnd != null && relationship.otherEnd.multivalued
        if (multivalued) {
            if (thisSideMultivalue) '@ManyToMany' else '@OneToMany'
        } else {
            if (thisSideMultivalue) '@ManyToOne' else '@OneToOne'
        }    
    }
    
    override generateEntityId(Class entity) {
        '''
        @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
        
        public Long getId() {
            return id;
        }
        '''
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
        import javax.enterprise.context.*;
        '''
    }

    override generateSignal(Signal signal) {
        '''
        @Inject @Transient Event<«signal.name»Event> «signal.name.toFirstLower»Event;
        '''
    }
    
    override generateProvider(Class provider) {
        '''
        @Inject @Transient «super.generateProvider(provider)»
        '''
    }
    
    override generatePort(Port port) {
        '''
        @Inject @Transient «super.generatePort(port)»
        '''
    }

    override def generateSendSignalAction(SendSignalAction action) {
        '''/* generateSendSignalAction - TBD */'''
//        val eventName = '''«action.signal.name.toFirstLower»Event'''
//        val signalName = action.signal.name
//        '''this.«eventName».fire(new «signalName»Event(«action.arguments.generateMany([arg | arg.generateAction], ', ')»))'''
    }
}