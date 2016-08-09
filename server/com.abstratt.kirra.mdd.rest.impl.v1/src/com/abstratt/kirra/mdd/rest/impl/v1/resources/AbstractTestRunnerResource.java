package com.abstratt.kirra.mdd.rest.impl.v1.resources;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.UMLPackage;
import org.restlet.representation.Representation;

import com.abstratt.kirra.mdd.rest.KirraRESTUtils;
import com.abstratt.kirra.mdd.rest.impl.v1.resources.AbstractTestRunnerResource.TestResult.SourceLocation;
import com.abstratt.kirra.mdd.rest.impl.v1.resources.AbstractTestRunnerResource.TestResult.Status;
import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.RepositoryService;
import com.abstratt.mdd.core.runtime.ExecutionContext.CallSite;
import com.abstratt.mdd.core.runtime.ModelExecutionException;
import com.abstratt.mdd.core.runtime.Runtime;
import com.abstratt.mdd.core.runtime.RuntimeRaisedException;
import com.abstratt.mdd.core.runtime.types.BasicType;
import com.abstratt.mdd.core.util.FeatureUtils;
import com.abstratt.mdd.core.util.MDDExtensionUtils;
import com.abstratt.mdd.frontend.web.ResourceUtils;
import com.abstratt.nodestore.INodeStoreCatalog;
import com.abstratt.pluginutils.ISharedContextRunnable;

public abstract class AbstractTestRunnerResource extends AbstractKirraRepositoryResource {

    static class TestResult {
        static class SourceLocation {
            public String filename;
            public String frameName;
            public Integer lineNumber;

            public SourceLocation(String filename, Integer lineNumber, String frameName) {
                this.filename = filename;
                this.lineNumber = lineNumber;
                this.frameName = frameName;
            }
        }

        enum Status {
            Fail, Pass
        }

        public List<SourceLocation> errorLocation = new ArrayList<SourceLocation>();
        public String testCaseName;
        public String testClassName;
        public String testMessage;
        public Status testStatus;
        public SourceLocation testSourceLocation;

        public TestResult(String testClassName, String testCaseName, Status testStatus, String testMessage,
                SourceLocation testSourceLocation) {
            this.testClassName = testClassName;
            this.testCaseName = testCaseName;
            this.testStatus = testStatus;
            this.testMessage = testMessage;
            this.testSourceLocation = testSourceLocation;
        }
    }
    
    static class TestCase {
        public TestCase(String testClassName, String testCaseName) {
            this.testClass = testClassName;
            this.testCase = testCaseName;
        }
        public TestCase() {
        }
        
        public String testClass;
        public String testCase;
        public String testCaseUri;
    }
    
    protected TestResult runTestCaseAndRollback(final TestCase testCase) {
    	IRepository repository = RepositoryService.DEFAULT.getCurrentRepository();
        Operation testCaseOperation = repository.findNamedElement(testCase.testClass.replace(".", NamedElement.SEPARATOR) + NamedElement.SEPARATOR
                + testCase.testCase, UMLPackage.Literals.OPERATION, null);
        try {
        	return runTestCase(testCaseOperation);
        } finally {
        	RepositoryService.DEFAULT.getFeature(INodeStoreCatalog.class).zap();
        }
    }

    protected TestResult runTestCase(Operation testCase) {
        String testCaseName = testCase.getName();
        String testClassName = FeatureUtils.getOwningClassifier(testCase).getQualifiedName();
        TestResult testResult;
        final Runtime runtime = RepositoryService.DEFAULT.getFeature(Runtime.class);
        ResourceUtils.ensure(testCase != null, "Could not find operation", null);
        ResourceUtils.ensure(MDDExtensionUtils.isTestCase(testCase), "Not a test case operation", null);
        SourceLocation testLocation = findOperationLocation(testCase);
        try {
            BasicType instance = runtime.newInstance(testCase.getClass_());
            runtime.runOperation(null, instance, testCase);
            runtime.saveContext(false);
            if (TestResource.shouldFail(testCase)) {
                String errorMessage = "Should have failed";
                String expectedContext = TestResource.getExpectedContext(testCase);
                String expectedConstraint = TestResource.getExpectedConstraint(testCase);
                if (expectedContext != null)
                    errorMessage += " - expected context: " + expectedContext;
                if (expectedConstraint != null)
                    errorMessage += " - expected constraint: " + expectedConstraint;
                testResult = new TestResult(testClassName, testCaseName, TestResult.Status.Fail, errorMessage, testLocation);
                SourceLocation location = findOperationLocation(testCase);
                if (location != null)
                    testResult.errorLocation.add(location);
            } else
                testResult = new TestResult(testClassName, testCaseName, TestResult.Status.Pass, null, testLocation);
        } catch (ModelExecutionException mee) {
            String message = mee.getMessage();
            if (TestResource.shouldFail(testCase)) {
                String expectedContext = TestResource.getExpectedContext(testCase);
                String expectedConstraint = TestResource.getExpectedConstraint(testCase);
                String actualConstraint = null;
                if (mee instanceof RuntimeRaisedException) {
                    RuntimeRaisedException rre = (RuntimeRaisedException) mee;
                    actualConstraint = rre.getConstraint() == null ? null : rre.getConstraint().getName();
                }
                String actualContext = mee.getContext() == null ? null : mee.getContext().getName();
                boolean matchExpectation = true;
                if (!StringUtils.isBlank(expectedContext)
                        && !StringUtils.trimToEmpty(expectedContext).equals(StringUtils.trimToEmpty(actualContext))) {
                    matchExpectation = false;
                    message += " - Expected context: " + expectedContext + ", actual: " + actualContext;
                }
                if (!StringUtils.isBlank(expectedConstraint)
                        && !StringUtils.trimToEmpty(expectedConstraint).equals(StringUtils.trimToEmpty(actualConstraint))) {
                    matchExpectation = false;
                    message += " - Expected constraint: " + expectedConstraint + ", actual: " + actualConstraint;
                }
                testResult = new TestResult(testClassName, testCaseName, matchExpectation ? Status.Pass : Status.Fail,
                        matchExpectation ? null : message, testLocation);
            } else {
                testResult = new TestResult(testClassName, testCaseName, TestResult.Status.Fail, message, testLocation);
                for (CallSite callSite : mee.getCallSites())
                    testResult.errorLocation
                            .add(new SourceLocation(callSite.getSourceFile(), callSite.getLineNumber(), callSite.getFrameName()));
            }
        } catch (RuntimeException rre) {
            testResult = new TestResult(testClassName, testCaseName, TestResult.Status.Fail, rre.toString(), testLocation);
        }
        return testResult;
    }

    protected SourceLocation findOperationLocation(Operation operation) {
        return new SourceLocation(MDDExtensionUtils.getSource(operation), MDDExtensionUtils.getLineNumber(operation),
                operation.getQualifiedName());
    }
}
