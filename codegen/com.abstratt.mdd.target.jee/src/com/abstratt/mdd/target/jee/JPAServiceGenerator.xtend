package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.target.base.IBehaviorGenerator.SimpleContext
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
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import static extension com.abstratt.mdd.core.util.FeatureUtils.*
import static extension com.abstratt.mdd.core.util.ConstraintUtils.*
import com.abstratt.mdd.core.util.ConstraintUtils
import com.abstratt.mdd.core.util.ActivityUtils
import org.eclipse.uml2.uml.OutputPin
import org.eclipse.uml2.uml.ReadVariableAction
import org.eclipse.uml2.uml.AggregationKind
import org.eclipse.uml2.uml.ReadStructuralFeatureAction
import org.eclipse.uml2.uml.CallOperationAction

class JPAServiceGenerator extends ServiceGenerator {

    JPABehaviorGenerator behaviorGenerator

    new(IRepository repository) {
        super(repository)
        behaviorGenerator = new CustomJPABehaviorGenerator(this, repository)
    }
    
    override isServiceOperation(Operation op) {
        super.isServiceOperation(op) || op.activity?.queryPerformingActivity
    }
    
    static class CustomJPABehaviorGenerator extends JPAJPQLServiceBehaviorGenerator {
        JPAServiceGenerator parent

        new(JPAServiceGenerator parent, IRepository repository) {
            super(repository)
            this.parent = parent
        }
    }

    override generateJavaClassPrefix(Class entity) {
        '''
            
            public «entity.name»Service() {
            }
            
            private EntityManager getEntityManager() {
                return util.PersistenceHelper.getEntityManager();
            }
            
            «entity.generateDerivedRelationshipAccessors»
            
            «entity.generateCreate»
            «entity.generateFind»
            «entity.generateRefresh»
            «entity.generateMerge»
            «entity.generateFindAll»
            «entity.generateUpdate»
            «entity.generateDelete»
            «entity.generateRelationshipDomain»
            «entity.generateActionParameterDomain»
            «entity.generateActionEnablement»
        '''
    }
    
    def override boolean isProviderFor(Class candidate, Class entity) {
        entity != candidate
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
                getEntityManager().persist(toCreate);
                return toCreate;
            }
        '''
    }

    def generateFind(Classifier entity) {
        '''
            public «entity.name» find(Object id) {
                return getEntityManager().find(«entity.name».class, id);
            }
        '''
    }
    
    def generateRefresh(Classifier entity) {
        '''
            public «entity.name» refresh(«entity.name» toRefresh) {
                getEntityManager().refresh(toRefresh);
                return toRefresh; 
            }
        '''
    }
    
    def generateMerge(Classifier entity) {
        '''
            public «entity.name» merge(«entity.name» toMerge) {
                return getEntityManager().merge(toMerge);
            }
        '''
    }

    def generateFindAll(Classifier entity) {
        return generateFindAll("findAll", entity)
    }
    
    def generateFindAll(String javaMethodName, Classifier entity) {
        '''
            public List<«entity.name»> «javaMethodName»() {
                «generateFindAllCore(entity)»
            }
        '''
    }
    
    def generateFindAllCore(Classifier entity) {
        val entityAlias = entity.name.toFirstLower
        '''
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<«entity.name»> cq = cb.createQuery(«entity.name».class);
        Root<«entity.name»> «entityAlias» = cq.from(«entity.name».class);
        return getEntityManager().createQuery(cq.select(«entityAlias»).orderBy(cb.asc(«entityAlias».get("id"))).distinct(true)).getResultList();
        '''
    }
    
    
    override generateRelated(Classifier entity) {
        val relationships = getRelationships(entity).filter[!derived].map[otherEnd].filter[multiple || !navigable]
        relationships.generateMany[ relationship |
            val otherEnd = relationship.otherEnd
        '''
            public List<«relationship.type.name»> find«relationship.name.toFirstUpper»By«otherEnd.name.toFirstUpper»(«otherEnd.type.name» «otherEnd.name») {
                CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
                CriteriaQuery<«relationship.type.name»> cq = cb.createQuery(«relationship.type.name».class);
                Root<«relationship.type.name»> root = cq.from(«relationship.type.name».class);
                return getEntityManager().createQuery(cq.select(root).where(cb.equal(root.get("«otherEnd.name»"), «otherEnd.name»)).distinct(true)).getResultList();
            }
        '''
        ]
    }
    
    def CharSequence generateRelationshipDomain(Classifier entity) {
        val relationships = getRelationships(entity).filter[!derived && navigable && aggregation != AggregationKind.COMPOSITE_LITERAL]
        relationships.generateMany[ relationship |
            val constraints = relationship.findInvariantConstraints
            val methodName = '''getDomainFor«relationship.name.toFirstUpper»'''
            '''
            public List<«relationship.type.name»> «methodName»(«entity.name» context) {
            	«IF constraints.isEmpty»
            	return new «relationship.type.name»Service().findAll();
    	        «ELSE»
            	//TODO honor constraints
        	    «generateFindAllCore(relationship.type as Class)»
                «ENDIF»
            }
            '''
        ]
    }
    
    def CharSequence generateActionEnablement(Class entity) {
        val context = 'context'
        val actions = entity.instanceActions.filter[action | action.preconditions.exists[!parametrizedConstraint]]
        if (actions.empty) return ''
        
        val conditions = actions.map[generateActionPredicateExpression(entity, context, it).toString]
        val needsCurrentUser = conditions.exists[contains('_currentUser')]
        val userClass = if (needsCurrentUser) userClasses.head
        
        '''
        public static class ActionEnablements {
        	public final long id;
        	«actions.generateMany([
        		'''public final boolean «name»Enabled;''' 
        	])»
        	
