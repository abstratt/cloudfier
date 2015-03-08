package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.target.jse.ServiceGenerator
import org.eclipse.uml2.uml.Classifier
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.VisibilityKind
import org.eclipse.uml2.uml.Parameter
import static extension com.abstratt.kirra.mdd.core.KirraHelper.*

class JPAServiceGenerator extends ServiceGenerator {

    JPABehaviorGenerator behaviorGenerator

    new(IRepository repository) {
        super(repository)
        behaviorGenerator = new CustomJPABehaviorGenerator(this, repository)
    }

    static class CustomJPABehaviorGenerator extends JPABehaviorGenerator {
        JPAServiceGenerator parent

        new(JPAServiceGenerator parent, IRepository repository) {
            super(repository)
            this.parent = parent
        }

        override generateJavaMethodBody(Operation operation) {
            '''
                CriteriaBuilder cb = entityManager.getCriteriaBuilder();
                CriteriaQuery<?> cq = cb.createQuery();
                «super.generateJavaMethodBody(operation)»
            '''
        }

        override generateJavaMethodParameter(Parameter parameter) {
            val parameterType = if (parameter.multivalued) '''CriteriaQuery<«parameter.type.toJavaType»>''' else
                    parameter.type.toJavaType
            '''«parameterType» «parameter.name»'''
        }
        
        override generateOperationReturnType(Operation operation) {
            // methods returning collections will usually return lists (due to Query#getResultList())
            val result = operation.getReturnResult()
            if (result?.multivalued)
                '''Collection<«result.type.toJavaType»> '''
            else
                super.generateOperationReturnType(operation)
        }

        override generateProviderReference(Classifier context, Classifier provider) {
            if (context == provider)
                'this'
            else 
                '''new «provider.name.toFirstUpper»Service()'''
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
                return entityManager.createQuery(cq.select(cq.from(«entity.name».class))).getResultList();
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
                return entityManager.createQuery(cq.select(cq.from(«relationship.type.name».class)).where(cb.equal(cq.from(«relationship.type.name».class).get("«otherEnd.name»"), «otherEnd.name»))).getResultList();
            }
        '''
        ]
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
