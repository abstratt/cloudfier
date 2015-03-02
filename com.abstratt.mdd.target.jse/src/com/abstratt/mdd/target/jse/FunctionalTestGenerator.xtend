package com.abstratt.mdd.target.jse

import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.Action
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Classifier
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.Package
import org.eclipse.uml2.uml.Type
import org.eclipse.uml2.uml.VisibilityKind

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.FeatureUtils.*
import static extension com.abstratt.mdd.core.util.TemplateUtils.*

class FunctionalTestGenerator extends EntityGenerator {

    protected Iterable<Package> testedPackages

    protected Iterable<Class> testRelatedClasses
    protected Iterable<Class> testClasses

    new(IRepository repository) {
        super(repository)
        val allPackages = repository.getTopLevelPackages(null)
        val appPackages = allPackages.applicationPackages
        this.testedPackages = appPackages.filter [
            it.ownedTypes.exists [
                it.entity
            ]
        ]
        val testPackages = allPackages.filter [
            it.ownedTypes.exists [
                it.testClass
            ]
        ]
        val relevantClasses = testPackages.map[
            ownedTypes
                .filter(typeof(Class))
                .filter[!templateInstance]
        ].flatten
        this.testClasses = relevantClasses.filter[testClass]
        this.testRelatedClasses = relevantClasses.filter[!testClass]
    }

    def boolean isTestClass(Type toCheck) {
        toCheck.hasStereotype('Test')
    }

    private def boolean isTestCase(Operation toCheck) {
        toCheck.public && toCheck.ownedParameters.empty && toCheck.class_.isTestClass()
    }

    def CharSequence generateTestClass(Class testClass) {
        val testCases = testClass.operations.filter[isTestCase]
        val helperMethods = testClass.operations.filter[!isTestCase]
        '''
            package «testClass.packagePrefix».test;
            
            import org.junit.*;  
            import static org.junit.Assert.*;  
            import java.util.*;
            import java.util.stream.*;
            import java.util.function.*;
            
            «testedPackages.generateMany[p|
            '''
            import «p.toJavaPackage».*;
            '''
            ]»
            
            public class «testClass.name» {
                «testCases.generateMany[generateTestCase]»
                «helperMethods.generateMany[generateHelperMethod]»
                «generateTearDown»
            } 
        '''
    }
    
    override generateProviderReference(Class context, Class provider) {
        '''new «provider.toJavaType»Service()'''
    }
    
    def generateTearDown() {
        '''
        @After
        public void tearDown() {
            «entities.generateMany['''«name».zap();''']»
        }
        '''
    }

    override CharSequence generateBasicTypeOperationCall(CallOperationAction action) {
        val operation = action.operation
        val classifier = action.operationTarget 
        if (classifier != null)
            return switch (classifier.qualifiedName) {
                case 'mdd_types::Assert':
                    switch (operation.name) {
                        case 'isNull': '''assertNull(«generateAction(action.arguments.head)»)'''
                        case 'isNotNull': '''assertNotNull(«generateAction(action.arguments.head)»)'''
                        case 'isTrue': '''assertTrue(«generateAction(action.arguments.head)»)'''
                        case 'areEqual':
                            '''assertEquals(«generateAction(action.arguments.head)», «generateAction(action.arguments.last)»)'''
                        default: '''Unsupported Assert operation: «operation.name»'''
                    }
                default:
                    super.generateBasicTypeOperationCall(action)
            }
        super.generateBasicTypeOperationCall(action)
    }
    
    def boolean isAssertion(Action action) {
        if (!(action instanceof CallOperationAction))
            return false
        val asCall = action as CallOperationAction    
        return "mdd_types::Assert" == asCall.operation.owningClassifier.qualifiedName
    }
    
    override generateStatement(Action statementAction) {
        if (statementAction.assertion) {
            val siblings = statementAction.owningBlock.findTerminals
            val index = siblings.indexOf(statementAction)
            val firstInBlock = index > 0 && !siblings.get(index-1).assertion || index == 0
            val lastInBlock = index < siblings.size - 1 && !siblings.get(index+1).assertion || index == siblings.size - 1
            val prefix = if (firstInBlock) '\n' else ''
            val suffix = if (lastInBlock) '\n' else ''
            '''«prefix»«super.generateStatement(statementAction)»«suffix»'''
        } else
            super.generateStatement(statementAction)
    }
    
    def CharSequence generateHelperMethod(Operation helperOperation) {
        helperOperation.generateJavaMethod(VisibilityKind.PACKAGE_LITERAL, helperOperation.static)
    }

    def CharSequence generateTestCase(Operation testCase) {
        val testBehavior = testCase.activity
        '''
            @Test
            public void «testCase.name»() {
                «testBehavior.generateActivity»
            }
        '''
    }

    def render(CharSequence cs, String quote) {
        '''«quote»«cs.toString.replaceAll(quote, '\\\'' + quote).replaceAll('\n', '\\\'')»«quote»'''
    }

    def generateRootSuite(String applicationName, Iterable<Class> testClasses) {
        '''
            «testClasses.map [
                '''require('./«name».js');'''
            ].join('\n')»
        '''
    }

    def generateTestHelperClass(Class testHelperClass) {
        val operations = testHelperClass.operations
        '''
            package «testHelperClass.packagePrefix».test;
            
            import org.junit.*;  
            import java.util.*;
            import java.util.stream.*;
            import java.util.function.*;
            
            «testedPackages.generateMany[p|
            '''
            import «p.toJavaPackage».*;
            '''
            ]»
            
            public class «testHelperClass.name» {
                «operations.generateMany[generateHelperMethod]»
            } 
        '''
    }

}
