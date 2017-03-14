package com.abstratt.mdd.target.tests.jee

import com.abstratt.mdd.target.tests.jse.PlainEntityBehaviorGenerationTests
import com.abstratt.mdd.target.jee.JPAEntityGenerator

class JPAEntityBehaviorGenerationTests extends PlainEntityBehaviorGenerationTests {

    new(String name) {
        super(name)
    }

    override protected createEntityGenerator() {
        new JPAEntityGenerator(repository)
    }

    override testUnlink_OptionalCurrent() {
        testBodyGeneration(
            '''
                operation op1();
                begin
                    unlink ManyMyClass1OneMyClass2(related1B := self, optionalRelated1 := self.optionalRelated1);
                end;
            ''',
            '''
                if (this.optionalRelated1 != null) {
                    this.optionalRelated1.removeFromRelated1B(this);
                    this.setOptionalRelated1(null);
                }
            '''
        )
    }

    override testReplace_OptionalGiven() {
        testBodyGeneration(
            '''
                operation op1(given : MyClass2);
                begin
                    unlink ManyMyClass2OneMyClass1(related1C := self, optionalRelated2 := given);
                end;
            ''',
            '''
                if (given != null) {
                    this.removeFromOptionalRelated2(given);
                    given.setRelated1C(null);
                }
            '''
        )
    }

}
