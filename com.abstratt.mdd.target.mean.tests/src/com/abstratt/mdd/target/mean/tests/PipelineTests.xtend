package com.abstratt.mdd.target.mean.tests

import com.abstratt.mdd.target.mean.ApplicationContext
import java.io.IOException
import junit.framework.Test
import junit.framework.TestSuite
import org.eclipse.core.runtime.CoreException
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.CallOperationAction

import static org.eclipse.uml2.uml.UMLPackage.Literals.*

import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import org.eclipse.uml2.uml.ReadStructuralFeatureAction
import com.abstratt.mdd.target.mean.ModelGenerator
import org.eclipse.uml2.uml.AddVariableValueAction

class PipelineTests extends AbstractGeneratorTest {

    def static Test suite() {
        return new TestSuite(PipelineTests)
    }

    new(String name) {
        super(name)
    }

    def testBasic() throws CoreException, IOException {
        var source = '''
        model banking;
          class Account
              attribute balance : Double;
              operation deposit(amount : Double);
              begin
                  self.balance := self.balance + amount;
              end;
              operation withdraw(amount : Double);
              begin
                  self.balance := self.balance - amount;
              end;
              
              operation transfer(destination : Account, amount : Double) : Double;
              begin
                  self.withdraw(amount);
                  destination.deposit(amount);
                  return self.balance;
              end;
          end;
        end.
        '''
        parseAndCheck(source)

        val operation = getOperation('banking::Account::transfer')
        
        val application = new ApplicationContext 
        val activity = operation.methods.head as Activity
        application.newActivityContext(activity)
        application.activityContext.buildPipeline(activity.rootAction)
        val rootStage = application.activityContext.rootStage
        assertNotNull(rootStage)
        assertEquals(3, rootStage.substages.size)
        assertEquals(CALL_OPERATION_ACTION, rootStage.substages.get(0).rootAction.eClass)
        assertEquals("withdraw", (rootStage.substages.get(0).rootAction as CallOperationAction).operation.name)
        assertEquals(CALL_OPERATION_ACTION, rootStage.substages.get(1).rootAction.eClass)
        assertEquals("deposit", (rootStage.substages.get(1).rootAction as CallOperationAction).operation.name)
        assertEquals(ADD_VARIABLE_VALUE_ACTION, rootStage.substages.get(2).rootAction.eClass)
        assertEquals("", (rootStage.substages.get(2).rootAction as AddVariableValueAction).variable.name)
        rootStage.substages.forEach[assertTrue(it.substages.empty)]
    }
    
    def testReadExtent() throws CoreException, IOException {
        var source = '''
        model banking;
          class Account
              attribute balance : Double;
              query allAccounts() : Account[*];
              begin
                  return Account extent;
              end;
          end;
        end.
        '''
        parseAndCheck(source)

        val operation = getOperation('banking::Account::allAccounts')
        
        val application = new ApplicationContext 
        val activity = operation.methods.head as Activity
        application.newActivityContext(activity)
        application.activityContext.buildPipeline(activity.rootAction)
        val rootStage = application.activityContext.rootStage
        assertNotNull(rootStage)
        assertEquals(1, rootStage.substages.size)
        assertEquals(READ_EXTENT_ACTION, rootStage.substages.get(0).rootAction.eClass)
        rootStage.substages.forEach[assertTrue(it.substages.empty)]
    }
    
    def testBatchUpdate() throws CoreException, IOException {
        var source = '''
        model banking;
          class Account
              attribute balance : Double;
              static operation payInterest(rate : Double);
              begin
                  Account extent.forEach((a : Account) {
                      a.balance := a.balance * rate;
                  });
              end;
          end;
        end.
        '''
        parseAndCheck(source)

        val operation = getOperation('banking::Account::payInterest')
        
        val application = new ApplicationContext 
        val activity = operation.methods.head as Activity
        application.newActivityContext(activity)
        application.activityContext.buildPipeline(activity.rootAction)
        val rootStage = application.activityContext.rootStage
        assertNotNull(rootStage)
        assertEquals(STRUCTURED_ACTIVITY_NODE, rootStage.rootAction.eClass)
        assertEquals(1, rootStage.substages.size)
        assertEquals(READ_EXTENT_ACTION, rootStage.substages.get(0).rootAction.eClass)
        rootStage.substages.forEach[assertTrue(it.substages.empty)]
    }
    
    def testDerivedAttribute() throws CoreException, IOException {
        var source = '''
        model banking;
          class Customer attribute name : String; end;
          
          class Account
              attribute number : String;
              reference customer : Customer;
              derived attribute title : String := { self.customer.name + " - " + self.number };
          end;
        end.
        '''
        parseAndCheck(source)

        val attribute = getProperty('banking::Account::title')
        
        val application = new ApplicationContext 
        val activity = attribute.defaultValue.resolveBehaviorReference as Activity
        application.newActivityContext(activity)
        application.activityContext.buildPipeline(activity.rootAction)
        val rootStage = application.activityContext.rootStage
        
        println(rootStage.toString)
        
        println (new ModelGenerator(repository).generateDerivedAttribute(attribute))
        
        assertNotNull(rootStage)
        assertEquals(STRUCTURED_ACTIVITY_NODE, rootStage.rootAction.eClass)
        assertEquals(1, rootStage.substages.size)
        assertEquals(READ_STRUCTURAL_FEATURE_ACTION, rootStage.substages.get(0).rootAction.eClass)
        assertEquals("customer", (rootStage.substages.get(0).rootAction as ReadStructuralFeatureAction).structuralFeature.name)
        assertTrue(rootStage.substages.get(0).substages.empty)
    }
    
    def testDoubleDerivedAttribute() throws CoreException, IOException {
        var source = '''
        model banking;
          class Title attribute name : String; end;

          class Customer 
              attribute name : String;
              reference title : Title;
              derived attribute description : String := { self.title.name + " " + self.name };
          end;
          
          class Account
              attribute number : String;
              reference customer : Customer;
              derived attribute title : String := { self.customer.description + " - " + self.number };
          end;
        end.
        '''
        parseAndCheck(source)

        val attribute = getProperty('banking::Account::title')
        
        val application = new ApplicationContext 
        val activity = attribute.defaultValue.resolveBehaviorReference as Activity
        application.newActivityContext(activity)
        application.activityContext.buildPipeline(activity.rootAction)
        val rootStage = application.activityContext.rootStage
        
        println(rootStage.toString)
        
        println (new ModelGenerator(repository).generateDerivedAttribute(attribute))
        
        assertNotNull(rootStage)
        assertEquals(STRUCTURED_ACTIVITY_NODE, rootStage.rootAction.eClass)
        assertEquals(1, rootStage.substages.size)
        assertEquals(READ_STRUCTURAL_FEATURE_ACTION, rootStage.substages.get(0).rootAction.eClass)
        assertEquals("description", (rootStage.substages.get(0).rootAction as ReadStructuralFeatureAction).structuralFeature.name)
        assertEquals(1, rootStage.substages.head.substages.size)
        
        assertEquals(READ_STRUCTURAL_FEATURE_ACTION, rootStage.substages.head.substages.get(0).rootAction.eClass)
        assertEquals("customer", (rootStage.substages.head.substages.get(0).rootAction as ReadStructuralFeatureAction).structuralFeature.name)
    }
}
