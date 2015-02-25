package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Classifier
import org.eclipse.uml2.uml.Operation
import org.eclipse.uml2.uml.Package
import org.eclipse.uml2.uml.Type

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.TemplateUtils.*
import org.eclipse.uml2.uml.VisibilityKind

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
            import java.util.*;
            import java.util.stream.*;
            import javax.persistence.*;
            import javax.inject.*;
            import javax.ejb.*;
            import javax.enterprise.event.*;
            
            «testedPackages.generateMany[p|
            '''
            import «p.toJavaPackage».*;
            '''
            ]»
            
            public class «testClass.name» {
                «testCases.generateMany[generateTestCase]»
                «helperMethods.generateMany[generateHelperMethod]»
            } 
        '''
    }

    override CharSequence generateBasicTypeOperationCall(Classifier classifier, CallOperationAction action) {
        val operation = action.operation

        if (classifier != null)
            return switch (classifier.qualifiedName) {
                case 'mdd_types::Assert':
                    switch (operation.name) {
                        case 'isNull': '''Assert.assertNull(«generateAction(action.arguments.head)»)'''
                        case 'isNotNull': '''Assert.assertNotNull(«generateAction(action.arguments.head)»)'''
                        case 'isTrue': '''Assert.assertTrue(«generateAction(action.arguments.head)»)'''
                        case 'areEqual': '''Assert.assertEquals(«generateAction(action.arguments.head)», «generateAction(
                            action.arguments.tail.head)»)'''
                        default: '''Unsupported Assert operation: «operation.name»'''
                    }
                default:
                    super.generateBasicTypeOperationCall(classifier, action)
            }
        super.generateBasicTypeOperationCall(classifier, action)
    }

    def CharSequence generateHelperMethod(Operation helperOperation) {
        helperOperation.generateJavaMethod(VisibilityKind.PACKAGE_LITERAL)
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
            import javax.persistence.*;
            import javax.inject.*;
            import javax.ejb.*;
            import javax.enterprise.event.*;
            
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