        	public long getId() {
        		return id;
        	}
        	
        	«actions.generateMany([
        		'''
        		public boolean is«name.toFirstUpper»Enabled() {
        			return «name»Enabled;
        		}
    			''' 
        	])»
        	
        	public ActionEnablements(«actions.generateMany([
        		'''boolean «name»Enabled''' 
        	    ], ', ')», long id) {
                this.id = id;
        	    «actions.generateMany([
        		'''this.«name»Enabled = «name»Enabled;''' 
        	    ])»	
        	}	
        }
        
        public List<ActionEnablements> getActionEnablements(Collection<Long> contextIds) {
        	if (contextIds.isEmpty()) {
        		return Collections.emptyList();
        	}
            return getEntityManager()
                .createQuery(
                    "SELECT NEW «entity.nearestPackage.toJavaPackage».«entity.toJavaType»Service$ActionEnablements("
                        «conditions.map['''+ "«it»,"'''].join('\n')»
                        + "context.id"
                    + ") FROM «entity.name» context WHERE context.id IN :ids", ActionEnablements.class
                )
                «IF needsCurrentUser»
                .setParameter("_currentUser", new «userClass.toJavaType»())
                «ENDIF»
                .setParameter("ids", contextIds)
                .getResultList(); 
        }
        '''
    }    
    
    def CharSequence generateActionPredicateExpression(Class contextEntity, String context, Operation action) {
        val predicateActivities = action.preconditions.filter[!parametrizedConstraint].map[specification.resolveBehaviorReference as Activity]
        val condition = {
	        val alias = '''«action.name»_'''
	        predicateActivities.generateMany([
	        	'''(«generateParameterlessPreconditionQuery(alias, context, it)»)'''
	        ], 
	        ''' AND ''')
        }
    	return '''«condition» as «action.name»Enabled'''
    }
    
    def CharSequence generateParameterlessPreconditionQuery(String alias, String context, Activity preconditionActivity) {
    	val newContext = new SimpleContext(alias)
        behaviorGenerator.runInContext(newContext, 
        	[
        		ActivityContext.generateInNewContext(preconditionActivity, [context], [
					val filterGenerator = new JPQLFilterActionGenerator(repository) {
						override generateVariableNameReplacement(OutputPin source, ReadVariableAction action) {
							if (source == null)
								return context.generateCurrentReference
							return super.generateVariableNameReplacement(source, action)
						}
						
						override generateCallOperationAction(CallOperationAction action) {
							if (action.operation.static) {
								if ("mdd_types::System::user".equals(action.operation.qualifiedName)) {
									return ':_currentUser'
								}
							}
							
							return super.generateCallOperationAction(action)
						}
					}
					filterGenerator.generateAction(preconditionActivity.rootAction.findSingleStatement.sourceAction)
				])
        	]
    	)
    }
    
    def CharSequence generateActionParameterDomain(Class entity) {
        val actions = entity.instanceActions.filter[!parameters.empty]
        val context = 'context'
        actions.generateMany[ action |
            action.parameters.filter[hasParameterConstraints].generateMany[ parameter |
                '''
                public List<«parameter.type.name»> getParameterDomainFor«parameter.name.toFirstUpper»To«action.name.toFirstUpper»(«entity.name» «context») {
                    return getEntityManager().createQuery(
                        "«generateActionParameterDomainQuery(entity, context, parameter)»", «parameter.type.name».class
                    ).setParameter("«context»", «context»).getResultList(); 
                }
                '''
            ]
        ]
    }
    
    def CharSequence generateActionParameterDomainQuery(Class contextEntity, String context, Parameter parameter) {
        val predicateActivities = parameter.parameterConstraints.map[specification.resolveBehaviorReference as Activity]
        val alias = '''«parameter.type.name.toFirstLower»_'''
        val condition = predicateActivities.generateMany([
        	'''(«generateParameteredPreconditionQuery(alias, context, it)»)'''
        ], " AND ")
    	'''SELECT DISTINCT «alias» FROM «parameter.type.name» «alias», «contextEntity.name» «context» WHERE «condition» AND («context» = :«context»)'''
    }     
    
    def CharSequence generateParameteredPreconditionQuery(String alias, String context, Activity preconditionActivity) {
    	val newContext = new SimpleContext(alias)
        behaviorGenerator.runInContext(newContext, 
        	[
        		ActivityContext.generateInNewContext(preconditionActivity, [context], [
					val filterGenerator = new JPQLFilterActionGenerator(repository) {
						override generateVariableNameReplacement(OutputPin source, ReadVariableAction action) {
							if (source == null)
								return context.generateCurrentReference
							return super.generateVariableNameReplacement(source, action)
						}
					}
					filterGenerator.generateAction(preconditionActivity.rootAction.findSingleStatement.sourceAction)
				])
        	]
    	)
    }
    
    def generateDerivedRelationshipAccessor(Class entity, Property derivedRelationship) {
        '''
            public «derivedRelationship.toJavaType» «derivedRelationship.generateAccessorName»(«entity.name» context) {
                «behaviorGenerator.generateJavaMethodBody(derivedRelationship.derivation)»
            }
        '''
    }

    def generateUpdate(Classifier entity) {
        '''
            public «entity.name» update(«entity.name» toUpdate) {
                assert toUpdate.getId() != null;
                getEntityManager().persist(toUpdate);
                return toUpdate;
            }
        '''
    }

    def generateDelete(Classifier entity) {
        '''
            public void delete(Object id) {
                «entity.name» found = getEntityManager().find(«entity.name».class, id);
                if (found != null)
                    getEntityManager().remove(found);
            }
        '''
    }
}
