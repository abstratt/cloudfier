package com.abstratt.mdd.target.tests.jee

import com.abstratt.mdd.core.tests.harness.AssertHelper
import com.abstratt.mdd.target.jee.JPAServiceBehaviorGenerator
import com.abstratt.mdd.target.jee.QueryActionGenerator
import com.abstratt.mdd.target.tests.AbstractGeneratorTest
import java.util.LinkedHashMap

class JPAServiceBehaviorGeneratorTests extends AbstractGeneratorTest {

	new(String name) {
		super(name)
	}

	def testCount() {
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
		val activity = getActivity('car_rental::Rental::countRentalsInProgress')
		val generated = new JPAServiceBehaviorGenerator(repository).generateJavaMethodBody(activity)
		AssertHelper.assertStringStartsAndEndsWith(
            '''
			CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
			CriteriaQuery<Long> cq = cb.createQuery(Long.class);
			Root<Rental> rental_ = cq.from(Rental.class);
			return getEntityManager().createQuery(
			''',
			'''
			).getResultList().stream().findAny().orElse(null);
		    ''', generated.toString)
	}

	def void testCollectTuples() {
		var source = '''
			model crm;
			class Customer
			    attribute name : String;
			    attribute vip : Boolean;              
			    static query customerDetails() : {customerName : String, isVip : Boolean}[*];
			    begin
			        return Customer extent.collect((c : Customer) : {customerName : String, isVip : Boolean} {
			        	{
			        	    customerName := c.name,
			        	    isVip := c.vip
			        	}
			        });
			    end;
			end;
			end.
		'''
		parseAndCheck(source)
		val activity = getActivity('crm::Customer::customerDetails')
		val generated = new JPAServiceBehaviorGenerator(repository).generateJavaMethodBody(activity)
		AssertHelper.assertStringStartsAndEndsWith(
            '''
			CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
			CriteriaQuery<CustomerNameIsVipTuple> cq = cb.createQuery(CustomerNameIsVipTuple.class);
			Root<Customer> customer_ = cq.from(Customer.class);
			return getEntityManager().createQuery(
		''', 
        '''
			).getResultList();
		''', generated.toString)
	}

	def testCollectEntityActions_Count() {
		var source = '''
			  model mypackage;
			   class MyEntity
			       attribute attr1 : String;  
			       static query countInstances() : Integer;
			       begin
			           return MyEntity extent.size();
			end;
			     end;
			  end.
		 '''
		parseAndCheck(source)
		val myEntity = getClass("mypackage::MyEntity")
		val activity = getActivity('mypackage::MyEntity::countInstances')
		val collected = new LinkedHashMap
		new JPAServiceBehaviorGenerator(repository).collectEntityActions(collected, activity)
		assertEquals(1, collected.size())
		val entitiesUsed = collected.get(null)
		assertNotNull(entitiesUsed)
		assertEquals(1, entitiesUsed.size())
		assertSame(myEntity, entitiesUsed.head)
	}
}