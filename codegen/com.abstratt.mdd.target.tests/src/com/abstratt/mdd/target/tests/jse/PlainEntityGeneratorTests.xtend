package com.abstratt.mdd.target.tests.jse

import com.abstratt.mdd.core.tests.harness.AssertHelper
import com.abstratt.mdd.target.jse.PlainEntityGenerator
import com.abstratt.mdd.target.tests.AbstractGeneratorTest

class PlainEntityGeneratorTests extends AbstractGeneratorTest {
    new(String name) {
        super(name)
    }
    
    private def testBodyGeneration(CharSequence operation, CharSequence expected) {
        var source = '''
            model mymodel;
                class MyClass
                    attribute anAttribute : Integer;
                    attribute anOptionalAttribute : Integer[0,1];
                    query aQuery() : Integer;
                    operation anAction();
                    «operation»
                end;
            end.
         '''
        parseAndCheck(source)
        val op1 = getOperation('mymodel::MyClass::op1') 
        val generated = new PlainEntityGenerator(repository).generateOperationBody(op1)
        AssertHelper.assertStringsEqual(
            expected.toString, generated.toString)
    }    

    /** See #179 and related issues. */
    def testPrecondition_RequiredParameter() {
        testBodyGeneration('''
            operation op1(par1 : Integer) 
                precondition (par1) { par1 >= 0 };
            ''',
            '''
            if (!(par1 >= 0L)) {
                throw new ConstraintViolationException();
            }
            '''
        )
    }
    def testPrecondition_OptionalParameter() {
        testBodyGeneration('''
            operation op1(par1 : Integer[0,1])
                precondition (par1) { par1 >= 0 };
            ''',
            '''
            if (par1 != null) {
                if (!((par1 != null && par1.compareTo(0L) >= 0))) {
                    throw new ConstraintViolationException();
                }
            }
            '''
        )
    }
    def testDateToday() {
        testBodyGeneration('''
            operation op1();
            begin
                var today;
                today := Date#today();
            end;
            ''',
            '''Date today=java.sql.Date.valueOf(java.time.LocalDate.now());'''
        )
    }
    def testDateArithmetic() {
        testBodyGeneration('''
            operation op1(refDate : Date);
            begin 
                var dayBefore;
                dayBefore := refDate.transpose(Duration#days(-1));
            end;
            ''',
            '''Date dayBefore = new Date(refDate.getTime()+-1L*(1000*60*60*24)/*days*/);'''
        )
    }
    
