//package com.abstratt.mdd.target.tests;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.Arrays;
//import java.util.Map;
//
//import junit.framework.TestSuite;
//
//import org.apache.commons.io.FileUtils;
//import org.eclipse.core.filesystem.EFS;
//import org.eclipse.core.runtime.CoreException;
//import org.eclipse.uml2.uml.Class;
//import org.eclipse.uml2.uml.UMLPackage;
//
//import com.abstratt.mdd.core.target.ITargetPlatform;
//import com.abstratt.mdd.core.target.ITopLevelMapper;
//import com.abstratt.mdd.core.target.ITopLevelMapper.MapperInfo;
//import com.abstratt.mdd.core.target.TargetCore;
//import com.abstratt.mdd.core.target.Test;
//import com.abstratt.mdd.core.target.Test.Result;
//import com.abstratt.mdd.core.tests.harness.AbstractRepositoryTests;
//import com.abstratt.mdd.core.util.MDDUtil;
//
//public class TargetPlatformTestingTests extends AbstractRepositoryTests {
//
//	public TargetPlatformTestingTests(String name) {
//		super(name);
//	}
//	
//	public void testTestDiscovery() throws CoreException, IOException {
//        File repositoryDir = getRepositoryDir().toLocalFile(EFS.NONE, null);
//        FileUtils.writeLines(new File(repositoryDir, "foobar.stg"), Arrays.asList("group suite;", "expected_test1() ::= \"\"\nexpected_test2() ::= \"\"\nactual_test3() ::= \"\"\nactual_test1() ::= \"\"\nexpected_test4() ::= \"\"\nactual_test4() ::= \"\"\n"));
//        getRepository().getProperties().setProperty("mdd.target.engine", "stringtemplate");
//        getRepository().getProperties().setProperty("mdd.target.foobar.template", "foobar.stg");
//        ITargetPlatform platform  = TargetCore.getPlatform(getRepository().getProperties(), "foobar");
//        ITopLevelMapper mapper = platform.getMapper(MDDUtil.fromEMFToJava(getRepositoryURI()));
//        Map<String, Test> tests = TargetCore.getTests(mapper);
//        assertEquals(2, tests.size());
//        assertTrue(tests.containsKey("test1"));
//        assertTrue(tests.containsKey("test4"));
//    }
//	
//	   public void testTestDescription() throws CoreException, IOException {
//	        File repositoryDir = getRepositoryDir().toLocalFile(EFS.NONE, null);
//	        FileUtils.writeLines(new File(repositoryDir, "foobar.stg"), Arrays.asList("group foobar;", "expected_test1() ::= \"\"\nactual_test1(element, elementName = \"somePackage::someClass\") ::= \"\"\n"));
//	        getRepository().getProperties().setProperty("mdd.target.engine", "stringtemplate");
//	        getRepository().getProperties().setProperty("mdd.target.foobar.template", "foobar.stg");
//	        ITargetPlatform platform  = TargetCore.getPlatform(getRepository().getProperties(), "foobar");
//	        ITopLevelMapper mapper = platform.getMapper(MDDUtil.fromEMFToJava(getRepositoryURI()));
//	        Map<String, Test> tests = TargetCore.getTests(mapper);
//	        MapperInfo description = tests.get("test1").describe(mapper);
//	        assertEquals("foobar.stg", description.getFileName());
//	        assertEquals("actual_test1", description.getMapperName());
//	        assertEquals(3, description.getLocation());
//	        assertEquals("somePackage::someClass", tests.get("test1").getTestedElement());
//	    }
//
//	
//   public void testTestPass() throws CoreException, IOException {
//        File repositoryDir = getRepositoryDir().toLocalFile(EFS.NONE, null);
//        FileUtils.writeLines(new File(repositoryDir, "foobar.stg"), Arrays.asList("group suite;", "expected_test1() ::= \"output\"\nactual_test1() ::= \"output\"\n"));
//        getRepository().getProperties().setProperty("mdd.target.engine", "stringtemplate");
//        getRepository().getProperties().setProperty("mdd.target.foobar.template", "foobar.stg");
//        ITargetPlatform platform  = TargetCore.getPlatform(getRepository().getProperties(), "foobar");
//        ITopLevelMapper mapper = platform.getMapper(MDDUtil.fromEMFToJava(getRepositoryURI()));
//        Map<String, Test> tests = TargetCore.getTests(mapper);
//        Result result = tests.get("test1").test(mapper, null);
//        assertTrue(result.getMessage(), Test.Result.Kind.Pass == result.getKind());
//    }
//   
//   public void testTestWithParameter() throws CoreException, IOException {
//       String source = "";
//       source += "model simple;\n";
//       source += "  class X\n";
//       source += "  end;\n";
//       source += "end.";
//       parseAndCheck(source);
//       Class parameter = getRepository().findNamedElement("simple::X", UMLPackage.Literals.CLASS, null);
//
//       File repositoryDir = getRepositoryDir().toLocalFile(EFS.NONE, null);
//       FileUtils.writeLines(new File(repositoryDir, "foobar.stg"), Arrays.asList("group suite;", "expected_test1() ::= \"outputX\"\nactual_test1(element) ::= \"output<element.name>\"\n"));
//       getRepository().getProperties().setProperty("mdd.target.engine", "stringtemplate");
//       getRepository().getProperties().setProperty("mdd.target.foobar.template", "foobar.stg");
//       ITargetPlatform platform  = TargetCore.getPlatform(getRepository().getProperties(), "foobar");
//       ITopLevelMapper mapper = platform.getMapper(MDDUtil.fromEMFToJava(getRepositoryURI()));
//       Map<String, Test> tests = TargetCore.getTests(mapper);
//       Result result = tests.get("test1").test(mapper, parameter);
//       assertTrue(result.getMessage(), Test.Result.Kind.Pass == result.getKind());
//   }
//
//   public void testTestFail() throws CoreException, IOException {
//       File repositoryDir = getRepositoryDir().toLocalFile(EFS.NONE, null);
//       FileUtils.writeLines(new File(repositoryDir, "foobar.stg"), Arrays.asList("group suite;", "expected_test1() ::= \"outputX\"\nactual_test1() ::= \"outputY\"\n"));
//       getRepository().getProperties().setProperty("mdd.target.engine", "stringtemplate");
//       getRepository().getProperties().setProperty("mdd.target.foobar.template", "foobar.stg");
//       ITargetPlatform platform  = TargetCore.getPlatform(getRepository().getProperties(), "foobar");
//       ITopLevelMapper mapper = platform.getMapper(MDDUtil.fromEMFToJava(getRepositoryURI()));
//       Map<String, Test> tests = TargetCore.getTests(mapper);
//       Result result = tests.get("test1").test(mapper, null);
//       assertTrue(result.getMessage(), Test.Result.Kind.Fail == result.getKind());
//   }
//   
//	public static junit.framework.Test suite() {
//		return new TestSuite(TargetPlatformTestingTests.class);
//	}
//}
