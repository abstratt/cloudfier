package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.target.jse.BehaviorlessClassGenerator
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Classifier

class RepositoryGenerator extends BehaviorlessClassGenerator {
    
    new(IRepository repository) {
        super(repository)
    }
    
    def generateRepository(Class entity) {
        '''
        package «entity.packagePrefix»;

        «generateStandardImports»        
        
        «entity.generateImports»

        @Stateless
        public class «entity.name»Repository {
            
            «entity.generateCreate»
            «entity.generateFind»
            «entity.generateUpdate»
            «entity.generateDelete»
        }
        '''
    }
    
    def generateStandardImports() {
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
    
    def generateCreate(Classifier entity) {
        '''
        public «entity.name» create(«entity.name» toCreate) {
            entityManager.persist(toCreate);
            return toCreate;
        }
        '''
    }
    
    def generateFind(Classifier entity) {
        '''
        public «entity.name» find(Object id) {
            return entityManager.find(«entity.name».class, id);
        }
        '''
    }
    
    def generateUpdate(Classifier entity) {
        '''
        public «entity.name» update(«entity.name» toUpdate) {
            assert toUpdate.getId() != null;
            entityManager.persist(toUpdate);
            return toUpdate;
        }
        '''
    }
    
    def generateDelete(Classifier entity) {
        '''
        public void delete(Object id) {
            «entity.name» found = entityManager.find(«entity.name».class, id);
            if (found != null)
                entityManager.remove(found);
        }
        '''
    }
}