    def testIntegerAdd() {
        testBodyGeneration('''
            operation op1(val1 : Integer, val2 : Integer);
            begin
                var result;
                result := val1 + val2;
            end;
            ''',
            '''long result = val1 + val2;'''
        )
    }
    def testIntegerAddOptionalValues() {
        testBodyGeneration('''
            operation op1(val1 : Integer[0,1], val2 : Integer[0,1]);
            begin 
                var result;
                result := val1 + val2;
            end;
            ''',
            '''long result = (val1 == null ? 0 : val1) + (val2 == null ? 0 : val2);'''
        )
    }
    def testIntegerMultiplyOptionalValues() {
        testBodyGeneration('''
            operation op1(val1 : Integer[0,1], val2 : Integer[0,1]);
            begin 
                var result;
                result := val1 * val2;
            end;
            ''',
            '''long result = (val1 == null ? 0 : val1) * (val2 == null ? 0 : val2);'''
        )
    }
    def testIntegerDivideOptionalValues() {
        testBodyGeneration('''
            operation op1(val1 : Integer[0,1], val2 : Integer[0,1]);
            begin 
                var result;
                result := val1 / val2;
            end;
            ''',
            '''long result = (val1 == null ? 0 : val1) / (val2 == null ? 1 : val2);'''
        )
    }
    def testDoubleAdd() {
        testBodyGeneration('''
            operation op1(val1 : Double, val2 : Double);
            begin
                var result;
                result := val1 + val2;
            end;
            ''',
            '''double result = val1 + val2;'''
        )
    }
    def testDoubleAddOptionalValues() {
        testBodyGeneration('''
           operation op1(val1 : Double[0,1], val2 : Double[0,1]);
           begin
               var result;
               result := val1 + val2;
           end;
           ''',
            '''double result = (val1 == null ? 0 : val1) + (val2 == null ? 0 : val2);'''
        )
    }
    def testDoubleMultiplyOptionalValues() {
        testBodyGeneration('''
            operation op1(val1 : Double[0,1], val2 : Double[0,1]);
            begin
                var result;
                result := val1 * val2;
            end;
            ''',
            '''double result = (val1 == null ? 0 : val1) * (val2 == null ? 0 : val2);'''
        )
    }
    def testDoubleDivideOptionalValues() {
        testBodyGeneration('''
            operation op1(val1 : Double[0,1], val2 : Double[0,1]);
            begin
                var result;
                result := val1 / val2;
            end;
            ''',
            '''double result = (val1 == null ? 0 : val1) / (val2 == null ? 1 : val2);'''
        )
    }
    def testBooleanSafeAnd() {
        testBodyGeneration('''
            operation op1(val1 : Boolean[0,1], val2 : Boolean[0,1]);
            begin
                var result;
                result := val1 and val2;
            end;
            ''',
            '''boolean result = (val1 == null ? false : val1) && (val2 == null ? false : val2);'''
        )
    }
    def testBooleanSafeOr() {
        testBodyGeneration('''
            operation op1(val1 : Boolean[0,1], val2 : Boolean[0,1]);
            begin
                var result;
                result := val1 or val2;
            end;
            ''',
            '''boolean result = (val1 == null ? false : val1) || (val2 == null ? false : val2);'''
        )
    }
    def testComparison() {
        testBodyGeneration('''
            operation op1(val1 : Integer, val2 : Integer); 
            begin
                var result;
                result := val1 >= val2;
            end;
            ''',
            '''boolean result = val1 >= val2;'''
        )
    }
    def testSafeComparisonBothOptional() {
        testBodyGeneration('''
            operation op1(val1 : Integer[0,1], val2 : Integer[0,1]);
            begin
                var result;
                result := val1 >= val2;
            end;
            ''',
            '''boolean result = (val1 != null && val2 != null && val1.compareTo(val2) >= 0);'''
        )
    }    
    def testSafeComparisonTargetIsOptional() {
        testBodyGeneration('''
            operation op1(val1 : Integer[0,1], val2 : Integer);
            begin 
                var result;
                result := val1 >= val2;
            end;''',
            '''boolean result = (val1 != null && val1.compareTo(val2) >= 0);'''
        )
    }
    def testSafeComparisonArgumentIsOptional() {
        testBodyGeneration('''
            operation op1(val1 : Integer, val2 : Integer[0,1]); 
            begin
                var result;
                result := val1 >= val2;
            end;
            ''',
            '''boolean result = (val2 != null && val1.compareTo(val2) >= 0);'''
        )
    }
    
      
    /**
     * See: #157.
     */
    def testSafeQueryInvocation() {
        testBodyGeneration('''
            operation op1(val1 : MyClass[0,1]);
            begin 
                var result;
                result := val1.aQuery();
            end;''',
            '''long result = (val1 == null ? 0L : val1.aQuery());'''
        )
    }
    def testSafeActionInvocation() {
        testBodyGeneration('''
            operation op1(val1 : MyClass[0,1]);
            begin
                val1.anAction();
            end;
            ''', 
            '''
            if (val1 != null) {
                val1.anAction();
            }
            '''
        )
    }
    def testAttributeRead() {
        testBodyGeneration('''
            operation op1(val1 : MyClass);
            begin
                var result : Integer;
                result := val1.anAttribute;
            end;
            ''',
            '''long result = val1.getAnAttribute();'''
        )
    }
    def testAttributeReadOptionalValue() {
        testBodyGeneration('''
            operation op1(val1 : MyClass);
            begin
                var result : Integer;
                result := val1.anOptionalAttribute;
            end;
            ''',
            '''long result = (val1.getAnOptionalAttribute() == null ? 0L : val1.getAnOptionalAttribute());'''
        )
    }
    def testAttributeRead_OptionalSink() {
        testBodyGeneration('''
            operation op1(val1 : MyClass);
            begin
                var result : Integer[0,1];
                result := val1.anAttribute;
            end;
            ''',
            '''Long result = val1.getAnAttribute();'''
        )
    }
    def testAttributeReadOptionalValue_OptionalSink() {
        testBodyGeneration('''
            operation op1(val1 : MyClass);
            begin
                var result : Integer[0,1];
                result := val1.anOptionalAttribute;
            end;
            ''',
            '''Long result = val1.getAnOptionalAttribute();'''
        )
    }
    def testAttributeWrite() {
        testBodyGeneration('''
            operation op1(val1 : MyClass);
            begin
                val1.anAttribute := 1;
            end;
            ''',
            '''val1.setAnAttribute(1L);'''
        )
    }
    def testAttributeWriteOptionalValue() {
        testBodyGeneration('''
            operation op1(val1 : MyClass);
            begin
                val1.anAttribute := val1.anOptionalAttribute;
            end;
            ''',
            '''val1.setAnAttribute((val1.getAnOptionalAttribute() == null ? 0L : val1.getAnOptionalAttribute()));'''
        )
    }
    def testSafeAttributeReadOptionalValue() {
        testBodyGeneration('''
            operation op1(val1 : MyClass[0,1]);
            begin
                var result : Integer;
                result := val1.anOptionalAttribute;
            end;
            ''',
            '''long result = ((val1 == null || val1.getAnOptionalAttribute() == null) ? 0L : val1.getAnOptionalAttribute());'''
        )
    }
    def testSafeAttributeReadRequiredValue() {
        testBodyGeneration('''
            operation op1(val1 : MyClass[0,1]);
            begin
                var result : Integer;
                result := val1.anAttribute;
            end;
            ''',
            '''long result = (val1 == null ? 0L : val1.getAnAttribute());'''
        )
    }
    def testSafeAttributeWriteOptionalValue() {
        testBodyGeneration('''
            operation op1(val1 : MyClass[0,1]);
            begin
                val1.anAttribute := val1.anOptionalAttribute;
            end;
            ''',
            '''
            if (val1 != null) {
                val1.setAnAttribute(((val1 == null || val1.getAnOptionalAttribute() == null) ? 0L : val1.getAnOptionalAttribute()));
            }
            '''
        )
    }
    
    def testSafeAttributeWriteRequiredValue() {
        testBodyGeneration('''
            operation op1(val1 : MyClass[0,1]);
            begin
                val1.anAttribute := 1;
            end;
            ''',
            '''
            if (val1 != null) {
                val1.setAnAttribute(1L);
            }
            '''
        )
    }
    
}
