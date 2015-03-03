package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.Class
import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import org.eclipse.uml2.uml.Type
import com.abstratt.kirra.TypeRef
import org.eclipse.uml2.uml.Property
import java.util.UUID

class CRUDTestGenerator extends EntityGenerator {

    new(IRepository repository) {
        super(repository)
    }
    
    def CharSequence generateCRUDTestClass(Class entityClass) {
        '''
            package «entityClass.packagePrefix»;
            
            import org.junit.*;  
            import static org.junit.Assert.*;  
            import java.util.*;
            import java.util.Date;
            import java.util.stream.*;
            import java.text.*;
            import java.util.function.*;
            import javax.persistence.*;
            import javax.enterprise.context.*;
            import javax.inject.*;
            import javax.ejb.*;
            import java.sql.*;
            
            
            import «entityClass.nearestPackage.toJavaPackage».*;
            
            public class «entityClass.name»CRUDTest {
                private EntityManager em;
                private EntityTransaction tx;
                private «entityClass.name»Repository «entityClass.name.toFirstLower»Repository;
            
                @Before
                public void initEM() {
                    this.em = Persistence.createEntityManagerFactory("integration-test").createEntityManager();
                    this.tx = this.em.getTransaction();
                    this.tx.begin();
                    this.«entityClass.name.toFirstLower»Repository = new «entityClass.name»Repository();
                    this.«entityClass.name.toFirstLower»Repository.entityManager = this.em; 
                }
                «entityClass.generateCreateTest»
                «entityClass.generateRetrieveTest»
                «entityClass.generateUpdateTest»
                «entityClass.generateDeleteTest»
                «generateTearDown»
            } 
        '''
    }
    
    override generateDefaultValue(Type type) {
        if (type.primitive && type.name == 'Date')
            '''new Timestamp(System.currentTimeMillis())'''
        else
            super.generateDefaultValue(type)
    }
    
    def generateDefaultValue(Property property) {
        if (property.ID && property.type.primitive) {
            if (property.type.name == 'String')
                return '''"«UUID.randomUUID.toString»"'''
        }
        property.type.generateDefaultValue
    }
    
    def getRequiredProperties(Class entityClass) {
        entityClass.properties.filter[lower > 0 && !derived && (defaultValue == null)]
    }
    
    def generateValueAssignments(Iterable<Property> properties, String target) {
        '''
        «FOR property : properties»
        «target».«property.name» = «property.generateDefaultValue»;
        «ENDFOR»    
        '''
    }
    
    def generateCreateTest(Class entityClass) {
        '''
        @Test
        public void create() {
            «entityClass.name» toCreate = new «entityClass.name»();
            «entityClass.requiredProperties.generateValueAssignments('toCreate')»
            «entityClass.name» created = «entityClass.name.toFirstLower»Repository.create(toCreate);
            Object id = created.getId();
            assertNotNull(id);
            em.clear();
            «entityClass.name» retrieved = «entityClass.name.toFirstLower»Repository.find(id);
            assertNotNull(retrieved);
            assertEquals(id, retrieved.getId());
            «FOR property : entityClass.requiredProperties»
            «assertEquals(property.type, '''created.«property.name»''', '''retrieved.«property.name»''')»;
            «ENDFOR»
        }
        '''
    }
    
    def assertEquals(Type type, CharSequence actual, CharSequence expected) {
        if (type.primitive) {
            if (type.name == 'Date')
                return '''assertTrue((«actual».getTime() - «expected».getTime()) < 10000)'''
            else if (type.name == 'Double')    
                return '''assertEquals(«actual», «expected», 0.00000001)'''
        }
        '''assertEquals(«actual», «expected»)'''
    }
    
    def generateRetrieveTest(Class entityClass) {
        '''
        @Test
        public void retrieve() {
            «entityClass.name» toCreate1 = new «entityClass.name»();
            «entityClass.requiredProperties.generateValueAssignments('toCreate1')»
            «entityClass.name.toFirstLower»Repository.create(toCreate1);
            «entityClass.name» toCreate2 = new «entityClass.name»();
            «entityClass.requiredProperties.generateValueAssignments('toCreate2')»
            «entityClass.name.toFirstLower»Repository.create(toCreate2);
            em.clear();
            «entityClass.name» retrieved1 = «entityClass.name.toFirstLower»Repository.find(toCreate1.getId());
            assertNotNull(retrieved1);
            assertEquals(toCreate1.getId(), retrieved1.getId());
            
            «entityClass.name» retrieved2 = «entityClass.name.toFirstLower»Repository.find(toCreate2.getId());
            assertNotNull(retrieved2);
            assertEquals(toCreate2.getId(), retrieved2.getId());
        }
        '''
    }
    
    def generateUpdateTest(Class entityClass) {
        val property = entityClass.requiredProperties.findFirst[!readOnly]
        
        if (property == null)
            return ''
        
        val newValue = property.type.generateSampleValue
        '''
        @Test
        public void update() {
            «entityClass.name» toCreate = new «entityClass.name»();
            «entityClass.requiredProperties.generateValueAssignments('toCreate')» 
            Object id = «entityClass.name.toFirstLower»Repository.create(toCreate).getId();
            em.flush();
            em.clear();
            «entityClass.name» retrieved = «entityClass.name.toFirstLower»Repository.find(id);
            «property.toJavaType» originalValue = retrieved.«property.name»;
            retrieved.«property.name» = «newValue»;
            «entityClass.name.toFirstLower»Repository.update(retrieved);
            em.flush();
            em.clear();
            «entityClass.name» updated = «entityClass.name.toFirstLower»Repository.find(id); 
            assertNotEquals(originalValue, updated.«property.name»);
        }
        '''
    }
    
    def generateDeleteTest(Class entityClass) {
        '''
        @Test
        public void delete() {
            «entityClass.name» toDelete = new «entityClass.name»();
            «entityClass.requiredProperties.generateValueAssignments('toDelete')» 
            Object id = «entityClass.name.toFirstLower»Repository.create(toDelete).getId();
            assertNotNull(«entityClass.name.toFirstLower»Repository.find(id));
            «entityClass.name.toFirstLower»Repository.delete(id);
            assertNull(«entityClass.name.toFirstLower»Repository.find(id));
        }
        '''
    }
    
    def generateTearDown() {
        '''
        @After
        public void tearDown() {
            if (tx != null )
                tx.rollback();  
        }
        '''
    }
}
