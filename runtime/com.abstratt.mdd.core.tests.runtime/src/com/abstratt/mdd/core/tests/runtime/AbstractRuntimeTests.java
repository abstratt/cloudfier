package com.abstratt.mdd.core.tests.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.Property;

import com.abstratt.kirra.ExternalService;
import com.abstratt.kirra.SchemaManagement;
import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.RepositoryService;
import com.abstratt.mdd.core.runtime.Runtime;
import com.abstratt.mdd.core.runtime.RuntimeClass;
import com.abstratt.mdd.core.runtime.RuntimeObject;
import com.abstratt.mdd.core.runtime.types.BasicType;
import com.abstratt.mdd.core.tests.harness.AbstractRepositoryBuildingTests;
import com.abstratt.mdd.core.util.FeatureUtils;
import com.abstratt.nodestore.INodeStoreCatalog;
import com.abstratt.nodestore.INodeStoreFactory;
import com.abstratt.nodestore.NodeStores;

public class AbstractRuntimeTests extends AbstractRepositoryBuildingTests {

    interface FallibleRunnable<T extends Throwable> {
        public void run() throws T;
    }

    protected ExternalService externalService;

    public AbstractRuntimeTests(String name) {
        super(name);
    }

    public com.abstratt.kirra.Repository getKirraRepository() {
        return RepositoryService.DEFAULT.getCurrentResource().getFeature(com.abstratt.kirra.Repository.class);
    }

    public RuntimeClass getRuntimeClass(Runtime runtime, String className) {
        Classifier classInstance = (Classifier) getRepository().findNamedElement(className, IRepository.PACKAGE.getClassifier(), null);
        TestCase.assertNotNull(className, classInstance);
        return getRuntime().getRuntimeClass(classInstance);
    }

    public RuntimeClass getRuntimeClass(String className) {
        return getRuntimeClass(getRuntime(), className);
    }

    public BasicType readAttribute(RuntimeObject target, String name) {
        Classifier classInstance = target.getRuntimeClass().getModelClassifier();
        Property attribute = FeatureUtils.findAttribute(classInstance, name, false, true);
        TestCase.assertNotNull(classInstance.getQualifiedName() + NamedElement.SEPARATOR + name, attribute);
        return target.getValue(attribute);
    }

    /**
     * This convenience method does NOT do parameter-based matching.
     */
    public Object runOperation(BasicType target, String operationName, Object... arguments) {
        Classifier classifier = (Classifier) getRepository().findNamedElement(target.getClassifierName(),
                IRepository.PACKAGE.getClassifier(), null);
        TestCase.assertNotNull(target.getClassifierName(), classifier);
        Operation operation = FeatureUtils.findOperation(getRepository(), classifier, operationName, null);
        TestCase.assertNotNull(operationName, operation);
        return getRuntime().runOperation(null, target, operation, arguments);
    }

    /**
     * This convenience method does NOT do parameter-based matching.
     */
    public Object runStaticOperation(String className, String operationName, Object... arguments) {
        Class classifier = (Class) getRepository().findNamedElement(className, IRepository.PACKAGE.getClass_(), null);
        TestCase.assertNotNull("Not found: " + className, classifier);
        Operation operation = FeatureUtils.findOperation(getRepository(), classifier, operationName, null);
        TestCase.assertNotNull("Operation not found: " + classifier.getQualifiedName() + NamedElement.SEPARATOR + operationName, operation);
        return getRuntime().runOperation(null, null, operation, arguments);
    }

    public void sendSignal(BasicType target, String signalName, Map<String, BasicType> arguments) {
        RuntimeObject signalInstance = getRuntimeClass(signalName).newInstance(false, false);
        for (Entry<String, BasicType> entry : arguments.entrySet())
            writeAttribute(signalInstance, entry.getKey(), entry.getValue());
        getRuntime().sendSignal(target, signalInstance);
    }

    public void sendSignal(RuntimeObject target, RuntimeObject arguments) {
        Classifier classifier = (Classifier) getRepository().findNamedElement(target.getClassifierName(),
                IRepository.PACKAGE.getClassifier(), null);
        TestCase.assertNotNull(target.getClassifierName(), classifier);
        getRuntime().sendSignal(target, arguments);
    }

    public void writeAttribute(final RuntimeObject target, String name, final BasicType value) {
        Classifier classInstance = target.getRuntimeClass().getModelClassifier();
        final Property property = FeatureUtils.findAttribute(classInstance, name, false, true);
        TestCase.assertNotNull(classInstance.getQualifiedName() + NamedElement.SEPARATOR + name, property);
        getRuntime().runSession(new Runtime.Session<Object>() {
            @Override
            public Object run() {
                target.setValue(property, value);
                return null;
            }
        });
    }

    @Override
    protected void compilationCompleted() throws CoreException {
        setupRuntime();
    }

    @Override
    protected Properties createDefaultSettings() {
        Properties defaultSettings = super.createDefaultSettings();
        // so the kirra profile is available as a system package (no need to
        // load)
        defaultSettings.setProperty(IRepository.EXTEND_BASE_OBJECT, Boolean.TRUE.toString());
        defaultSettings.setProperty("mdd.enableKirra", Boolean.TRUE.toString());
        defaultSettings.setProperty("mdd.modelWeaver", "kirraWeaver");
        defaultSettings.setProperty("mdd.runtime.nodestore", "jdbc");

        return defaultSettings;
    }

    protected INodeStoreCatalog getCatalog() {
        return RepositoryService.DEFAULT.getCurrentResource().getFeature(INodeStoreCatalog.class);
    }

    protected com.abstratt.kirra.Repository getKirra() throws CoreException {
        return getKirraRepository();
    }

    protected SchemaManagement getKirraSchema() {
        return RepositoryService.DEFAULT.getCurrentResource().getFeature(SchemaManagement.class);
    }

    protected INodeStoreFactory getNodeStoreFactory() {
        return NodeStores.get().getDefaultFactory();
    }

    protected Map<String, Object> getNodeStoreSettings() {
        return new HashMap<String, Object>();
    }

    protected RuntimeObject newInstance(String className) {
        return getRuntimeClass(className).newInstance();
    }

    protected void runAndProcessEvents(Runnable runnable) {
        runnable.run();
        getRuntime().saveContext(false);
    }

    @Override
    protected void runInContext(final Runnable runnable) {
        final RuntimeException[] abort = { null };
        try {
            super.runInContext(new Runnable() {
                @Override
                public void run() {
                    runnable.run();
                    // avoid committing
                    abort[0] = new RuntimeException();
                    throw abort[0];
                }
            });
            TestCase.fail();
        } catch (RuntimeException e) {
            if (abort[0] == null)
                // something else
                throw e;
        }
    }

    protected void runInRuntime(final Runnable r) {
        getRuntime().runSession(new Runtime.Session<Object>() {
            @Override
            public Object run() {
                r.run();
                return null;
            }
        });
    }

    protected void setupRuntime() throws CoreException {
        getKirraRepository().initialize();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    Runtime getRuntime() {
        return Runtime.get();
    }
}
