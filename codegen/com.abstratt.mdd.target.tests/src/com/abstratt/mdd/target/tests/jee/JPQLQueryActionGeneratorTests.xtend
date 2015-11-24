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
    
    def void testExtent() throws CoreException, IOException {
        var source = '''
            model crm;
            class Customer
                attribute name : String;  
                query findAll() : Customer[*];
                begin
                    return Customer extent;
                end;
            end;
            end.
        '''
        parseAndCheck(source)
        val op = getOperation('crm::Customer::findAll')

        val root = getStatementSourceAction(op)
        val generated = new JPQLQueryActionGenerator(repository).generateAction(root)
        AssertHelper.assertStringsEqual(
            '''
                SELECT DISTINCT customer_ FROM Customer customer_
            ''', generated.toString)
    }
    
    def void testCount_InlinedCondition() throws CoreException, IOException {
        var source = '''
        model car_rental;
            class Rental
                attribute returnDate : Date[0,1];  
                derived readonly attribute inProgress : Boolean := {
                    self.returnDate == null
                };
                static query countRentalsInProgress() : Integer;
                begin
                    return Rental extent.select((l : Rental) : Boolean {
                        l.inProgress
                    }).size();
                end;
            end;
        end.
        '''
        parseAndCheck(source)
        val op = getOperation('car_rental::Rental::countRentalsInProgress')
        val root = getStatementSourceAction(op)
        val generated = new JPQLQueryActionGenerator(repository).generateAction(root)
        AssertHelper.assertStringsEqual(
            '''
                SELECT COUNT(rental_) FROM Rental rental_ WHERE rental_.returnDate IS NULL
            ''', generated.toString)
    }
    
    def void testCount() throws CoreException, IOException {
        var source = '''
            model car_rental;
            class Rental
                attribute returnDate : Date[0,1];  
                static query countRentalsInProgress() : Integer;
                begin
                    return Rental extent.select((l : Rental) : Boolean {
                        l.returnDate == null
                    }).size();
                end;
            end;
            end.
        '''
        parseAndCheck(source)
        val op = getOperation('car_rental::Rental::countRentalsInProgress')
        val root = getStatementSourceAction(op)
        val generated = new JPQLQueryActionGenerator(repository).generateAction(root)
        AssertHelper.assertStringsEqual(
            '''
            SELECT COUNT(rental_) FROM Rental rental_ WHERE rental_.returnDate IS NULL
            ''', generated.toString)
    }
    
    
    def void testMax() throws CoreException, IOException {
        var source = '''
        model crm;
            class Company
                attribute revenue : Double;
                query highestRevenue() : Double;
                begin
                    return Company extent.max((c : Company) : Double {
                        c.revenue
                    });
                end;
                
            end;
        end.
        '''
        parseAndCheck(source)
        val op = getOperation('crm::Company::highestRevenue')
        val root = getStatementSourceAction(op)
        val generated = new JPQLQueryActionGenerator(repository).generateAction(root)
        AssertHelper.assertStringsEqual(
            '''
                SELECT MAX(company_.revenue) FROM Company company_
            ''', generated.toString)
    }

    def void testCollectAttributes() throws CoreException, IOException {
        var source = '''
            model crm;
            class Company
                attribute revenue : Double;
            end;            
            class Customer
                attribute name : String;
                attribute company : Company;                              
                static query getCompanyRevenueWithCustomerName() : { customerName : String, companyRevenue : Double}[*];
                begin
                    return Customer extent.collect((c : Customer) : { : String, : Double} {
                        { cName := c.name, cRevenue := c.company.revenue }
                    });
                end;
            end;
            end.
        '''
        parseAndCheck(source)
        val op = getOperation('crm::Customer::getCompanyRevenueWithCustomerName')
        val root = getStatementSourceAction(op)
        val generated = new JPQLQueryActionGenerator(repository).generateAction(root)
        AssertHelper.assertStringsEqual(
            '''
                SELECT customer_.name AS cName, customer_.company.revenue AS cRevenue FROM Customer customer_
            ''', generated.toString)
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
    
    def void testSelectByRelatedIsEmpty() throws CoreException, IOException {
        var source = '''
            model crm;
            class Company
                attribute name : String;
                attribute customers : Customer[*];              
                static query companiesWithoutCustomers() : Company[*];
                begin
                    return Company extent.select((company : Company) : Boolean {
                        company.customers.isEmpty()
                    });
                end;
            end;
            class Customer
                attribute name : String;
            end;
            end.
        '''
        parseAndCheck(source)
        val op = getOperation('crm::Company::companiesWithoutCustomers')
        val root = getStatementSourceAction(op)
        val generated = new JPQLQueryActionGenerator(repository).generateAction(root)
        AssertHelper.assertStringsEqual(
            '''
                SELECT DISTINCT company_ FROM Company company_ WHERE company_.customers IS EMPTY
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
    
    def void testExists() throws CoreException, IOException {
        var source = '''
            model crm;
            class Customer
                attribute name : String;
                attribute vip : Boolean;
                static query anyVipCustomers() : Boolean;
                begin
                    return Customer extent.exists((customer : Customer) : Boolean {
                        customer.vip
                    });
                end;
            end;
            end.
        '''
        parseAndCheck(source)
        val op = getOperation('crm::Customer::anyVipCustomers')
        val root = getStatementSourceAction(op)
        val generated = new JPQLQueryActionGenerator(repository).generateAction(root)
        AssertHelper.assertStringsEqual(
            '''
				SELECT CASE WHEN COUNT(customer_) > 0 THEN TRUE ELSE FALSE END FROM Customer customer_ WHERE customer_.vip = TRUE
            ''', generated.toString)
    }
    
    def void testIsEmpty() throws CoreException, IOException {
        var source = '''
            model crm;
            class Customer
                attribute name : String;
                attribute vip : Boolean;
                static query anyVipCustomers() : Boolean;
                begin
                    return Customer extent.select((customer : Customer) : Boolean {
                        customer.vip
                    }).isEmpty();
                end;
            end;
            end.
        '''
        parseAndCheck(source)
        val op = getOperation('crm::Customer::anyVipCustomers')
        val root = getStatementSourceAction(op)
        val generated = new JPQLQueryActionGenerator(repository).generateAction(root)
        AssertHelper.assertStringsEqual(
            '''
				SELECT COUNT(customer_) = 0 FROM Customer customer_ WHERE customer_.vip = TRUE
            ''', generated.toString)
    }
    
}