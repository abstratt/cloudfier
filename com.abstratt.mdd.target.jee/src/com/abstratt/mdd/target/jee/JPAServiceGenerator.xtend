package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.target.jse.IBehaviorGenerator.SimpleContext
import com.abstratt.mdd.target.jse.ServiceGenerator
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Classifier
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.Parameter
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.VisibilityKind

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.target.jee.JPAHelper.*

class JPAServiceGenerator extends ServiceGenerator {

    JPABehaviorGenerator behaviorGenerator

    new(IRepository repository) {
        super(repository)
        behaviorGenerator = new CustomJPABehaviorGenerator(this, repository)
    }
    
    override isServiceOperation(Operation op) {
        super.isServiceOperation(op) || op.activity?.queryPerformingActivity
    }
    
    static class CustomJPABehaviorGenerator extends JPAServiceBehaviorGenerator {
        JPAServiceGenerator parent

        new(JPAServiceGenerator parent, IRepository repository) {
            super(repository)
            this.parent = parent
        }
    }

    override generateJavaClassPrefix(Class entity) {
        '''
            @Inject
            EntityManager entityManager;
            
            public «entity.name»Service() {
                this(util.PersistenceHelper.getEntityManager());       
            }
            
            public «entity.name»Service(EntityManager entityManager) {
                this.entityManager = entityManager;
            }
            
            «entity.generateDerivedRelationshipAccessors»
            
            «entity.generateCreate»
            «entity.generateFind»
            «entity.generateRefresh»
            «entity.generateMerge»
            «entity.generateFindAll»
            «entity.generateUpdate»
            «entity.generateDelete»
            «entity.generateRelated»
        '''
    }
    
    def CharSequence generateDerivedRelationshipAccessors(Class entity) {
        val derivedRelationships = getRelationships(entity).filter[derived]
        val derivedRelationshipsWithQuery = derivedRelationships.filter[derivation?.queryPerformingActivity]
        return derivedRelationshipsWithQuery.map[generateDerivedRelationshipAccessor(entity, it)].join
    }
    
    override generateJavaClass(Class entity) {
        '''
            @Stateless
            «super.generateJavaClass(entity)»
        '''
    }

    override generateServiceOperation(Operation serviceOperation) {
        behaviorGenerator.generateJavaMethod(serviceOperation, serviceOperation.visibility, false)
    }

    override generateStandardImports() {
        '''
            «super.generateStandardImports()»
            import javax.persistence.*;
            import javax.persistence.criteria.*;
            import javax.inject.*;
            import javax.ejb.*;
            import javax.enterprise.event.*;
            import javax.enterprise.context.*;
            import static util.PersistenceHelper.*;
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
    
    def generateRefresh(Classifier entity) {
        '''
            public «entity.name» refresh(«entity.name» toRefresh) {
                entityManager.refresh(toRefresh);
                return toRefresh; 
            }
        '''
    }
    
    def generateMerge(Classifier entity) {
        '''
            public «entity.name» merge(«entity.name» toMerge) {
                return entityManager.merge(toMerge);
            }
        '''
    }

    def generateFindAll(Classifier entity) {
        '''
            public List<«entity.name»> findAll() {
                CriteriaQuery<«entity.name»> cq = entityManager.getCriteriaBuilder().createQuery(«entity.name».class);
                return entityManager.createQuery(cq.select(cq.from(«entity.name».class)).distinct(true)).getResultList();
            }
        '''
    }
    
    def generateRelated(Classifier entity) {
        val nonNavigableRelationships = getRelationships(entity).filter[!derived].map[otherEnd].filter[!navigable]
        nonNavigableRelationships.generateMany[ relationship |
            val otherEnd = relationship.otherEnd
        '''
            public List<«relationship.type.name»> find«relationship.name.toFirstUpper»By«otherEnd.name.toFirstUpper»(«otherEnd.type.name» «otherEnd.name») {
                CriteriaBuilder cb = entityManager.getCriteriaBuilder();
                CriteriaQuery<«relationship.type.name»> cq = cb.createQuery(«relationship.type.name».class);
                return entityManager.createQuery(cq.select(cq.from(«relationship.type.name».class)).where(cb.equal(cq.from(«relationship.type.name».class).get("«otherEnd.name»"), «otherEnd.name»)).distinct(true)).getResultList();
            }
        '''
        ]
    }
    
    def generateDerivedRelationshipAccessor(Class entity, Property derivedRelationship) {
        val context = new SimpleContext("context")
        behaviorGenerator.enterContext(context)
        try {
        return
        '''
            public «derivedRelationship.toJavaType» «derivedRelationship.generateAccessorName»(«entity.name» context) {
                «behaviorGenerator.generateJavaMethodBody(derivedRelationship.derivation)»
            }
        '''
        } finally {
            behaviorGenerator.leaveContext(context)
        }
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
