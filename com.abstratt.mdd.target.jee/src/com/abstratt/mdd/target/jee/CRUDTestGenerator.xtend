package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.Class

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
            import java.util.stream.*;
            import java.util.function.*;
            import javax.persistence.*;
            import javax.enterprise.context.*;
            import javax.inject.*;
            import javax.ejb.*;
            
            
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
                «generateTearDown»
            } 
        '''
    }
    
    def generateCreateTest(Class entityClass) {
        '''
        @Test
        public void create() {
            «entityClass.name» toCreate = new «entityClass.name»(); 
            «entityClass.name» created = «entityClass.name.toFirstLower»Repository.create(toCreate);
            assertNotNull(created.getId());
            «entityClass.name» retrieved = «entityClass.name.toFirstLower»Repository.find(created.getId());
            assertNotNull(retrieved);
            assertEquals(created.getId(), retrieved.getId());
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
