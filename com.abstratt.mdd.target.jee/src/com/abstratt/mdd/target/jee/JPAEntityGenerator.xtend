package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.target.jse.IBehaviorGenerator
import com.abstratt.mdd.target.jse.PlainEntityGenerator
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Port
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.Signal

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*

class JPAEntityGenerator extends PlainEntityGenerator {
    protected String applicationName

    new(IRepository repository) {
        super(repository)
    }
    
    override IBehaviorGenerator createBehaviorGenerator() {
        return new JPABehaviorGenerator(repository) 
    }
    
    override generateEntityAnnotations(Class entity) {
        '''
        @Entity
        '''
    }
    
    override generateRelationship(Property relationship) {
        '''«relationship.toJpaRelationshipAnnotation» «super.generateRelationship(relationship)»'''
    }
    
    override generateAttribute(Property attribute) {
        '''«attribute.toJpaPropertyAnnotation» «super.generateAttribute(attribute)»'''
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

    def toJpaPropertyAnnotation(Property property) {
        // cannot use KirraHelper as this is about storing data, not user capabilities
        val nullable = property.lower == 0
        val unique = property.ID
        val insertable = !property.readOnly || !nullable
        val updatable = !property.readOnly || !insertable
        val values = #{'nullable' -> (nullable), 'updatable' -> (updatable), 'insertable' -> (insertable), 'unique' -> unique }
        val defaultValues = #{'nullable' -> true, 'updatable' -> true, 'insertable' -> true, 'unique' -> false }
        val nonDefaults = values.filter[ key, value | value != defaultValues.get(key) ]
        val pairs = nonDefaults.entrySet.map['''«key»=«value»''']
        '''@Column«IF !pairs.empty»(«pairs.join(', ')»)«ENDIF»''' 
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
    
    override generateSuffix(Class entity) {
        // turn off any weird stuff we generate for POJO entities
        ''''''
    }

    override generateSignal(Signal signal) {
//        when we support CDI events        
//        '''
//        @Inject @Transient Event<«signal.name»Event> «signal.name.toFirstLower»Event;
//        '''
        ''
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
}