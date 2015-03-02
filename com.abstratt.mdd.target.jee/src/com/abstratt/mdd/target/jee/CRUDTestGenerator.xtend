package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.Class
import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import org.eclipse.uml2.uml.Type
import com.abstratt.kirra.TypeRef

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
    
    override generateSampleValue(Type type) {
        if (type.primitive && type.name == 'Date')
            'new Timestamp(new Date().getTime() + 24 * 60 * 60 * 1000L)'
        else
            super.generateSampleValue(type)
    }
    
    def generateCreateTest(Class entityClass) {
        val property = entityClass.properties.findFirst[!derived]
        val sampleValue = property.type.generateDefaultValue
        
        '''
        @Test
        public void create() {
            «entityClass.name» toCreate = new «entityClass.name»();
            toCreate.«property.name» = «sampleValue»; 
            «entityClass.name» created = «entityClass.name.toFirstLower»Repository.create(toCreate);
            Object id = created.getId();
            assertNotNull(id);
            em.clear();
            «entityClass.name» retrieved = «entityClass.name.toFirstLower»Repository.find(id);
            assertNotNull(retrieved);
            assertEquals(id, retrieved.getId()); 
            «assertEquals(property.type, '''created.«property.name»''', '''retrieved.«property.name»''')»;
        }
        '''
    }
    
    def assertEquals(Type type, CharSequence actual, CharSequence expected) {
        if (type.primitive && type.name == 'Date')
            '''assertTrue((«actual».getTime() - «expected».getTime()) < 10000)'''
        else    
            '''assertEquals(«actual», «expected»)'''
    }
    
    def generateRetrieveTest(Class entityClass) {
        '''
        @Test
        public void retrieve() {
            «entityClass.name» toCreate1 = new «entityClass.name»();
            «entityClass.name.toFirstLower»Repository.create(toCreate1);
            «entityClass.name» toCreate2 = new «entityClass.name»();
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
        val property = entityClass.properties.findFirst[!derived]
        val originalValue = property.type.generateDefaultValue
        val newValue = property.type.generateSampleValue
        '''
        @Test
        public void update() {
            «entityClass.name» toCreate = new «entityClass.name»(); 
            toCreate.«property.name» = «originalValue»; 
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
            «entityClass.name» toCreate = new «entityClass.name»(); 
            Object id = «entityClass.name.toFirstLower»Repository.create(toCreate).getId();
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
