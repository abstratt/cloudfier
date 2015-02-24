package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.Package
import org.eclipse.uml2.uml.Type

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.TemplateUtils.*

class FunctionalTestGenerator extends AbstractJavaGenerator {
    
    protected Iterable<Package> testedPackages
    
    protected Iterable<Class> testRelatedClasses
    protected Iterable<Class> testClasses
    
    new(IRepository repository) {
        super(repository)
        val allPackages = repository.getTopLevelPackages(null)
        val appPackages = allPackages.applicationPackages
        this.testedPackages = appPackages.filter[
            it.ownedTypes.exists[
                it.entity
            ]
        ]
        val testPackages = allPackages.filter[
            it.ownedTypes.exists[
                it.testClass
            ]
        ]
        this.testRelatedClasses = testPackages.map[ownedTypes.filter[it instanceof Class && !(it as Class).templateInstance]].flatten.map[it as Class]
        this.testClasses = testPackages.map[ownedTypes.filter[testClass]].flatten.map[it as Class]
    }
    
    def boolean isTestClass(Type toCheck) {
        toCheck.hasStereotype('Test')
    }
    
    private def boolean isTestCase(Operation toCheck) {
        toCheck.public && toCheck.ownedParameters.empty && toCheck.class_.isTestClass()
    }
    
    def CharSequence generateTestClass(Class testClass) {
        val testCases = testClass.operations.filter[isTestCase]
        '''
        package «testClass.packagePrefix».test;

        import org.junit.*;        
        «testedPackages.generateMany[p | '''
        import «p.toJavaPackage».*;
        ''']»

        public class «testClass.name» {
            «testCases.generateMany[generateTestCase]»
        } 
        '''
    }
    
    def CharSequence generateTestCase(Operation testCase) {
        '''
        @Test
        public void «testCase.name»() {
            
        }
        '''
//        val testBehavior = testCase.methods.get(0) as Activity
//        var rootAction = testBehavior.rootAction
//        while (rootAction.findTerminals.size() == 1 && rootAction.findTerminals.get(0).eClass == UMLPackage.Literals.STRUCTURED_ACTIVITY_NODE)
//            rootAction = rootAction.findTerminals.get(0) as StructuredActivityNode
//        val failureExpected = testCase.hasStereotype('Failure')
//        val expectedFailureContext = StereotypeUtils.getValue(testCase, 'Failure', "context")
//        val expectedFailureConstraint = StereotypeUtils.getValue(testCase, 'Failure', "constraint")
//        
//        // extracted as a block as we generate from different places depending on whether
//        // a failure is expected
//        val generateCoreBehavior = [CharSequence success, CharSequence error |
//            '''
//               var behavior = function() {
//                   «generateActivityRootAction(testBehavior)»
//               };
//               behavior().then(«success», «error»);
//            '''
//        ]
//        
//        /*
//         * We need to break the behavior into a sequence of blocks that run in order but asynchronously.
//         * 
//         */
//        '''
//        test('«testCase.name»', function(done) {
//            «IF failureExpected»
//            «generateCoreBehavior.apply('''
//            function() {
//                done(new Error("Error expected («expectedFailureConstraint»), none occurred"));
//            }''', '''
//            function(error) {
//                try {
//                    «IF expectedFailureContext != null»
//                    assert.equal(error.name, 'ValidationError');
//                    assert.ok(error.errors.«expectedFailureContext»);
//                    «ELSE»
//                    assert.equal(error.name, 'Error');
//                    assert.ok(error.context);
//                    assert.equal(error.constraint, '«expectedFailureConstraint»');
//                    «ENDIF»
//                    done();
//                } catch (e) {
//                    done(e);
//                }                
//            }
//            '''.toString.trim)»
//            «ELSE»
//            «generateCoreBehavior.apply('done', 'done')»
//            «ENDIF»
//        });
//        '''
    }
    
    def render(CharSequence cs, String quote) {
        '''«quote»«cs.toString.replaceAll(quote, '\\\'' + quote).replaceAll('\n', '\\\'')»«quote»'''
    }
    
    def generateRootSuite(String applicationName, Iterable<Class> testClasses) {
        '''
        «testClasses.map[
            '''require('./«name».js');'''
        ].join('\n')»
        '''
    }
}
