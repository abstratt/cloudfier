package com.abstratt.mdd.target.tests.jee

import com.abstratt.mdd.target.jee.DataFlowAnalyzer
import com.abstratt.mdd.target.tests.AbstractGeneratorTest
import java.io.IOException
import org.eclipse.core.runtime.CoreException
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.ReadExtentAction
import org.eclipse.uml2.uml.ReadStructuralFeatureAction
import org.eclipse.uml2.uml.ReadVariableAction
import org.eclipse.uml2.uml.ValueSpecificationAction

import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import org.eclipse.uml2.uml.AddVariableValueAction
import org.eclipse.uml2.uml.StructuredActivityNode
import org.eclipse.uml2.uml.ReadLinkAction
import org.eclipse.uml2.uml.Property

class DataFlowAnalyzerTests  extends AbstractGeneratorTest {
    new(String name) {
        super(name)
    }
	def void testSelectByBooleanValue() throws CoreException, IOException {
        var source = '''
            model crm;
            class Customer
                attribute name : String;
                attribute vip : Boolean;              
                static query findVip() : Customer[*];
                begin
                    return Customer extent.select((c : Customer) : Boolean {
                        c.vip
                    });
                end;
            end;
            end.
        '''
        parseAndCheck(source)
        val op = getOperation('crm::Customer::findVip')
        val opBody = op.rootAction as StructuredActivityNode
        val opReturn = opBody.findSingleStatement as AddVariableValueAction
        val select = opReturn.value.sourceAction as CallOperationAction
        val readExtent = select.target.sourceAction as ReadExtentAction
        val closureValueSpec = select.arguments.head.sourceAction as ValueSpecificationAction
        val closure = closureValueSpec.value.resolveBehaviorReference as Activity
        val closureBody = closure.rootAction as StructuredActivityNode
        val closureReturn = closureBody.findSingleStatement as AddVariableValueAction
		val readVipAttribute = closureReturn.sourceAction as ReadStructuralFeatureAction
        val readLocalVarC = readVipAttribute.object.sourceAction as ReadVariableAction
        val dataFlowAnalyzer = new DataFlowAnalyzer()
        
        assertNotNull(readExtent.result)
        assertSame(readExtent.result, dataFlowAnalyzer.findSource(opReturn.value))
        assertSame(readExtent.result, dataFlowAnalyzer.findSource(readVipAttribute.object))
        assertSame(readVipAttribute.result, dataFlowAnalyzer.findSource(closureReturn.value))
    }
    
	def void testExists() throws CoreException, IOException {
        var source = '''
            model crm;
            class Company
                attribute name : String;
                attribute customers : Customer[*];              
                static query companiesWithVipCustomers() : Company[*];
                begin
                    return Company extent.select((company : Company) : Boolean {
                        company.customers.exists((customer : Customer) : Boolean {
                            customer.vip
                        })
                    });
                end;
            end;
            class Customer
                attribute name : String;
                attribute income : Double;
			    derived readonly attribute vip : Boolean := {
			        self.income > 10000
			    };
            end;
            end.
        '''
        parseAndCheck(source)
        val op = getOperation('crm::Company::companiesWithVipCustomers')
        val opBody = op.rootAction as StructuredActivityNode
        val opReturn = opBody.findSingleStatement as AddVariableValueAction
        val select = opReturn.value.sourceAction as CallOperationAction
        val readExtent = select.target.sourceAction as ReadExtentAction
        val closureValueSpec1 = select.arguments.head.sourceAction as ValueSpecificationAction
        val closure1 = closureValueSpec1.value.resolveBehaviorReference as Activity
        val closureBody1 = closure1.rootAction as StructuredActivityNode
        val closureReturn1 = closureBody1.findSingleStatement as AddVariableValueAction

        val exists = closureReturn1.value.sourceAction as CallOperationAction
        val readLinkCustomers = exists.target.sourceAction as ReadStructuralFeatureAction
        val readVarCompany = readLinkCustomers.sourceAction as ReadVariableAction
        val closureValueSpec2 = exists.arguments.head.sourceAction as ValueSpecificationAction
        val closure2 = closureValueSpec2.value.resolveBehaviorReference as Activity
        val closureBody2 = closure2.rootAction as StructuredActivityNode
        val closureReturn2 = closureBody2.findSingleStatement as AddVariableValueAction
        
		val readVipAttribute = closureReturn2.sourceAction as ReadStructuralFeatureAction
        val readVarCustomer = readVipAttribute.object.sourceAction as ReadVariableAction
        
        val vipAttribute = readVipAttribute.structuralFeature as Property
        val vipDerivation = vipAttribute.derivation
        
        val dataFlowAnalyzer = new DataFlowAnalyzer()
        
        assertNotNull(readExtent.result)
        assertSame(readLinkCustomers.result, dataFlowAnalyzer.findSource(readVipAttribute.object))
        assertSame(readLinkCustomers.result, dataFlowAnalyzer.findSource(exists.target))
        assertSame(readExtent.result, dataFlowAnalyzer.findSource(readLinkCustomers.object))
    }
}