package com.abstratt.mdd.target.tests.jee

import com.abstratt.mdd.core.tests.harness.AssertHelper
import com.abstratt.mdd.target.jee.JPQLQueryActionGenerator
import com.abstratt.mdd.target.tests.AbstractGeneratorTest
import java.io.IOException
import org.eclipse.core.runtime.CoreException

class JPQLQueryActionGeneratorTests extends AbstractGeneratorTest {
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
        val root = getStatementSourceAction(op)
        val generated = new JPQLQueryActionGenerator(repository).generateAction(root)
        AssertHelper.assertStringsEqual(
            '''
                SELECT DISTINCT customer_ FROM Customer customer_ WHERE customer_.vip = TRUE
            ''', generated.toString)
    }

    def void testSelectByAttributeInRelatedEntity() throws CoreException, IOException {
        var source = '''
            model crm;
            class Company
                attribute revenue : Double;
            end;
            class Customer
                attribute name : String;
                attribute employer : Company;              
                query findByCompanyRevenue(threshold : Double) : Customer[*];
                begin
                    return Customer extent.select((c : Customer) : Boolean {
                        c.employer.revenue >= threshold
                    });
                end;
            end;
            end.
        '''
        parseAndCheck(source)
        val op = getOperation('crm::Customer::findByCompanyRevenue')
        val root = getStatementSourceAction(op)
        val generated = new JPQLQueryActionGenerator(repository).generateAction(root)
        AssertHelper.assertStringsEqual(
            '''
                SELECT DISTINCT customer_ FROM Customer customer_ WHERE customer_.employer.revenue >= :threshold
            ''', generated.toString)
    }
    
    def void testSelectByRelatedEntity() throws CoreException, IOException {
        var source = '''
            model crm;
            class Company
                attribute revenue : Double;
            end;
            class Customer
                attribute name : String;
                attribute company : Company;              
                query findByCompany(toMatch : Company) : Customer[*];
                begin
                    return Customer extent.select((c : Customer) : Boolean {
                        c.company == toMatch
                    });
                end;
            end;
            end.
        '''
        parseAndCheck(source)
        val op = getOperation('crm::Customer::findByCompany')
        val root = getStatementSourceAction(op)
        val generated = new JPQLQueryActionGenerator(repository).generateAction(root)
        AssertHelper.assertStringsEqual(
            '''
                SELECT DISTINCT customer_ FROM Customer customer_ WHERE customer_.company = :toMatch
            ''', generated.toString)
    }

    
    def void testSelectByDoubleComparison() throws CoreException, IOException {
        var source = '''
            model crm;
            class Customer
                attribute name : String;
                attribute vip : Boolean;
                attribute salary : Double;
                query findHighestGrossing(threshold : Double) : Customer[*];
                begin
                    return Customer extent.select((c : Customer) : Boolean {
                        c.salary >= threshold
                    });
                end;
            end;
            end.
        '''
        parseAndCheck(source)
        val op = getOperation('crm::Customer::findHighestGrossing')
        val root = getStatementSourceAction(op)
        val generated = new JPQLQueryActionGenerator(repository).generateAction(root)
        AssertHelper.assertStringsEqual(
            '''
                SELECT DISTINCT customer_ FROM Customer customer_ WHERE customer_.salary >= :threshold
            ''', generated.toString)
    }
    
		
}