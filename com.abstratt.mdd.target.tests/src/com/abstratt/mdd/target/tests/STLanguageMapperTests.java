//package com.abstratt.mdd.target.tests;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.Arrays;
//
//import junit.framework.Test;
//import junit.framework.TestSuite;
//
//import org.apache.commons.io.FileUtils;
//import org.eclipse.core.filesystem.EFS;
//import org.eclipse.core.runtime.CoreException;
//import org.eclipse.emf.ecore.EObject;
//import org.eclipse.emf.query.conditions.eobjects.EObjectCondition;
//import org.eclipse.uml2.uml.Activity;
//import org.eclipse.uml2.uml.CallOperationAction;
//import org.eclipse.uml2.uml.Class;
//import org.eclipse.uml2.uml.UMLPackage;
//import org.eclipse.uml2.uml.Variable;
//
//import com.abstratt.mdd.core.target.ITargetPlatform;
//import com.abstratt.mdd.core.target.ITopLevelMapper;
//import com.abstratt.mdd.core.target.TargetCore;
//import com.abstratt.mdd.core.tests.harness.AbstractRepositoryTests;
//import com.abstratt.mdd.core.util.ActivityUtils;
//import com.abstratt.mdd.core.util.MDDUtil;
//
//public class  STLanguageMapperTests extends AbstractRepositoryTests {
//
//    public STLanguageMapperTests(String name) {
//        super(name);
//    }
//
//    public void testCrossProjectInheritance() throws CoreException, IOException {
//        String source = "";
//        source += "model simple;\n";
//        source += "  class Class1\n";
//        source += "  end;\n";
//        source += "end.";
//        parseAndCheck(source);
//
//        File superTemplate = getBaseDir().getChild("superGroup.stg").toLocalFile(EFS.NONE, null);
//        superTemplate.getParentFile().mkdirs();
//        FileUtils.writeLines(superTemplate, Arrays.asList(
//                "group superGroup;",
//                "className(class) ::= \"class name is <class.name>\""
//            ));
//        File repositoryDir = getRepositoryDir().toLocalFile(EFS.NONE, null);
//        FileUtils.writeLines(new File(repositoryDir, "foobar.stg"), Arrays.asList(
//                "group subGroup : superGroup;",
//                "contents(class) ::= \"<class:className()>\""
//            ));
//        getRepository().getProperties().setProperty("mdd.target.engine", "stringtemplate");
//        getRepository().getProperties().setProperty("mdd.importedProjects", superTemplate.getParentFile().toURI().toString());
//        getRepository().getProperties().setProperty("mdd.target.foobar.template", "foobar.stg");
//        ITargetPlatform platform  = TargetCore.getPlatform(getRepository().getProperties(), "foobar");
//        ITopLevelMapper mapper = platform.getMapper(MDDUtil.fromEMFToJava(getRepositoryURI()));
//        Class class1 = getRepository().findNamedElement("simple::Class1", UMLPackage.Literals.CLASS, null);
//        assertEquals("class name is Class1", mapper.map(class1));
//    }
//
//
//    public void testMatching() throws CoreException, IOException {
//        String source = "";
//        source += "model simple;\n";
//        source += "  class Class1\n";
//        source += "  end;\n";
//        source += "  class Class2\n";
//        source += "  end;\n";
//        source += "end.";
//        parseAndCheck(source);
//
//        File repositoryDir = getRepositoryDir().toLocalFile(EFS.NONE, null);
//        FileUtils.writeLines(new File(repositoryDir, "foobar.stg"), Arrays.asList(
//            "group aGroup;",
//            "shouldMatch ::= [\"Class1\" : \"true\", default: \"false\"]",
//            "match(class) ::= \"<shouldMatch.(class.name)>\"",
//            "contents(class) ::= \"<class.name>\""
//        ));
//        getRepository().getProperties().setProperty("mdd.target.engine", "stringtemplate");
//        getRepository().getProperties().setProperty("mdd.target.foobar.template", "foobar.stg");
//        ITargetPlatform platform  = TargetCore.getPlatform(getRepository().getProperties(), "foobar");
//        ITopLevelMapper mapper = platform.getMapper(MDDUtil.fromEMFToJava(getRepositoryURI()));
//        Class class1 = getRepository().findNamedElement("simple::Class1", UMLPackage.Literals.CLASS, null);
//        Class class2 = getRepository().findNamedElement("simple::Class2", UMLPackage.Literals.CLASS, null);
//        assertEquals("Class1", mapper.map(class1));
//        assertNull(mapper.map(class2));
//    }
//
//    public void testMultiplicityElementRequired() throws CoreException, IOException {
//        String source = "";
//        source += "model simple;\n";
//        source += "  class Class1\n";
//        source += "      attribute attr1 : mdd_types::Integer;\n";
//        source += "  end;\n";
//        source += "  class Class2\n";
//        source += "      attribute attr2 : mdd_types::Integer[0,1];\n";
//        source += "  end;\n";
//        source += "end.";
//        parseAndCheck(source);
//
//        File repositoryDir = getRepositoryDir().toLocalFile(EFS.NONE, null);
//        FileUtils.writeLines(new File(repositoryDir, "foobar.stg"), Arrays.asList("group method;", "contents(class) ::= \"<class.ownedAttributes:{attr|Attribute: <attr.name> <attr.required>}>\""));
//        getRepository().getProperties().setProperty("mdd.target.engine", "stringtemplate");
//        getRepository().getProperties().setProperty("mdd.target.foobar.template", "foobar.stg");
//        ITargetPlatform platform  = TargetCore.getPlatform(getRepository().getProperties(), "foobar");
//        ITopLevelMapper mapper = platform.getMapper(MDDUtil.fromEMFToJava(getRepositoryURI()));
//        Class class1 = getRepository().findNamedElement("simple::Class1", UMLPackage.Literals.CLASS, null);
//        Class class2 = getRepository().findNamedElement("simple::Class2", UMLPackage.Literals.CLASS, null);
//        String mapped1 = mapper.map(class1);
//        assertTrue(mapped1, AssertHelper.areEqual("Attribute: attr1 true", mapped1));
//        String mapped2 = mapper.map(class2);
//        assertTrue(mapped2, AssertHelper.areEqual("Attribute: attr2 false", mapped2));
//    }
//
//    public void testIdentifierGeneration() throws CoreException, IOException {
//        String source = "";
//        source += "model simple;\n";
//        source += "  class Class1\n";
//        source += "      attribute attr1 : mdd_types::Integer;\n";
//        source += "      operation op1();\n";
//        source += "      begin\n";
//        source += "          Class1 extent.select((c : Class1) : mdd_types::Boolean { return true });\n";
//        source += "      end;\n";
//        source += "      operation op2();\n";
//        source += "      begin\n";
//        source += "          Class1 extent.reduce((c : Class1, b : mdd_types::Boolean) : mdd_types::Boolean { return not b }, true);\n";
//        source += "      end;\n";
//        source += "      operation op3();\n";
//        source += "      begin\n";
//        source += "          Class1 extent.collect((c : Class1) : mdd_types::Integer { return c.attr1 });\n";
//        source += "      end;\n";
//        source += "  end;\n";
//        source += "end.";
//        parseAndCheck(source);
//
//        File repositoryDir = getRepositoryDir().toLocalFile(EFS.NONE, null);
//        FileUtils.writeLines(new File(repositoryDir, "foobar.stg"), Arrays.asList(
//                "group aGroup;",
//                "parameterVariables(element) ::= \"<element.parameterVariables>\"",
//                "resultVariable(element) ::= \"<element.resultVariable>\"",
//                "suggestedName(element) ::= \"<element.suggestedName>\""
//            ));
//        getRepository().getProperties().setProperty("mdd.target.engine", "stringtemplate");
//        getRepository().getProperties().setProperty("mdd.target.foobar.template", "foobar.stg");
//        ITargetPlatform platform  = TargetCore.getPlatform(getRepository().getProperties(), "foobar");
//        ITopLevelMapper mapper = platform.getMapper(MDDUtil.fromEMFToJava(getRepositoryURI()));
//
//        CallOperationAction select = findCallOperationAction("select", "op1");
//        assertNotNull(select);
//        Variable selectVariable = findVariable("c", "op1");
//        assertNotNull(selectVariable);
//        assertEquals("c", mapper.applyChildMapper("parameterVariables", select).trim());
//        assertEquals("c", mapper.applyChildMapper("suggestedName", selectVariable).trim());
//        assertEquals("c", mapper.applyChildMapper("resultVariable", select).trim());
//
//        CallOperationAction reduce = findCallOperationAction("reduce", "op2");
//        assertNotNull(reduce);
//        Variable reduceVariable1 = findVariable("c", "op2");
//        assertNotNull(reduceVariable1);
//        Variable reduceVariable2 = findVariable("b", "op2");
//        assertNotNull(reduceVariable2);
//        assertEquals("cb", mapper.applyChildMapper("parameterVariables", reduce).trim());
//        assertEquals("c", mapper.applyChildMapper("suggestedName", reduceVariable1).trim());
//        assertEquals("b", mapper.applyChildMapper("suggestedName", reduceVariable2).trim());
//        assertEquals("b", mapper.applyChildMapper("resultVariable", reduce).trim());
//
//        CallOperationAction collect = findCallOperationAction("collect", "op3");
//        assertNotNull(collect);
//        Variable collectVariable = findVariable("c", "op3");
//        assertNotNull(collectVariable);
//        assertEquals("c", mapper.applyChildMapper("parameterVariables", collect).trim());
//        assertEquals("c", mapper.applyChildMapper("suggestedName", collectVariable).trim());
//        assertEquals("mapped", mapper.applyChildMapper("resultVariable", collect).trim());
//    }
//
//    public static Test suite() {
//        return new TestSuite(STLanguageMapperTests.class);
//    }
//
//    private CallOperationAction findCallOperationAction(final String opName, final String operationName) {
//        return (CallOperationAction) getRepository().findFirst(new EObjectCondition() {
//            public boolean isSatisfied(EObject object) {
//                if (!(object instanceof CallOperationAction))
//                    return false;
//                CallOperationAction coa = (CallOperationAction) object;
//                return coa.getOperation().getName().equals(opName) && ActivityUtils.getActionActivity(coa).getSpecification().getName().equals(operationName);
//            }
//        });
//    }
//
//    private Variable findVariable(final String varName, final String operationName) {
//        return (Variable) getRepository().findFirst(new EObjectCondition() {
//            public boolean isSatisfied(EObject object) {
//                if (!(object instanceof Variable))
//                    return false;
//                Variable var = (Variable) object;
//                return varName.equals(var.getName()) && operationName.equals(((Activity) MDDUtil.getNearest(var, UMLPackage.Literals.ACTIVITY).getOwner()).getSpecification().getName());
//            }
//        });
//    }
// }
