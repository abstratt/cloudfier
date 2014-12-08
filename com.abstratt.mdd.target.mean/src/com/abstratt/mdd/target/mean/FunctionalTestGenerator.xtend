    package com.abstratt.mdd.target.mean

import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Classifier
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.Type

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.FeatureUtils.*
import static extension com.abstratt.mdd.core.util.TemplateUtils.*
import org.eclipse.uml2.uml.AddVariableValueAction
import org.eclipse.uml2.uml.StructuredActivityNode
import org.eclipse.uml2.uml.UMLPackage
import com.abstratt.mdd.core.util.StereotypeUtils

class FunctionalTestGenerator extends ModelGenerator {
    
    protected Iterable<Class> testRelatedClasses
    
    new(IRepository repository) {
        super(repository)
        val allPackages = repository.getTopLevelPackages(null)
        val testPackages = allPackages.filter[
            it.ownedTypes.exists[
                it.hasStereotype('Test')
            ]
        ]
        this.testRelatedClasses = testPackages.map[ownedTypes.filter[it instanceof Class && !(it as Class).templateInstance]].flatten.map[it as Class]
    }
    
    def override generateIndex() {
        '''
        require('../models/index.js');
        require('./CRUD.js');
        «testRelatedClasses.map['''require('./«it.name».js');'''].join('\n')»
        '''
    }
    
    def CharSequence generateSuiteHelper(Class helperClass) {
        '''
        require('../models/index.js');
        
        var Q = require("q");
        «entities.map['''var «name» = require('../models/«name».js');'''].join('\n')»
        
        var «helperClass.name» = {
            «helperClass.operations.map[ op |
                val method = op.methods.get(0) as Activity
                '''
                «op.name» : function(«op.ownedParameters.inputParameters.map[name].join(', ')») {
                    «method.generateActivity»
                }'''
            ].join(',\n')»
        };
        
        exports = module.exports = «helperClass.name»; 
        '''
    }
    
    def CharSequence generateTestSuite(Class testClass) {
        val helperClasses = testClass.nearestPackage.ownedTypes.filter[ Type type | 
            type instanceof Class && !type.hasStereotype('Test')
        ].map [it as Class].filter[!it.templateInstance]
        
        val isTestCase = [Operation it | it.public && it.ownedParameters.empty]
        val testCases = testClass.operations.filter(isTestCase)
        val helpers = testClass.operations.filter[!isTestCase.apply(it)]
        
        '''
        
        var assert = require("assert");
        var Q = require("q");
        require('../models/index.js');        
        
        
        «entities.map['''var «name» = require('../models/«name».js');'''].join('\n')»

        «helperClasses.map['''var «it.name» = require('./«it.name».js');'''].join('\n')»

        «IF !helpers.empty»
        var «testClass.name» = {
            «helpers.map[generateTestCaseHelper].join(',\n')»
        };
        «ENDIF»
        
        suite('«applicationName» functional tests - «testClass.name»', function() {
            this.timeout(100000);

            «testCases.map[generateTestCase].join()»
        });
        
        '''
    }
    
    def CharSequence generateTestCaseHelper(Operation op) {
        '''
            «op.name» : function(«op.ownedParameters.inputParameters.map[name].join(', ')») {
                «op.activity.generateActivityRootAction»
            }
        '''
    }
    
    override generateAddVariableValueAction(AddVariableValueAction action) {
        super.generateAddVariableValueAction(action)
    }
    
    override generateVariables(StructuredActivityNode node) {
        super.generateVariables(node)
    }
    
    def CharSequence generateTestCase(Operation testCase) {
        val testBehavior = testCase.methods.get(0) as Activity
        var rootAction = testBehavior.rootAction
        while (rootAction.findTerminals.size() == 1 && rootAction.findTerminals.get(0).eClass == UMLPackage.Literals.STRUCTURED_ACTIVITY_NODE)
            rootAction = rootAction.findTerminals.get(0) as StructuredActivityNode
        val failureExpected = testCase.hasStereotype('Failure')
        val expectedFailureContext = StereotypeUtils.getValue(testCase, 'Failure', "context")
        val expectedFailureConstraint = StereotypeUtils.getValue(testCase, 'Failure', "constraint")
        
        // extracted as a block as we generate from different places depending on whether
        // a failure is expected
        val generateCoreBehavior = [CharSequence success, CharSequence error |
            '''
               var behavior = function() {
                   «generateActivityRootAction(testBehavior)»
               };
               behavior().then(«success», «error»);
            '''
        ]
        
        /*
         * We need to break the behavior into a sequence of blocks that run in order but asynchronously.
         * 
         */
        '''
        test('«testCase.name»', function(done) {
            «IF failureExpected»
            «generateCoreBehavior.apply('''
            function() {
                done(new Error("Error expected («expectedFailureConstraint»), none occurred"));
            }''', '''
            function(error) {
                try {
                    «IF expectedFailureContext != null»
                    assert.equal(error.name, 'ValidationError');
                    assert.ok(error.errors.«expectedFailureContext»);
                    «ELSE»
                    assert.equal(error.name, 'Error');
                    assert.ok(error.context);
                    assert.equal(error.constraint, '«expectedFailureConstraint»');
                    «ENDIF»
                    done();
                } catch (e) {
                    done(e);
                }                
            }
            '''.toString.trim)»
            «ELSE»
            «generateCoreBehavior.apply('done', 'done')»
            «ENDIF»
        });
        '''
    }
    
    override CharSequence generateBasicTypeOperationCall(Classifier classifier, CallOperationAction action) {
        val operation = action.operation
        
        if (classifier != null)
            return switch (classifier.qualifiedName) {
                case 'mdd_types::Assert' : switch (operation.name) {
                    case 'isNull' : '''assert.ok(«generateAction(action.arguments.head)» == null)'''
                    case 'isNotNull' : '''assert.ok(«generateAction(action.arguments.head)» != null)'''
                    case 'isTrue' : '''assert.strictEqual(«generateAction(action.arguments.head)», true)'''
                    case 'areEqual' : '''assert.equal(«generateAction(action.arguments.head)», «generateAction(action.arguments.tail.head)»)'''
                    default : '''Unsupported Assert operation: «operation.name»'''
                }
                default: super.generateBasicTypeOperationCall(classifier, action)
            }
        super.generateBasicTypeOperationCall(classifier, action)         
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
    
    override generateStructuredActivityNodeAsBlock(StructuredActivityNode node) {
        super.generateStructuredActivityNodeAsBlock(node)
    }
}
