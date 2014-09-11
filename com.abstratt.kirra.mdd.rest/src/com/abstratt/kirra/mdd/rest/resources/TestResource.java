package com.abstratt.kirra.mdd.rest.resources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.query.conditions.eobjects.EObjectCondition;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.UMLPackage;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;

import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.RepositoryService;
import com.abstratt.mdd.core.util.StereotypeUtils;
import com.abstratt.mdd.frontend.web.ResourceUtils;

public class TestResource extends AbstractKirraRepositoryResource {

    static class TestCase {
        public String testCase;
        public String testClass;
        public String testCaseUri;
    }

    public static String getExpectedConstraint(Operation testCase) {
        Stereotype failure = StereotypeUtils.getStereotype(testCase, "Failure");
        return (String) testCase.getValue(failure, "constraint");
    }

    public static String getExpectedContext(Operation testCase) {
        Stereotype failure = StereotypeUtils.getStereotype(testCase, "Failure");
        return (String) testCase.getValue(failure, "context");
    }

    static boolean isTestCase(Operation op) {
        if (!op.isStatic() && StereotypeUtils.hasStereotype(op.getClass_(), "Test"))
            return true;
        return false;
    }

    static boolean shouldFail(Operation op) {
        return StereotypeUtils.hasStereotype(op, "Failure");
    }

    @Get
    public Representation listTests(Representation noneExpected) {
        IRepository repository = RepositoryService.DEFAULT.getFeature(IRepository.class);
        boolean testsEnabled = Boolean.parseBoolean(repository.getProperties().getProperty(IRepository.TESTS_ENABLED));
        ResourceUtils.ensure(testsEnabled, "Testing not enabled, please set property " + IRepository.TESTS_ENABLED
                + " to true, redeploy and try again", null);

        List<Operation> testCases = repository.findAll(new EObjectCondition() {
            @Override
            public boolean isSatisfied(EObject eObject) {
                if (UMLPackage.Literals.OPERATION != eObject.eClass())
                    return false;
                Operation op = (Operation) eObject;
                return TestResource.isTestCase(op);
            }
        }, true);
        List<TestCase> result = new ArrayList<TestResource.TestCase>();
        for (Operation testCaseOperation : testCases) {
            TestCase testCase = new TestCase();
            testCase.testClass = testCaseOperation.getClass_().getQualifiedName().replace(NamedElement.SEPARATOR, ".");
            testCase.testCase = testCaseOperation.getName();
            testCase.testCaseUri = getExternalReference().addSegment(testCase.testClass).addSegment(testCase.testCase).toString();

            result.add(testCase);
        }
        Collections.sort(result, new Comparator<TestCase>() {
            @Override
            public int compare(TestCase o1, TestCase o2) {
                int result = o1.testClass.compareTo(o2.testClass);
                if (result == 0)
                    result = o1.testCase.compareTo(o2.testCase);
                return result;
            }
        });
        return jsonToStringRepresentation(Collections.singletonMap("testCases", result));
    }
}
