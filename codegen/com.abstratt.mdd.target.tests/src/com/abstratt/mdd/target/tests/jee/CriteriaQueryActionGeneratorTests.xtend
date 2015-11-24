package com.abstratt.mdd.target.tests.jee

import com.abstratt.mdd.core.tests.harness.AssertHelper
import com.abstratt.mdd.target.tests.AbstractGeneratorTest
import java.io.IOException
import org.eclipse.core.runtime.CoreException
import com.abstratt.mdd.target.jee.CriteriaQueryActionGenerator

class CriteriaQueryActionGeneratorTests extends AbstractGeneratorTest {
    new(String name) {
        super(name)
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
        val generated = new CriteriaQueryActionGenerator(repository).generateAction(root)
        // we want to issue the exists function the same way for whichever case we are handling here
        // so we will always add a criteria for relating the child object to the parent object
        // (I don't really know which cases I am talking about here)  
        AssertHelper.assertStringsEqual(
            '''
                cq.distinct(true).where(
                	cb.exists(
                        customerSubquery
                            .select(customers)
                            .where(
                                cb.equal(customers.get("company"), company_), 
                                cb.isTrue(customers.get("vip"))
                            )
                    )
                )
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
        val generated = new CriteriaQueryActionGenerator(repository).generateAction(root)
        AssertHelper.assertStringsEqual(
            '''
                cq.distinct(true).where(
                	cb.isEmpty(company_.get("customers"))
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
        val generated = new CriteriaQueryActionGenerator(repository).generateAction(root)
        // we want to issue the exists function the same way for whichever case we are handling here
        // so we will always add a criteria for relating the child object to the parent object
        // (even if we could have navigated from the parent to the child)  
        AssertHelper.assertStringsEqual(
            '''
                cq.distinct(true).where(
                	cb.exists(customerSubquery
                        .select(customers)
                        .where(
                            cb.equal(customers.get("company"), company_), 
                            cb.isTrue(customers.get("vip"))
                        )
                ).not())
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
                    return not Customer extent.select((customer : Customer) : Boolean {
                        customer.vip
                    }).isEmpty();
                end;
            end;
            end.
        '''
        parseAndCheck(source)
        val op = getOperation('crm::Customer::anyVipCustomers')
        val root = getStatementSourceAction(op)
        val generated = new CriteriaQueryActionGenerator(repository).generateAction(root)
        AssertHelper.assertStringsEqual(
            '''
                !(cq.distinct(true)
                    .where(
                        cb.isTrue(customer_.get("vip"))
                    )
					.select(cb.count(customer_)) == 0)
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
        val generated = new CriteriaQueryActionGenerator(repository).generateAction(root)
        AssertHelper.assertStringsEqual(
            '''
				cq.distinct(true)
					.where(
						cb.isTrue(customer_.get("vip"))
					).multiselect(
						cb.selectCase()
							.when(cb.gt(cb.count(customer_), cb.literal(0)), true)
							.otherwise(false)
					)
            ''', generated.toString)
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
        val generated = new CriteriaQueryActionGenerator(repository).generateAction(root)
        AssertHelper.assertStringsEqual(
            '''
                cq.distinct(true)
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
        val generated = new CriteriaQueryActionGenerator(repository).generateAction(root)
        AssertHelper.assertStringsEqual(
            '''
                cq.distinct(true)
                    .where(cb.isTrue(customer_.get("vip")))
            ''', generated.toString)
    }
    
    def void testCollectAttributes() throws CoreException, IOException {
    	// What is the difference between this test and testCollectTuples? 
        var source = '''
            model crm;
            class Company
                attribute revenue : Double;
            end;            
            class Customer
                attribute name : String;
                attribute company : Company;                              
                query getCompanyRevenueWithCustomerName() : { customerName : String, companyRevenue : Double}[*];
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
        val generated = new CriteriaQueryActionGenerator(repository).generateAction(root)
        AssertHelper.assertStringsEqual(
            '''
                cq
                    .distinct(true)
                    .multiselect(customer_.get("name"), customer_.get("company").get("revenue"))
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
        val generated = new CriteriaQueryActionGenerator(repository).generateAction(root)
        AssertHelper.assertStringsEqual(
            '''
	            cq
	            	.distinct(true)
	            	.select(cb.max(company_.get("revenue")))
            ''', generated.toString)
    }
    
    
    def void testCount() throws CoreException, IOException {
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
        val generated = new CriteriaQueryActionGenerator(repository).generateAction(root)
        AssertHelper.assertStringsEqual(
            '''
	            cq.distinct(true).where(
	                cb.equal(rental_.get("returnDate"), cb.nullLiteral(null))
	            )
	            .select(cb.count(rental_))
            ''', generated.toString)
    }
    
    def void testCount_InlinedCondition() throws CoreException, IOException {
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
        val generated = new CriteriaQueryActionGenerator(repository).generateAction(root)
        AssertHelper.assertStringsEqual(
            '''
	            cq.distinct(true).where(
	                cb.equal(rental_.get("returnDate"), cb.nullLiteral(null))
	            )
	            .select(cb.count(rental_))
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
        val generated = new CriteriaQueryActionGenerator(repository).generateAction(root)
        AssertHelper.assertStringsEqual(
            '''
                cq
                    .distinct(true)
                    .where(cb.greaterThanOrEqualTo(
                        employer.get("revenue"),
                        cb.parameter(Double.class,"threshold")
                    ))
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
        val generated = new CriteriaQueryActionGenerator(repository).generateAction(root)
        AssertHelper.assertStringsEqual(
            '''
                cq.distinct(true)
                    .where(cb.equal(
                        customer_.get("company"),
                        cb.parameter(Company.class,"toMatch")
                    ))
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
        val generated = new CriteriaQueryActionGenerator(repository).generateAction(root)
        AssertHelper.assertStringsEqual(
            '''
                cq
                    .distinct(true)
                    .where(cb.greaterThanOrEqualTo(
                        customer_.get("salary"),
                        cb.parameter(Double.class,"threshold")
                    ))
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
        val generated = new CriteriaQueryActionGenerator(repository).generateAction(root)
        AssertHelper.assertStringsEqual(
            '''
                cq
                    .groupBy(customer_.get("title"))
                    .multiselect(customer_.get("title"), cb.count(customer_))
                    .having(cb.greaterThan(cb.count(customer_), cb.literal(100L)))
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
        val generated = new CriteriaQueryActionGenerator(repository).generateAction(root)
        AssertHelper.assertStringsEqual(
            '''
                cq
                    .groupBy(customer_.get("title"))
                    .multiselect(customer_.get("title"), cb.count(customer_))
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
        val generated = new CriteriaQueryActionGenerator(repository).generateAction(root)
        AssertHelper.assertStringsEqual(
            '''
                cq
                    .groupBy(customer_.get("title"))
                    .multiselect(customer_.get("title"), cb.sum(customer_.get("salary")))
            ''', generated.toString)
    }
    
}
