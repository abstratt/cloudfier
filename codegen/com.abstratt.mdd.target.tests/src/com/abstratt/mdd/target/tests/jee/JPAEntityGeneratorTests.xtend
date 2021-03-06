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
        
        import mdd_media;
        
        class Class1
            attribute attr1 : String := "value 1";
            attribute attr2 : StateMachine1;
            attribute attr3 : Integer invariant { (self.attr3 > 0) };
            attribute attr4 : Integer[0,1] invariant { (self.attr4 == null) or (!!self.attr4 > 0) };
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
        
        class Class2
            attribute picture1 : Picture[0,1];
            attribute video1 : Video[0,1];
            attribute sound1 : Audio[0,1];
            attribute doc1 : Document[0,1];
        end;
        
        end.
        '''
        parseAndCheck(source)
    }
 
    @Test
    def void testAttributeGeneration() {
        buildModel()
        val property1 = getProperty('pack1::Class1::attr1')
        
        val generated = new JPAEntityGenerator(repository).generateAttribute(property1)
        AssertHelper.assertStringsEqual(
        '''
        protected String attr1 = "value 1";
        @Column(nullable=false)
        public String getAttr1(){ 
            return this.attr1;
        }
        public void setAttr1(String newAttr1){
            this.attr1=newAttr1;
        }
        ''', generated.toString)
    }
 
    
    @Test
    def void testEntityGeneration_setterConstraint() {
        buildModel()
        val attr3 = getProperty('pack1::Class1::attr3')
        
        val generated = new JPAEntityGenerator(repository).generateAttributeSetter(attr3)
        AssertHelper.assertStringsEqual(
        '''
        public void setAttr3(long newAttr3) {
            if (!(newAttr3 > 0L)) {
                // invariant violated 
                throw new ConstraintViolationException("");
            }
            this.attr3 = newAttr3;
        }
        ''', generated.toString)
        
    }
    
    @Test
    def void testEntityGeneration_setterConstraint_NullableAttribute() {
        buildModel()
        val attr4 = getProperty('pack1::Class1::attr4')
        
        val generated = new JPAEntityGenerator(repository).generateAttributeSetter(attr4)
        AssertHelper.assertStringsEqual(
        '''
        public void setAttr4(Long newAttr4) {
            if (!(newAttr4 != null && newAttr4.compareTo(0L) > 0)) {
                //invariant violated                 
                throw new ConstraintViolationException("");
            }
            this.attr4 = newAttr4;
        }
        ''', generated.toString)
    }
    
    @Test
    def void testEntityGeneration_customClassName() {
        val settings = createDefaultSettings()
        settings.put('mdd.application.name', 'myapp')
        settings.put('mdd.generator.jpa.mapping.pack1.Class1', 'my_class1')        
        saveSettings(getRepositoryDir(), settings)
        
        buildModel()
        val entity = getClass('pack1::Class1')
        val generated = new JPAEntityGenerator(repository).generateEntityAnnotations(entity)
        AssertHelper.assertStringsEqual(
        '''
        @Entity
        @Table(schema="myapp", name="my_class1")
        ''', generated.toString)
    }
    
    @Test
    def void testEntityGeneration_customColumnName() {
        val settings = createDefaultSettings()
        settings.put('mdd.generator.jpa.mapping.pack1.Class1.attr1', 'my_attr1')        
        saveSettings(getRepositoryDir(), settings)
        
        buildModel()
        val attrib1 = getProperty('pack1::Class1::attr1')
        val generated = new JPAEntityGenerator(repository).toJpaPropertyAnnotation(attrib1)
        AssertHelper.assertStringsEqual(
        '''
        @Column(nullable=false,name="my_attr1")
        ''', generated.toString)
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
    
    @Test
    def void testStateMachineGeneration() {
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
    def void testPictureAttributeGeneration() {
        buildModel()
        val property1 = getProperty('pack1::Class2::picture1')
        
        val generated = new JPAEntityGenerator(repository).generateAttribute(property1)
        AssertHelper.assertStringsEqual(
        '''
        protected String picture1;
        @Column
        public String getPicture1(){ 
            return this.picture1;
        }
        public void setPicture1(String newPicture1){
            this.picture1 = newPicture1;
        }
        ''', generated.toString)
    }
    
}