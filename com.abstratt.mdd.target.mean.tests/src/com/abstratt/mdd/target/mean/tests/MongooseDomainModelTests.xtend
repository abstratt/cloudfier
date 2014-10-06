package com.abstratt.mdd.target.mean.tests

import com.abstratt.kirra.mdd.core.KirraMDDCore
import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.target.TargetCore
import com.abstratt.mdd.core.tests.harness.AbstractRepositoryBuildingTests
import com.abstratt.mdd.core.tests.harness.AssertHelper
import java.io.IOException
import java.util.Properties
import junit.framework.Test
import junit.framework.TestSuite
import org.eclipse.core.runtime.CoreException
import org.eclipse.uml2.uml.UMLPackage

class MongooseDomainModelTests extends AbstractRepositoryBuildingTests {

    def static Test suite() {
        return new TestSuite(MongooseDomainModelTests)
    }

    new(String name) {
        super(name)
    }

    def testSimpleModel() throws CoreException, IOException {
        var source = '''
        model simple;
          class Class1
              attribute attr1 : String;
              attribute attr2 : Integer;
              attribute attr3 : Date;            
          end;
        end.
        '''
        parseAndCheck(source)

        val platform = TargetCore.getPlatform(getRepository().getProperties(), "mean")
        val mapper = platform.getMapper(null)
        val class1 = getRepository().findNamedElement("simple::Class1", UMLPackage.Literals.CLASS, null)
        val mapped = mapper.map(class1).toString()
        assertTrue(mapped, AssertHelper.areEqual(
        '''
        var class1Schema = new Schema({ 
            attr1: String, 
            attr2: Number,
            attr3: Date
        }); 
        var Class1 = mongoose.model('Class1', class1Schema);      
        '''
        , mapped))
    }
    
    def _testAction() throws CoreException, IOException {
        val source = '''
        model simple;
        
        class Class1
            attribute attr1 : Integer;
            operation incAttr1(value : Integer);
            begin
                self.attr1 := self.attr1 + value;
            end;            
        end;
        end.
        '''
        parseAndCheck(source)

        val platform = TargetCore.getPlatform(getRepository().getProperties(), "mean")
        val mapper = platform.getMapper(null)
        val class1 = getRepository().findNamedElement("simple::Class1", UMLPackage.Literals.CLASS, null)
        val mapped = mapper.map(class1).toString()
        AssertHelper.assertStringsEqual(
        '''
        var class1Schema = new Schema({ 
            attr1: Number 
        }); 
        class1Schema.methods.incAttr1 = function (value) {
            this.attr1 = this.attr1 + value; 
        };
        var Class1 = mongoose.model('Class1', class1Schema);      
        ''', mapped)
    }
    
    
    override Properties createDefaultSettings() {
        val defaultSettings = super.createDefaultSettings()
        // so the kirra profile is available as a system package (no need to
        // load)
        defaultSettings.setProperty("mdd.enableKirra", Boolean.TRUE.toString())
        // so kirra stereotypes are automatically applied
        defaultSettings.setProperty(IRepository.WEAVER, KirraMDDCore.WEAVER)
        // so classes extend Object by default (or else weaver ignores them)
        defaultSettings.setProperty(IRepository.EXTEND_BASE_OBJECT, Boolean.TRUE.toString())
        return defaultSettings
    }
}
