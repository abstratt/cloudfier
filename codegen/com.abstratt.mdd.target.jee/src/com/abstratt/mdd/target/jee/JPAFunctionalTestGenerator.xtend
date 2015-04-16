package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.target.jse.AbstractGenerator
import com.abstratt.mdd.target.jse.FunctionalTestGenerator
import com.abstratt.mdd.target.jse.TestHelperGenerator
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.Class
import com.abstratt.mdd.target.jse.FunctionalTestBehaviorGenerator
import com.abstratt.mdd.target.jse.PlainJavaBehaviorGenerator
import static extension com.abstratt.mdd.target.jse.TestUtils.*
import com.abstratt.mdd.target.jse.IBehaviorGenerator
import org.eclipse.uml2.uml.Classifier
import com.abstratt.mdd.target.jse.TestUtils
import org.eclipse.uml2.uml.Action

class JPAFunctionalTestGenerator extends AbstractGenerator {

    IBehaviorGenerator childBehaviorGenerator
    JPAEntityGenerator childEntityGenerator
    CustomFunctionalTestGenerator childTestGenerator
    CustomTestHelperGenerator childTestHelperGenerator

    static class CustomFunctionalTestGenerator extends FunctionalTestGenerator {
        JPAFunctionalTestGenerator parent

        new(JPAFunctionalTestGenerator parent, IRepository repository) {
            super(repository)
            this.parent = parent
        }

        override generateTestClassPrefix() {
            '''
                private EntityManager em;
                private EntityTransaction tx;
            
                @Before
                public void initEM() {
                    this.em = Persistence.createEntityManagerFactory("integration-test").createEntityManager();
                    util.PersistenceHelper.setEntityManager(em);
                    this.tx = this.em.getTransaction();
                    this.tx.begin();
                }
                
                @After
                public void tearDown() {
                    if (tx != null)
                        tx.rollback();
                    if (em != null)
                        em.close();    
                }
            '''
        }
        
        
        override generateStandardImports() {
            '''
            «super.generateStandardImports()»
            «parent.childEntityGenerator.generateStandardImports()»
            import util.*;
            '''
        }
        
        override generateActivity(Activity activity) {
            parent.childBehaviorGenerator.generateActivity(activity)
        }
    }

    static class CustomBehaviorGenerator extends JPAClientBehaviorGenerator {
        JPAFunctionalTestGenerator parent

        new(JPAFunctionalTestGenerator parent, IRepository repository) {
            super(repository)
            this.parent = parent
        }
        
        override generateBasicTypeOperationCall(CallOperationAction action) {
            if (action.assertion)
                TestUtils.generateAssertOperationCall(action, [generateAction])
            else
                super.generateBasicTypeOperationCall(action)
        }
        
        override generateStatement(Action statementAction) {
            TestUtils.generateStatement(statementAction, [super.generateStatement(statementAction)])
        }
        
        override generateProviderReference(Classifier context, Classifier provider) {
            '''new «provider.name.toFirstUpper»Service()'''
        }
        
    }
    
    static class CustomTestHelperGenerator extends TestHelperGenerator {
        JPAFunctionalTestGenerator parent

        new(JPAFunctionalTestGenerator parent, IRepository repository) {
            super(repository)
            this.parent = parent
        }
        
        override createBehaviorGenerator() {
            new JPAClientBehaviorGenerator(repository)
        }
        
        override generateProviderReference(Classifier context, Classifier provider) {
            '''new «provider.name.toFirstUpper»Service(em)'''
        }
        
        override generateStandardImports() {
            '''
            «parent.childEntityGenerator.generateStandardImports()»
            '''
        }
        
        override generateTestHelperClassPrefix(Class helperClass) {
            '''
            '''
        }
        
    }

    new(IRepository repository) {
        super(repository)
        childBehaviorGenerator = new CustomBehaviorGenerator(this, repository)
        childEntityGenerator = new JPAEntityGenerator(repository)
        childTestGenerator = new CustomFunctionalTestGenerator(this, repository)
        childTestHelperGenerator = new CustomTestHelperGenerator(this, repository)
    }
    
    def generateTestClass(Class entity) {
        childTestGenerator.generateTestClass(entity)
    }
    
    def generateTestHelperClass(Class entity) {
        childTestHelperGenerator.generateTestHelperClass(entity)
    }
}
