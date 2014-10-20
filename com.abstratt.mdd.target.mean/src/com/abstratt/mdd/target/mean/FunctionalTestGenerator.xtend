package com.abstratt.mdd.target.mean

import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.Action
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
import org.eclipse.uml2.uml.CreateObjectAction

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
        require('./CRUD.js');
        «testRelatedClasses.map['''require('./«it.name».js');'''].join('\n')»
        '''
    }
    
    def CharSequence generateSuiteHelper(Class helperClass) {
        '''
        var mongoose = require('mongoose');
        require('../models');
        
        var «helperClass.name» = {
            «helperClass.operations.map[ op |
                val method = op.methods.get(0) as Activity
                '''«op.name» : function(«op.ownedParameters.inputParameters.map[name].join(', ')») «method.generateActivity»'''
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
        
        var mongoose = require('mongoose');
        var HttpClient = require("../http-client.js");
        var helpers = require('../helpers.js');
        var util = require('util');
        var q = require('q');

        var assert = require("assert");
        var folder = process.env.KIRRA_FOLDER || 'cloudfier-examples';

        var kirraBaseUrl = process.env.KIRRA_BASE_URL || "http://localhost:48084";
        var kirraApiUrl = process.env.KIRRA_API_URL || (kirraBaseUrl);
        var httpClient = new HttpClient(kirraApiUrl);
        «helperClasses.map['''var «it.name» = require('./«it.name».js');'''].join('\n')»
        
        suite('«applicationName» functional tests - «testClass.name»', function() {
            this.timeout(10000);

            «helpers.map[generateTestCaseHelper].join()»
            
            «testCases.map[generateTestCase].join()»
        });
        
        '''
    }
    
    override def generateClassReference(Classifier classifier) {
        '''require('«IF classifier.entity»../models«ELSE».«ENDIF»/«classifier.name».js')'''
    }
    
    def CharSequence generateTestCaseHelper(Operation op) {
        '''
            var «op.name» = function(«op.ownedParameters.inputParameters.map[name].join(', ')») {
                
            };
        '''
    }
    
    def CharSequence generateTestCase(Operation testCase) {
        val testBehavior = testCase.methods.get(0) as Activity
        /*
         * We need to break the behavior into a sequence of blocks that run in order but asynchronously.
         * 
         */
        '''
        test('«testCase.name»', function(done) {
            «testBehavior.rootAction.nodes.filter[(it as Action).terminal].map[
                '''
                // a block
                «generateAction(it)»
                '''
            ].join»
            done();
        });
        '''
    }
    
    override CharSequence generateBasicTypeOperationCall(Classifier classifier, CallOperationAction action) {
        val operation = action.operation
        
        if (classifier != null)
            return switch (classifier.qualifiedName) {
                case 'mdd_types::Assert' : switch (operation.name) {
                    case 'isNull' : '''assert.ok(«generateAction(action.arguments.head)» == null, '«generateAction(action.arguments.head)»')'''
                    case 'isNotNull' : '''assert.ok(«generateAction(action.arguments.head)» != null, '«generateAction(action.arguments.head)»: ' + «generateAction(action.arguments.head)»)'''
                    case 'isTrue' : '''assert.ok(«generateAction(action.arguments.head)» === true, '«generateAction(action.arguments.head)»: ' + «generateAction(action.arguments.head)»)'''
                    case 'areEqual' : '''assert.equal(«generateAction(action.arguments.head)», «generateAction(action.arguments.tail.head)», '«generateAction(action.arguments.head)» == «generateAction(action.arguments.tail.head)»')'''
                    default : '''Unsupported Assert operation: «operation.name»'''
                }
                default: super.generateBasicTypeOperationCall(classifier, action)
            }
        super.generateBasicTypeOperationCall(classifier, action)         
    }
    
    
    def generateRootSuite(String applicationName, Iterable<Class> testClasses) {
        '''
        «testClasses.map[
            '''require('./«name».js');'''
        ].join('\n')»
        '''
    }
    
    
}
