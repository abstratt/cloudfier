package com.abstratt.mdd.target.tests;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.UMLPackage;

import com.abstratt.mdd.core.target.ITargetPlatform;
import com.abstratt.mdd.core.target.ITopLevelMapper;
import com.abstratt.mdd.core.target.TargetCore;
import com.abstratt.mdd.core.tests.harness.AbstractRepositoryBuildingTests;
import com.abstratt.mdd.core.tests.harness.AssertHelper;
import com.abstratt.mdd.core.tests.harness.FixtureHelper;
import com.abstratt.mdd.core.util.MDDUtil;
import com.abstratt.mdd.frontend.core.IProblem;

//import com.abstratt.mdd.target.engine.st.STValidator.STProblem;

public class CustomPlatformTests extends AbstractRepositoryBuildingTests {

    public static junit.framework.Test suite() {
        return new TestSuite(CustomPlatformTests.class);
    }

    public CustomPlatformTests(String name) {
        super(name);
    }

    public void _testPojoExtensionWithGetter() throws CoreException, IOException {
        String source = "";
        source += "model simple;\n";
        source += "  class Account\n";
        source += "      attribute attr1 : mdd_types::Integer;\n";
        source += "  end;\n";
        source += "end.";
        parseAndCheck(source);

        File repositoryDir = getRepositoryDir().toLocalFile(EFS.NONE, null);
        FileUtils.writeLines(new File(repositoryDir, "foobar.stg"), Arrays.asList("group method;", "contents(class) ::= <<",
                "<class.ownedAttributes:{attr|Attribute: <attr.name;format=\"capitalize\">}>", ">>"));
        getRepository().getProperties().setProperty("mdd.target.engine", "stringtemplate");
        getRepository().getProperties().setProperty("mdd.target.foobar.template", "foobar.stg");
        getRepository().getProperties().setProperty("mdd.target.foobar.base", "pojo");
        ITargetPlatform platform = TargetCore.getPlatform(getRepository().getProperties(), "foobar");
        ITopLevelMapper mapper = platform.getMapper(MDDUtil.fromEMFToJava(getRepositoryURI()));
        Class toMap = getRepository().findNamedElement("simple::Account", UMLPackage.Literals.CLASS, null);
        String mapped = mapper.map(toMap);
        TestCase.assertTrue(mapped, AssertHelper.areEqual("Attribute: Attr1", mapped));
    }

    // public void testBasic() throws CoreException, IOException {
    // String source = "";
    // source += "model simple;\n";
    // source += "  class Account\n";
    // source += "  end;\n";
    // source += "end.";
    // parseAndCheck(source);
    //
    // File repositoryDir = getRepositoryDir().toLocalFile(EFS.NONE, null);
    // FileUtils.writeLines(new File(repositoryDir, "foobar.stg"),
    // Arrays.asList("group foo;",
    // "contents(class) ::= \"hello, <class.name>\""));
    // getRepository().getProperties().setProperty("mdd.target.engine",
    // "stringtemplate");
    // getRepository().getProperties().setProperty("mdd.target.foobar.template",
    // "foobar.stg");
    // ITargetPlatform platform =
    // TargetCore.getPlatform(getRepository().getProperties(), "foobar");
    // ITopLevelMapper mapper =
    // platform.getMapper(MDDUtil.fromEMFToJava(getRepositoryURI()));
    // Class toMap = getRepository().findNamedElement("simple::Account",
    // UMLPackage.Literals.CLASS, null);
    // String mapped = mapper.map(toMap);
    // assertEquals("hello, Account", mapped);
    // }

    /** Extension of hardcoded generators has been dropped. */
    public void _testPojoExtensionWithMethod() throws CoreException, IOException {
        String source = "";
        source += "model simple;\n";
        source += "  class Account\n";
        source += "      operation op1() : mdd_types::Integer;\n";
        source += "      begin\n";
        source += "          return 1;\n";
        source += "      end;\n";
        source += "  end;\n";
        source += "end.";
        parseAndCheck(source);

        File repositoryDir = getRepositoryDir().toLocalFile(EFS.NONE, null);
        FileUtils.writeLines(new File(repositoryDir, "foobar.stg"),
                Arrays.asList("group method;", "contents(class) ::= \"<class.ownedOperations:{op|<op.method>}>\""));
        getRepository().getProperties().setProperty("mdd.target.engine", "stringtemplate");
        getRepository().getProperties().setProperty("mdd.target.foobar.template", "foobar.stg");
        getRepository().getProperties().setProperty("mdd.target.foobar.base", "pojo");
        ITargetPlatform platform = TargetCore.getPlatform(getRepository().getProperties(), "foobar");
        ITopLevelMapper mapper = platform.getMapper(MDDUtil.fromEMFToJava(getRepositoryURI()));
        Class toMap = getRepository().findNamedElement("simple::Account", UMLPackage.Literals.CLASS, null);
        String mapped = mapper.map(toMap);
        TestCase.assertTrue(mapped, AssertHelper.areEqual("{ return 1;}", mapped));
    }

    public void _testTemplateValidationSimple() throws CoreException {
        String source = "";
        source += "group p;\n\n";
        source += "foo(p) ::= \"\"\n";
        IProblem[] results = fixtureHelper.parseFiles(getBaseDir(), getRepository(), Collections.singletonMap(getName() + ".stg", source));
        FixtureHelper.assertCompilationSuccessful(results);
    }

    public void testPlatforms() throws CoreException, IOException {
        getRepository().getProperties().setProperty("mdd.target.engine", "gstring");
        getRepository().getProperties().setProperty("mdd.target.foo.template", "");
        getRepository().getProperties().setProperty("mdd.target.bar.template", "");
        Collection<String> platforms = TargetCore.getPlatformIds(getRepository().getProperties());
        TestCase.assertEquals(2, platforms.size());
        TestCase.assertTrue(platforms.contains("foo"));
        TestCase.assertTrue(platforms.contains("bar"));
    }

    public void testTemplateValidationMissingParent() throws CoreException {
        String source = "";
        source += "group p : unknownParent;\n";
        source += "foo(p) ::= \"\"\n";
        IProblem[] results = fixtureHelper.parseFiles(getBaseDir(), getRepository(), Collections.singletonMap(getName() + ".stg", source));
        FixtureHelper.assertTrue(results, results.length == 1);
        // FixtureHelper.assertTrue(results, results[0] instanceof STProblem);
    }
}
