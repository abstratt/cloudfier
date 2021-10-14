//package com.abstratt.mdd.target.tests.mean
//
//import com.abstratt.mdd.target.mean.ExtentStage
//import com.abstratt.mdd.target.mean.FilterStage
//import com.abstratt.mdd.target.mean.QueryPipeline
//import java.io.IOException
//import junit.framework.Test
//import junit.framework.TestSuite
//import org.eclipse.core.runtime.CoreException
//
//import static extension com.abstratt.mdd.core.util.ActivityUtils.*
//import com.abstratt.mdd.target.mean.MappingStage
//import com.abstratt.mdd.target.tests.AbstractGeneratorTest
//
//class QueryPipelineTests extends AbstractGeneratorTest {
//
//    def static Test suite() {
//        return new TestSuite(QueryPipelineTests)
//    }
//
//    new(String name) {
//        super(name)
//    }
//
//    def testExtent() throws CoreException, IOException {
//        var source = '''
//        model crm;
//        class Customer
//              query findAll() : Customer[*];
//              begin
//                  return Customer extent;
//              end;
//        end;
//        end.
//        '''
//        parseAndCheck(source)
//        val op = getOperation('crm::Customer::findAll')
//
//        val root = op.activity.rootAction.findStatements.head.sourceAction
//        val pipeline = QueryPipeline.build(root)
//        assertEquals(1, pipeline.stageCount)
//        assertTrue(pipeline.getStage(0) instanceof ExtentStage)
//    }
//
//    def testSelect() throws CoreException, IOException {
//        val source = '''
//        model crm;
//        class Customer
//            attribute name : String;
//            attribute mvp : Boolean;
//            static query mvpCustomers() : Customer[*];
//            begin
//                return Customer extent.select((c : Customer) : Boolean { c.mvp = true});
//            end;            
//        end;
//        end.
//        '''
//        parseAndCheck(source)
//        val op = getOperation('crm::Customer::mvpCustomers')
//        val root = op.activity.rootAction.findStatements.head.sourceAction
//        val pipeline = QueryPipeline.build(root)
//        assertEquals(2, pipeline.stageCount)
//        assertTrue(pipeline.getStage(0) instanceof ExtentStage)
//        assertTrue(pipeline.getStage(1) instanceof FilterStage)
//        val filter = pipeline.getStage(1) as FilterStage
//        assertNotNull(filter.condition)
//    }
//    
//    def testMap() throws CoreException, IOException {
//        val source = '''
//        model crm;
//        class Account
//            attribute holder : Customer;
//            static query holders() : Customer[*];
//            begin
//                return Account extent.collect((a : Account) : Customer { a.holder });
//            end;            
//        end;
//        class Customer
//        end;
//        end.
//        '''
//        parseAndCheck(source)
//        val op = getOperation('crm::Account::holders')
//        val root = op.activity.rootAction.findStatements.head.sourceAction
//        val pipeline = QueryPipeline.build(root)
//        assertEquals(2, pipeline.stageCount)
//        assertTrue(pipeline.getStage(0) instanceof ExtentStage)
//        assertTrue(pipeline.getStage(1) instanceof MappingStage)
//        val mapping = pipeline.getStage(1) as MappingStage
//        assertNotNull(mapping.mapping)
//    }
//
//}
