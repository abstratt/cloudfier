package com.abstratt.mdd.target.tests.jse

import com.abstratt.mdd.core.tests.harness.AssertHelper
import com.abstratt.mdd.target.jse.PlainEntityGenerator
import com.abstratt.mdd.target.tests.AbstractGeneratorTest

class PlainEntityGeneratorTests extends AbstractGeneratorTest {
    new(String name) {
        super(name)
    }

//    /** See #179 and related issues. */
//    def testPrecondition() {
//        var source = '''
//            model mymodel;
//                class MyClass
//                    operation op1(par1 : Integer[0,1])
//                        precondition (par1) { par1 >= 0 };
//                end;
//            end.
//         '''
//        parseAndCheck(source)
//        val op1 = getOperation('mymodel::MyClass::op1')
//        val generated = new PlainEntityGenerator(repository).generateOperationBody(op1)
//        AssertHelper.assertStringsEqual(
//            '''
//            if (!((par1 == null) || (par1.compareTo(0L) >= 0))) {
//                throw new ConstraintViolationException();
//            }
//            ...
//            ''', generated.toString)
//        
//    }

    private def testBodyGeneration(CharSequence parameters, CharSequence vars, CharSequence input, CharSequence expected) {
        var source = '''
            model mymodel;
                class MyClass
                    operation op1(«parameters»);
                    begin
                        var «vars»;
                        «input»
                    end;    
                end;
            end.
         '''
        parseAndCheck(source)
        val op1 = getOperation('mymodel::MyClass::op1') 
        val generated = new PlainEntityGenerator(repository).generateOperationBody(op1)
        AssertHelper.assertStringsEqual(
            expected.toString, generated.toString)
        
    }
    def testDateToday() {
        testBodyGeneration('', 
            '''today''',
            '''today := Date#today();''',
            '''Date today=java.sql.Date.valueOf(java.time.LocalDate.now());'''
        )
    }
    def testDateDayAfter() {
        testBodyGeneration('''refDate : Date''', 
            '''dayAfter''',
            '''dayAfter := refDate.transpose(Duration#days(1));''',
            '''Date dayAfter = new Date(refDate.getTime()+1L*(1000*60*60*24)/*days*/);'''
        )
    }
    def testDateDayBefore() {
        testBodyGeneration('''refDate : Date''', 
            '''dayBefore''',
            '''dayBefore := refDate.transpose(Duration#days(-1));''',
            '''Date dayBefore = new Date(refDate.getTime()+-1L*(1000*60*60*24)/*days*/);'''
        )
    }
    def testDateSafeDayBefore() {
        testBodyGeneration('''refDate : Date[0, 1]''', 
            '''dayBefore''',
            '''dayBefore := refDate.transpose(Duration#days(-1));''',
            '''Date dayBefore = new Date(refDate.getTime()+-1L*(1000*60*60*24)/*days*/);'''
        )
    }
    
}
