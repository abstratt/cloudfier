package com.abstratt.mdd.target.tests.mean

import com.abstratt.mdd.core.tests.harness.AssertHelper
import com.abstratt.mdd.target.mean.JSGenerator
import java.io.IOException
import junit.framework.Test
import junit.framework.TestSuite
import org.eclipse.core.runtime.CoreException

import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import com.abstratt.mdd.target.tests.AbstractGeneratorTest

class JSGeneratorTests extends AbstractGeneratorTest {

    JSGenerator generator = new JSGenerator

    def static Test suite() {
        return new TestSuite(JSGeneratorTests)
    }

    new(String name) {
        super(name)
    }

    def testDateDifferenceInDays() throws CoreException, IOException {
        var source = '''
        model simple;
          class Class1
              attribute date : Date;
              query difference() : Integer;
              begin
                  return self.date.differenceInDays(Date#now());
              end; 
          end;
        end.
        '''
        parseAndCheck(source)

        val operation = getOperation('simple::Class1::difference')
        val mapped = generator.generateActivity(operation.activity).toString
        
        AssertHelper.assertStringsEqual(
        '''
        {
            return (new Date() - this.date) / (1000*60*60*24);    
        }
        '''
        , mapped)
    }
    
    def testDateTranspose() throws CoreException, IOException {
        var source = '''
        model simple;
          class Class1
              attribute date : Date;
              query nextWeek() : Date;
              begin
                  return self.date.transpose(Duration#days(7));
              end; 
          end;
        end.
        '''
        parseAndCheck(source)

        val operation = getOperation('simple::Class1::nextWeek')
        val mapped = generator.generateActivity(operation.activity).toString
        
        AssertHelper.assertStringsEqual(
        '''
        {
            return new Date(this.date + 7 * 1000 * 60 * 60 * 24 /* days */);    
        }
        '''
        , mapped)
    }
}
