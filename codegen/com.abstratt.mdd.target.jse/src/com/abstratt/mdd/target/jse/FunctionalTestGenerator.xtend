package com.abstratt.mdd.target.jse

import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.Action
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.Package
import org.eclipse.uml2.uml.Type
import org.eclipse.uml2.uml.VisibilityKind

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.FeatureUtils.*
import static extension com.abstratt.mdd.core.util.TemplateUtils.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import com.abstratt.mdd.core.util.StereotypeUtils

class FunctionalTestGenerator extends PlainEntityGenerator {

    new(IRepository repository) {
        super(repository)
    }

    private def boolean isTestCase(Operation toCheck) {
        toCheck.public && toCheck.ownedParameters.empty && toCheck.class_.isTestClass()
    }
    
    override IBehaviorGenerator createBehaviorGenerator() {
        new FunctionalTestBehaviorGenerator(repository)
    }

    def CharSequence generateTestClass(Class testClass) {
        val testedPackages = appPackages.filter [
            it.ownedTypes.exists  [
                it.entity
            ]
        ] 
        
        val testCases = testClass.operations.filter[isTestCase]
        val helperMethods = testClass.operations.filter[!isTestCase]
        '''
            package «testClass.packagePrefix».test;
            
            «generateStandardImports»
            
            «testedPackages.generateMany[p|
            '''
            import «p.toJavaPackage».*;
            '''
            ]»
            
            public class «testClass.name» {
                «generateTestClassPrefix»
                «testCases.generateMany[generateTestCase]»
                «helperMethods.generateMany[generateHelperMethod]»
            } 
        '''
    }
    
    override generateStandardImports() {
        '''
            «super.generateStandardImports»
            // anonymous:
            «this.entities.generateMany[ entity |
                entity.findAnonymousDataTypes.generateMany[ dataType |
                '''import «entity.package.toJavaPackage».«entity.name»Service.«dataType.generateAnonymousDataTypeName»;'''
                ]
            ]»
            
            import org.junit.*;  
            import static org.junit.Assert.*;  
        '''
    }
    
    def generateHelperMethod(Operation helperOperation) {
        helperOperation.generateJavaMethod(VisibilityKind.PACKAGE_LITERAL, helperOperation.static)
    }
    
    def generateTestClassPrefix() {
        '''
        @After
        public void tearDown() {
            «entities.generateMany['''«name».zap();''']»
        }
        '''
    }
    
    def CharSequence generateTestCase(Operation testCase) {
        val testBehavior = testCase.activity
        val expectedFailureConstraint = StereotypeUtils.getValue(testCase, 'Failure', "constraint")
        val expectedFailuresEnabled = false 
        
        '''
            @Test«IF expectedFailuresEnabled && expectedFailureConstraint != null»(expected=«expectedFailureConstraint»Exception.class)«ENDIF»
            public void «testCase.name»() {
                «testBehavior.generateActivity»
            }
        '''
    }

    def generateRootSuite(String applicationName, Iterable<Class> testClasses) {
        '''
            «testClasses.map [
                '''require('./«name».js');'''
            ].join('\n')»
        '''
    }
}
