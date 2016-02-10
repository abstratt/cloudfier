package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.target.base.IBehaviorGenerator
import com.abstratt.mdd.target.jse.PlainEntityGenerator
import org.apache.commons.lang3.StringUtils
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Classifier
import org.eclipse.uml2.uml.Port
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.Signal

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.FeatureUtils.*
import static extension com.abstratt.mdd.target.jee.JPAHelper.*
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.Activity

class JPAEntityGenerator extends PlainEntityGenerator {
    protected String applicationName

    new(IRepository repository) {
        super(repository)
    }
    
    override IBehaviorGenerator createBehaviorGenerator() {
        return new CustomJPABehaviorGenerator(this, repository) 
    }
    
    static class CustomJPABehaviorGenerator extends JPABehaviorGenerator {
        JPAEntityGenerator parent

        new(JPAEntityGenerator parent, IRepository repository) {
            super(repository)
            this.parent = parent
        }
        
        override generateActivity(Activity activity) {
            val operation = activity.specification as Operation
            if (!activity.queryPerformingActivity) {
                return super.generateActivity(activity)
            }
            // delegate query performing operations to the service
            '''return «operation.class_.name.toFirstLower»Service».«operation.name»(this);'''
        }
        
    }
    
    override generateEntityAnnotations(Class entity) {
        '''
        @Entity
        '''
    }
    
    override generateRelationship(Property relationship) {
        '''
        «relationship.toJpaRelationshipAnnotation»
        «relationship.toJpaJoinTableAnnotation»
        «super.generateRelationship(relationship)»
        '''
    }
    
    override generateRelationshipAccessorType(Property relationship) {
        if (relationship.multivalued)
            '''Collection<«relationship.type.toJavaType»>'''
        else
            super.generateRelationshipAccessorType(relationship)
    }
    
    override generateSequenceAttribute(Property attribute) {
        '''
        public Long «attribute.generateAccessorName»() {
            return id;
        }
        '''
    }
    
    override generateAttribute(Property attribute) {
        '''
        «attribute.toJpaPropertyAnnotation»
        «super.generateAttribute(attribute)»
        '''
    }
    
    def toJpaRelationshipAnnotation(Property relationship) {
        val hasOtherEnd = relationship.otherEnd != null 
        val navigableFromTheOtherSide = hasOtherEnd && relationship.otherEnd.navigable
        val multivalued = relationship.multivalued
        val thisSideMultivalue = hasOtherEnd && relationship.otherEnd.multivalued
        
        val values = newHashMap()
        if (navigableFromTheOtherSide && !relationship.isPrimary)
            values.put('mappedBy', '''"«relationship.otherEnd.name»"''')
        // only ...ToOne annotations have optionality
// TODO: need to improve CRUD tests before enforcing required relationships         
//        if (!multivalued && relationship.isRequired(true))    
//            values.put('optional', 'false')
         
        val pairs = values.entrySet.map['''«key»=«value»''']
        val annotation = if (multivalued) {
            if (thisSideMultivalue) '@ManyToMany' else '@OneToMany'
        } else {
            if (thisSideMultivalue) '@ManyToOne' else '@OneToOne'
        }
        '''«annotation»«IF !pairs.empty»(«pairs.join(', ')»)«ENDIF»'''
    }
    
        
    def toJpaJoinTableAnnotation(Property relationship) {
        if (!relationship.primary || !(relationship.multivalued && relationship.otherEnd.multivalued))
            return ''
        val associationName = getMappingTableName(relationship.owningClassifier, relationship, relationship.otherEnd)
        '''
        @JoinTable(name = "«associationName»", 
            joinColumns = {@JoinColumn(name = "«relationship.otherEnd.name»_id")},
            inverseJoinColumns = {@JoinColumn(name = "«relationship.name»_id")}
        )
        '''
    }
    
    
    private def String getMappingTableName(Classifier contextEntity, Property source, Property opposite) {
        val associationName = source.association.name
        if (!StringUtils.isBlank(associationName))
            return associationName;
        if (opposite != null && opposite.isNavigable()) {
            val primary = if (source.isPrimary()) source else opposite;
            val secondary = if (source.isPrimary()) opposite else source;
            return primary.getName() + "_" + secondary.getName();
        }
        return contextEntity.getName() + "_" + source.getName();
    }

    def toJpaPropertyAnnotation(Property property) {
        // careful when using KirraHelper as this is about storing data, not user capabilities
        
        // making an exception for username properties as they may not be necessarily filled in (unprovisioned user)
        val nullable = property.lower == 0 || property.userNameProperty
        val length = if (property.type.name == 'Memo') 16*1024 else 255 
        val unique = property.ID
        val insertable = true /* the back-end can anything) */ // !property.readOnly || !nullable
        val updatable = true /* the back-end can anything) */ // !property.readOnly || !insertable
        val values = #{'nullable' -> (nullable), 'updatable' -> (updatable), 'insertable' -> (insertable), 'unique' -> unique, 'length' -> length }
        val defaultValues = #{'nullable' -> true, 'updatable' -> true, 'insertable' -> true, 'unique' -> false, 'length' -> 255 }
        val nonDefaults = values.filter[ key, value | value != defaultValues.get(key) ]
        val pairs = nonDefaults.entrySet.map['''«key»=«value»''']
        '''
        @Column«IF !pairs.empty»(«pairs.join(', ')»)«ENDIF»
        «IF property.type.enumeration»@Enumerated(EnumType.STRING)«ENDIF»
        ''' 
    }
    
    def private String toSequenceName(Property property) {
    	//XXX this is going to cause trouble when people start using inheritance 
    	val entity = property.class_
    	val columnName = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(property.name), '_')
    	val tableName = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(entity.name), '_')
    	return '''«tableName»_«columnName»_seq'''
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
        import static util.PersistenceHelper.*;
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
    
    override generateAnonymousDataTypes(Class context) {
        // let services do it
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
    
    override generateRelationshipDerivationAsActivity(Activity derivation, Property relationship) {
        if (!derivation.queryPerformingActivity)
            return super.generateDerivedAttributeComputationAsActivity(relationship, derivation)
        // delegate query performing to the service
        '''return new «relationship.class_.name.toFirstUpper»Service().«relationship.generateAccessorName»(this);'''    
    }
}