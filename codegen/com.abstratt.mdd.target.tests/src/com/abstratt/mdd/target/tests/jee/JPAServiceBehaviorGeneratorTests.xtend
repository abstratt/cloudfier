package com.abstratt.mdd.target.tests.jee

import com.abstratt.mdd.core.tests.harness.AssertHelper
import com.abstratt.mdd.target.tests.AbstractGeneratorTest
import com.abstratt.mdd.target.jee.JPAServiceBehaviorGenerator
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
        AssertHelper.assertStringsEqual(
            '''
	        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
	        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
	        Root<Rental> rental_ = cq.from(Rental.class);
	        return getEntityManager().createQuery(
	            cq.distinct(true).where(
	                cb.equal(rental_.get("returnDate"), cb.nullLiteral(null))
	            )
	            .select(cb.count(rental_))
	        ).getResultList().stream().findAny().orElse(null);
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