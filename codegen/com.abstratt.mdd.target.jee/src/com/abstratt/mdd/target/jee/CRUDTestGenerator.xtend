package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.target.jse.PlainEntityGenerator
import java.util.UUID
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.Type

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.FeatureUtils.*


class CRUDTestGenerator extends PlainEntityGenerator {

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
            import util.PersistenceHelper;
            
            
            import «entityClass.nearestPackage.toJavaPackage».*;
            
            public class «entityClass.name»CRUDTest {
                private «entityClass.generateServiceClassName» «entityClass.generateServiceReference»;
            
                private EntityManager em;
                private EntityTransaction tx;
            
                @Before
                public void initEM() {
                    this.em = util.PersistenceHelper.createSchemaAndInitData();
                    util.PersistenceHelper.setEntityManager(em);
                    this.tx = this.em.getTransaction();
                    this.tx.begin();
                    this.«entityClass.generateServiceReference» = new «entityClass.generateServiceClassName»();
                }
                
                @After
                public void tearDown() {
                    if (tx != null)
                        tx.rollback();
                    if (em != null)
                        em.close();    
                }
                
                «entityClass.generateActionEnablementTest»
                «entityClass.generateCreateTest»
                «entityClass.generateRetrieveTest»
                «entityClass.generateUpdateTest»
                «entityClass.generateDeleteTest»
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
        «target».set«property.name.toFirstUpper»(«property.generateDefaultValue»);
        «ENDFOR»    
        '''
    }
    
    def generateActionEnablementTest(Class entityClass) {
    	if (!entityClass.instanceActions.exists[action | action.preconditions.exists[!parametrizedConstraint]]) return ''
        '''
        @Test
        public void checkActionEnablement() {
            «entityClass.generateServiceReference».getActionEnablements(Collections.singleton(1L));
        }
        '''
    }
    
    def generateCreateTest(Class entityClass) {
        '''
        @Test
        public void create() {
            «entityClass.name» toCreate = new «entityClass.name»();
            «entityClass.requiredProperties.generateValueAssignments('toCreate')»
            «entityClass.name» created = «entityClass.generateServiceReference».create(toCreate);
            Object id = created.getId();
            assertNotNull(id);
            em.clear();
            «entityClass.name» retrieved = «entityClass.generateServiceReference».find(id);
            assertNotNull(retrieved);
            assertEquals(id, retrieved.getId());
            «FOR property : entityClass.requiredProperties»
            «assertEquals(property.type, '''created.get«property.name.toFirstUpper»()''', '''retrieved.get«property.name.toFirstUpper»()''')»;
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
            «entityClass.generateServiceReference».create(toCreate1);
            «entityClass.name» toCreate2 = new «entityClass.name»();
            «entityClass.requiredProperties.generateValueAssignments('toCreate2')»
            «entityClass.generateServiceReference».create(toCreate2);
            em.clear();
            «entityClass.name» retrieved1 = «entityClass.generateServiceReference».find(toCreate1.getId());
            assertNotNull(retrieved1);
            assertEquals(toCreate1.getId(), retrieved1.getId());
            
            «entityClass.name» retrieved2 = «entityClass.generateServiceReference».find(toCreate2.getId());
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
            Object id = «entityClass.generateServiceReference».create(toCreate).getId();
            PersistenceHelper.flush(true);
            «entityClass.name» retrieved = «entityClass.generateServiceReference».find(id);
            «property.toJavaType» originalValue = retrieved.get«property.name.toFirstUpper»();
            retrieved.set«property.name.toFirstUpper»(«newValue»);
            «entityClass.generateServiceReference».update(retrieved);
            PersistenceHelper.flush(true);
            «entityClass.name» updated = «entityClass.generateServiceReference».find(id); 
            assertNotEquals(originalValue, updated.get«property.name.toFirstUpper»());
        }
        '''
    }
    
    def generateServiceClassName(Class entityClass) {
        '''«entityClass.name.toFirstUpper»Service'''
    }
    
    def generateServiceReference(Class entityClass) {
        '''«entityClass.name.toFirstLower»Service'''
    }
    
    def generateDeleteTest(Class entityClass) {
        '''
        @Test
        public void delete() {
            «entityClass.name» toDelete = new «entityClass.name»();
            «entityClass.requiredProperties.generateValueAssignments('toDelete')» 
            Object id = «entityClass.generateServiceReference».create(toDelete).getId();
            assertNotNull(«entityClass.generateServiceReference».find(id));
            «entityClass.generateServiceReference».delete(id);
            assertNull(«entityClass.generateServiceReference».find(id));
        }
        '''
    }
}
