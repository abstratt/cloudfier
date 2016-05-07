package com.abstratt.mdd.target.tests.jee

import com.abstratt.mdd.core.tests.harness.AssertHelper
import com.abstratt.mdd.target.jee.JPAEntityGenerator
import com.abstratt.mdd.target.tests.AbstractGeneratorTest
import org.junit.Test

class JPAEntityGeneratorTests extends AbstractGeneratorTest {

	new(String name) {
		super(name)
	}
	
	def void buildModel() {
		val source = '''
		package pack1;
		
		class Class1
		    attribute attr1 : String;
		    attribute attr2 : StateMachine1;
		    operation op1();
		    private operation op2();
		    statemachine StateMachine1
		
		        initial state State0
		            transition on call(op1) to State1;
		            transition on call(op2) to State2;
		        end;
		
		        state State1
		        end;
		
		        state State2
		        end;		
		    end;
		end;
		
		end.
		'''
		parseAndCheck(source)
	}
	@Test
	def void testEventGeneration_publicOperation() {
		buildModel()
		val op1 = getOperation('pack1::Class1::op1')
		
		val generated = new JPAEntityGenerator(repository).generateOperation(op1)
		AssertHelper.assertStringsEqual(
        '''
        public void op1() {
        	this.handleEvent(StateMachine1Event.Op1);
        }
		''', generated.toString)
	}


	@Test
	def void testEventGeneration_privateOperation() {
		buildModel()
		val op2 = getOperation('pack1::Class1::op2')
		
		val generated = new JPAEntityGenerator(repository).generateOperation(op2)
		AssertHelper.assertStringsEqual(
        '''
        private void op2() {
        	this.handleEvent(StateMachine1Event.Op2);
        }
		''', generated.toString)
	}
	
}