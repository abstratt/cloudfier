package com.abstratt.mdd.target.tests.jee

import com.abstratt.mdd.core.tests.harness.AssertHelper
import com.abstratt.mdd.target.jee.JPAServiceGenerator
import com.abstratt.mdd.target.tests.AbstractGeneratorTest
import java.io.IOException
import org.eclipse.core.runtime.CoreException
import org.junit.Test

class JPAServiceGeneratorTests extends AbstractGeneratorTest {

	new(String name) {
		super(name)
	}

	@Test
	def void testParameterDomain() throws CoreException, IOException {
		var source = '''
			package house;
			class House
			    attribute address : String;
			    attribute doors : Door[0, *];
			    operation knock(door : Door)
			        precondition HouseDoor(door) {
			            door.doorHouse == self
			        };
			end;
			class Door
			    attribute name : String;
			    attribute doorHouse : House;
			end;
			end.
		'''
		parseAndCheck(source)
		val knockOp = getOperation('house::House::knock')
		val houseClass = knockOp.class_
		val doorParameter = knockOp.getOwnedParameter("door", null)
		val generated = new JPAServiceGenerator(repository).generateActionParameterDomainQuery(houseClass, "thisHouse", doorParameter)
		AssertHelper.assertStringsEqual(
            '''
			SELECT DISTINCT door_ FROM Door door_, House thisHouse WHERE (door_.doorHouse = thisHouse) AND (thisHouse = :thisHouse)
		''', generated.toString)
	}

}