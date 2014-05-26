package com.abstratt.kirra.mdd.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.query.conditions.eobjects.EObjectCondition;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.UMLPackage;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;

import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.RepositoryService;
import com.abstratt.mdd.core.util.MDDExtensionUtils;
import com.abstratt.mdd.core.util.StereotypeUtils;
import com.abstratt.mdd.frontend.web.ResourceUtils;

public class TestResource extends AbstractKirraRepositoryResource {
	
	static class TestCase {
		public String testCase;
		public String testClass;
		public String testCaseUri;
	}
	
	@Get
	public Representation listTests(Representation noneExpected) {
		IRepository repository = RepositoryService.DEFAULT.getFeature(IRepository.class);
		boolean testsEnabled = Boolean.parseBoolean(repository
				.getProperties().getProperty(IRepository.TESTS_ENABLED));
        ResourceUtils.ensure(testsEnabled, "Testing not enabled, please set property " + IRepository.TESTS_ENABLED + " to true, redeploy and try again", null);
		
		List<Operation> testCases = repository.findAll(new EObjectCondition() {
			@Override
			public boolean isSatisfied(EObject eObject) {
				if (UMLPackage.Literals.OPERATION != eObject.eClass())
					return false;
				Operation op = (Operation) eObject;
				return isTestCase(op);
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
		return jsonToStringRepresentation(Collections.singletonMap("testCases", result));
	}

	static boolean isTestCase(Operation op) {
		if (!op.isStatic() && StereotypeUtils.hasStereotype(op.getClass_(), "Test"))
			return true;
		return false;
	}
	
	static boolean shouldFail(Operation op) {
		return StereotypeUtils.hasStereotype(op, "Failure");
	}

	public static String getExpectedContext(Operation testCase) {
		Stereotype failure = StereotypeUtils.getStereotype(testCase, "Failure");
		return (String) testCase.getValue(failure, "context");
	}
	
	public static String getExpectedConstraint(Operation testCase) {
		Stereotype failure = StereotypeUtils.getStereotype(testCase, "Failure");
		return (String) testCase.getValue(failure, "constraint");
	}
}
