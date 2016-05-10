package com.abstratt.kirra.mdd.rest.impl.v1.resources;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.query.conditions.eobjects.EObjectCondition;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.UMLPackage;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

import com.abstratt.kirra.mdd.rest.KirraRESTUtils;
import com.abstratt.kirra.mdd.rest.impl.v1.resources.AbstractTestRunnerResource.TestResult.Status;
import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.util.MDDExtensionUtils;
import com.abstratt.mdd.core.util.StereotypeUtils;
import com.abstratt.mdd.frontend.web.ResourceUtils;
import com.abstratt.pluginutils.ISharedContextRunnable;

public class TestResource extends AbstractTestRunnerResource {

    public static String getExpectedConstraint(Operation testCase) {
        Stereotype failure = StereotypeUtils.getStereotype(testCase, "Failure");
        return (String) testCase.getValue(failure, "constraint");
    }

    public static String getExpectedContext(Operation testCase) {
        Stereotype failure = StereotypeUtils.getStereotype(testCase, "Failure");
        return (String) testCase.getValue(failure, "context");
    }

    static boolean shouldFail(Operation op) {
        return StereotypeUtils.hasStereotype(op, "Failure");
    }

    @Get
    public Representation listTests(Representation noneExpected) {
        List<TestCase> testCases = collectTestCases();
        Collections.sort(testCases, new Comparator<TestCase>() {
            @Override
            public int compare(TestCase o1, TestCase o2) {
                int result = o1.testClass.compareTo(o2.testClass);
                if (result == 0)
                    result = o1.testCase.compareTo(o2.testCase);
                return result;
            }
        });
        return jsonToStringRepresentation(Collections.singletonMap("testCases", testCases));
    }
    
    @Post
    public Representation runSuite(Representation noneExpected) {
        // first we enter a context to find all test cases
        List<TestCase> testCases = collectTestCases();
        // now we run them one by one, using a (potentially) different context every time
        List<TestResult> testCaseResults = testCases.stream().map(testCase -> runTestCaseAndRollback(testCase)).collect(Collectors.toList());
        Map<String, Number> stats = new LinkedHashMap<>();
        long passed = testCaseResults.stream().filter(testCase -> testCase.testStatus == Status.Pass).count();
        stats.put("total", testCaseResults.size());
        stats.put("passed", passed);
        stats.put("failed", testCaseResults.size() - passed);        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("results", testCaseResults);
        result.put("stats", stats);        
        return jsonToStringRepresentation(result);
    }

    private List<TestCase> collectTestCases() {
        return KirraRESTUtils.runInKirraRepository(getRequest(), new ISharedContextRunnable<IRepository, List<TestCase>>() {
            @Override
            public List<TestCase> runInContext(IRepository context) {
                List<Operation> testCaseOperations = context.findAll(new EObjectCondition() {
                    @Override
                    public boolean isSatisfied(EObject eObject) {
                        if (UMLPackage.Literals.OPERATION != eObject.eClass())
                            return false;
                        Operation op = (Operation) eObject;
                        return MDDExtensionUtils.isTestCase(op);
                    }
                }, true);
                return testCaseOperations.stream().map(operation -> asTestCase(operation)).collect(Collectors.toList());
            }

        });
    }
    private TestCase asTestCase(Operation testCaseOperation) {
        TestCase testCase = new TestCase();
        testCase.testClass = testCaseOperation.getClass_().getQualifiedName().replace(NamedElement.SEPARATOR, ".");
        testCase.testCase = testCaseOperation.getName();
        testCase.testCaseUri = getExternalReference().addSegment(testCase.testClass).addSegment(testCase.testCase).toString();
        return testCase;
    }
}
