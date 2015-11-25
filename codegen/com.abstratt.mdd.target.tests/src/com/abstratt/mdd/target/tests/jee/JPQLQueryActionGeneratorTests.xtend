package com.abstratt.mdd.target.tests.jee

import com.abstratt.mdd.core.tests.harness.AssertHelper
import com.abstratt.mdd.target.jee.ActivityContext
import com.abstratt.mdd.target.jee.JPQLQueryActionGenerator
import com.abstratt.mdd.target.tests.AbstractGeneratorTest
import java.io.IOException
import org.eclipse.core.runtime.CoreException

import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import com.abstratt.mdd.target.base.IBehaviorGenerator.SimpleContext

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
                SELECT NEW crm.CustomerService$CustomerNameCompanyRevenueTuple(customer_.name, customer_.company.revenue) FROM Customer customer_
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
    
    def void testAny() throws CoreException, IOException {
        var source = '''
            model crm;
            class Customer
                attribute name : String;
                attribute vip : Boolean;
                query findAnyVIP() : Customer[0,1];
                begin
                    return Customer extent.\any((c : Customer) : Boolean {
                        c.vip
                    });
                end;
            end;
            end.
        '''
        parseAndCheck(source)
        val op = getOperation('crm::Customer::findAnyVIP')
        val root = getStatementSourceAction(op)
        val generated = new JPQLQueryActionGenerator(repository).generateAction(root)
        AssertHelper.assertStringsEqual(
            '''
                SELECT DISTINCT customer_ FROM Customer customer_ WHERE customer_.vip = TRUE
            ''', generated.toString)
    }
    
    def void testSelf_SubQuery() throws CoreException, IOException {
        var source = '''
            model rental;
            class Car
                attribute name : String;
                attribute driver : Customer[0,1];
            end;
            class Customer
                attribute name : String;
                derived attribute renting : Boolean := {
                	Car extent.exists((c : Car) : Boolean { c.driver == self })
                };
                static query anyRentingCustomer() : Customer[0,1];
                begin
                    return Customer extent.\any((c : Customer) : Boolean { c.renting });
                end; 
            end;
            end.
        '''
        parseAndCheck(source)
        val op = getOperation('rental::Customer::anyRentingCustomer')
        val root = getStatementSourceAction(op)
        val generated = new JPQLQueryActionGenerator(repository).generateAction(root)
        
        AssertHelper.assertStringsEqual(
            '''
                SELECT DISTINCT customer_ FROM Customer customer_ WHERE EXISTS(SELECT car_ FROM Car car_ WHERE car_.driver=customer_)
            ''', generated.toString)
    }    
    
    def void testSelf() throws CoreException, IOException {
        var source = '''
            model rental;
            class Car
                attribute name : String;
                attribute driver : Customer[0,1];
            end;
            class Customer
                attribute name : String;
                query getRentalCar() : Car[0,1];
                begin
                    return Car extent.\any((c : Car) : Boolean { c.driver == self });
                end; 
            end;
            end.
        '''
        parseAndCheck(source)
        val op = getOperation('rental::Customer::getRentalCar')
        val root = getStatementSourceAction(op)
        val generated = new JPQLQueryActionGenerator(repository).generateAction(root)
        
        AssertHelper.assertStringsEqual(
            '''
                SELECT DISTINCT car_ FROM Car car_ WHERE car_.driver = :context
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

    def void testSubQueryExists() throws CoreException, IOException {
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
                attribute vip : Boolean;              
            end;
            end.
        '''
        parseAndCheck(source)
        val op = getOperation('crm::Company::companiesWithVipCustomers')
        val root = getStatementSourceAction(op)
        val generated = new JPQLQueryActionGenerator(repository).generateAction(root)
        AssertHelper.assertStringsEqual(
            '''
                SELECT DISTINCT company_ FROM Company company_ WHERE EXISTS(
                    SELECT customers FROM Customer customers WHERE customers.company = company_ AND customers.vip = TRUE
                )
            ''', generated.toString)
    }
    
    def void testSubQuerySelectIsEmpty() throws CoreException, IOException {
        var source = '''
            model crm;
            class Company
                attribute name : String;
                attribute customers : Customer[*];              
                static query companiesWithVipCustomers() : Company[*];
                begin
                    return Company extent.select((company : Company) : Boolean {
                        company.customers.select((customer : Customer) : Boolean {
                            customer.vip
                        }).isEmpty()
                    });
                end;
            end;
            class Customer
                attribute name : String;
                attribute vip : Boolean;              
            end;
            end.
        '''
        parseAndCheck(source)
        val op = getOperation('crm::Company::companiesWithVipCustomers')
        val root = getStatementSourceAction(op)
        val generated = new JPQLQueryActionGenerator(repository).generateAction(root)
        AssertHelper.assertStringsEqual(
            '''
                SELECT DISTINCT company_ FROM Company company_ WHERE NOT EXISTS(
                    SELECT customers FROM Customer customers WHERE customers.company = company_ AND customers.vip = TRUE
                )
            ''', generated.toString)
    }

    def void testGroupByAttributeIntoSum() throws CoreException, IOException {
        var source = '''
            model crm;
            class Customer
                attribute name : String;
                attribute title : String;
                attribute salary : Double;              
                query sumSalaryByTitle() : {title : String, totalSalary : Double} [*];
                begin
                    return Customer extent.groupBy((c : Customer) : String {
                        c.title
                    }).groupCollect((grouped : Customer[*]) : {title : String, totalSalary : Double} {
                        { 
                            title := grouped.one().title,
                            totalSalary := grouped.sum((c : Customer) : Double {
                                c.salary
                            })
                        }   
                    });
                end;
            end;
            end.
        '''
        parseAndCheck(source)
        val op = getOperation('crm::Customer::sumSalaryByTitle')
        val root = getStatementSourceAction(op)
        val generated = new JPQLQueryActionGenerator(repository).generateAction(root)
        AssertHelper.assertStringsEqual(
            '''
                SELECT NEW crm.CustomerService$TitleTotalSalaryTuple(customer_.title, SUM(customer_.salary))
                    FROM Customer customer_ GROUP BY customer_.title
            ''', generated.toString)
    }
    
    def void testGroupByAttributeIntoCount() throws CoreException, IOException {
        var source = '''
            model crm;
            class Customer
                attribute name : String;
                attribute title : String;              
                query countByTitle() : {title : String, customerCount : Integer} [*];
                begin
                    return Customer extent.groupBy((c : Customer) : String {
                        c.title
                    }).groupCollect((group : Customer[*]) : {title:String, customerCount : Integer} {
                        { 
                            title := group.one().title,
                            customerCount := group.size()
                        }   
                    });
                end;
            end;
            end.
        '''
        parseAndCheck(source)
        val op = getOperation('crm::Customer::countByTitle')
        val root = getStatementSourceAction(op)
        val generated = new JPQLQueryActionGenerator(repository).generateAction(root)
        AssertHelper.assertStringsEqual(
            '''
                SELECT NEW crm.CustomerService$TitleCustomerCountTuple(customer_.title, COUNT(customer_)) 
                    FROM Customer customer_ GROUP BY customer_.title
            ''', generated.toString)
    }
    
    def void testGroupByAttributeIntoCountWithFilter() throws CoreException, IOException {
        var source = '''
            model crm;
            class Customer
                attribute name : String;
                attribute title : String;              
                query countByTitle() : {title : String, customerCount : Integer} [*];
                begin
                    return Customer extent.groupBy((c : Customer) : String {
                        c.title
                    }).groupCollect((group : Customer[*]) : {title:String, customerCount : Integer} {
                        { 
                            title := group.one().title,
                            customerCount := group.size()
                        }   
                    }).select((counted : {title:String, customerCount : Integer}) : Boolean {
                        counted.customerCount > 100
                    });
                end;
            end;
            end.
        '''
        parseAndCheck(source)
        val op = getOperation('crm::Customer::countByTitle')
        val root = getStatementSourceAction(op)
        val generated = new JPQLQueryActionGenerator(repository).generateAction(root)
        AssertHelper.assertStringsEqual(
            '''
                SELECT NEW crm.CustomerService$TitleCustomerCountTuple(customer_.title, COUNT(customer_))
                    FROM Customer customer_ GROUP BY customer_.title HAVING COUNT(customer_) > 100 
            ''', generated.toString)
    }
    
}