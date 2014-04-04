package com.abstratt.mdd.target.tests;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import junit.framework.Test;
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
import com.abstratt.mdd.core.util.MDDUtil;

public class  GStringLanguageMapperTests extends AbstractRepositoryBuildingTests {

    public GStringLanguageMapperTests(String name) {
        super(name);
    }


    public void testSimple() throws CoreException, IOException {
        String source = "";
        source += "model simple;\n";
        source += "  class Class1\n";
        source += "      attribute attr1 : mdd_types::Integer;\n";
        source += "  end;\n";
        source += "  class Class2\n";
        source += "      attribute attr2 : mdd_types::Integer;\n";
        source += "  end;\n";
        source += "end.";
        parseAndCheck(source);
        
        File repositoryDir = getRepositoryDir().toLocalFile(EFS.NONE, null);
        FileUtils.writeLines(new File(repositoryDir, "foobar.gt"), Arrays.asList("import com.abstratt.mdd.target.engine.gstring.*;\nimport org.eclipse.uml2.uml.*;\nclass Simple extends GroovyTemplate {\nString generate(Classifier clazz) {\nclazz.ownedAttributes.collect{attr-> \"Attribute: ${attr.name}\"}.join('\\n') } }"));
        getRepository().getProperties().setProperty("mdd.target.engine", "gstring");
        getRepository().getProperties().setProperty("mdd.target.foobar.template", "foobar.gt");
        ITargetPlatform platform  = TargetCore.getPlatform(getRepository().getProperties(), "foobar");
        ITopLevelMapper mapper = platform.getMapper(MDDUtil.fromEMFToJava(getRepositoryURI()));
        Class class1 = getRepository().findNamedElement("simple::Class1", UMLPackage.Literals.CLASS, null);
        String mapped1 = mapper.map(class1);
        assertTrue(mapped1, AssertHelper.areEqual("Attribute: attr1", mapped1));
        Class class2 = getRepository().findNamedElement("simple::Class2", UMLPackage.Literals.CLASS, null);
        String mapped2 = mapper.map(class2);
        assertTrue(mapped2, AssertHelper.areEqual("Attribute: attr2", mapped2));
    }

    public static Test suite() {
        return new TestSuite(GStringLanguageMapperTests.class);
    }
}