package com.abstratt.mdd.core.tests.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

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

	protected ExternalService externalService;

	public AbstractRuntimeTests(String name) {
		super(name);
	}

	protected com.abstratt.kirra.Repository getKirra() throws CoreException {
		return getKirraRepository();
	}

	@Override
	protected void compilationCompleted() throws CoreException {
		setupRuntime();
	}

	protected void setupRuntime() throws CoreException {
		getKirraRepository().initialize();
	}

	public com.abstratt.kirra.Repository getKirraRepository() {
		return RepositoryService.DEFAULT.getCurrentResource().getFeature(com.abstratt.kirra.Repository.class);
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

	interface FallibleRunnable<T extends Throwable> {
		public void run() throws T;
	}
	
	@Override
	protected void runInContext(final Runnable runnable) {
		final RuntimeException[] abort = {null};
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
			fail();
		} catch (RuntimeException e) {
			if (abort[0] == null)
				// something else
				throw e;
		}
	}
	
	protected void runInRuntime(final Runnable r) {
		getRuntime().runSession(new Runtime.Session<Object>() {
			public Object run() {
    			r.run();
    			return null;
			}
		});
	}
	
    protected void runAndProcessEvents(Runnable runnable) {
		runnable.run();
		getRuntime().saveContext(false);
	}

	public RuntimeClass getRuntimeClass(String className) {
		return getRuntimeClass(getRuntime(), className);
	}
	
	public RuntimeClass getRuntimeClass(Runtime runtime, String className) {
		Classifier classInstance = (Classifier) getRepository().findNamedElement(className, IRepository.PACKAGE.getClassifier(), null);
		assertNotNull(className, classInstance);
		return getRuntime().getRuntimeClass(classInstance);
	}


	protected RuntimeObject newInstance(String className) {
		return getRuntimeClass(className).newInstance();
	}

	public BasicType readAttribute(RuntimeObject target, String name) {
		Classifier classInstance = target.getRuntimeClass().getModelClassifier();
		Property attribute = classInstance.getAttribute(name, null);
		assertNotNull(classInstance.getQualifiedName() + NamedElement.SEPARATOR + name, attribute);
		return target.getValue(attribute);
	}
	
	/**
	 * This convenience method does NOT do parameter-based matching.
	 */
	public Object runOperation(BasicType target, String operationName, Object... arguments) {
		Classifier classifier = (Classifier) getRepository().findNamedElement(target.getClassifierName(), IRepository.PACKAGE.getClassifier(), null);
		assertNotNull(target.getClassifierName(), classifier);
		Operation operation = FeatureUtils.findOperation(getRepository(), classifier, operationName, null);
		assertNotNull(operationName, operation);
		return getRuntime().runOperation(null, target, operation, arguments);
	}
	
	Runtime getRuntime() {
		return Runtime.get();
	}

	public void sendSignal(RuntimeObject target, RuntimeObject arguments) {
		Classifier classifier = (Classifier) getRepository().findNamedElement(target.getClassifierName(), IRepository.PACKAGE.getClassifier(), null);
		assertNotNull(target.getClassifierName(), classifier);
		getRuntime().sendSignal(target, arguments);
	}
	
	public void sendSignal(BasicType target, String signalName, Map<String, BasicType> arguments) {
		RuntimeObject signalInstance = getRuntimeClass(signalName).newInstance(false, false);
		for (Entry<String, BasicType> entry : arguments.entrySet())
			writeAttribute(signalInstance, entry.getKey(), entry.getValue());
		getRuntime().sendSignal(target, signalInstance);
	}

	/**
	 * 	 This convenience method does NOT do parameter-based matching.
	 */
	public Object runStaticOperation(String className, String operationName, Object... arguments) {
		Class classifier = (Class) getRepository().findNamedElement(className, IRepository.PACKAGE.getClass_(), null);
		assertNotNull("Not found: " + className, classifier);
		Operation operation = FeatureUtils.findOperation(getRepository(), classifier, operationName, null);
		assertNotNull("Operation not found: " + classifier.getQualifiedName() + NamedElement.SEPARATOR + operationName, operation);
		return getRuntime().runOperation(null, null, operation, arguments);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void writeAttribute(final RuntimeObject target, String name, final BasicType value) {
		Classifier classInstance = target.getRuntimeClass().getModelClassifier();
		final Property property = classInstance.getAttribute(name, null);
		assertNotNull(classInstance.getQualifiedName() + NamedElement.SEPARATOR + name, property);
		getRuntime().runSession(new Runtime.Session<Object>() {
			@Override
			public Object run() {
				target.setValue(property, value);
				return null;
			}
		});
	}
	
	@Override
	protected Properties createDefaultSettings() {
		Properties defaultSettings = super.createDefaultSettings();
		// so the kirra profile is available as a system package (no need to load)
		defaultSettings.setProperty(IRepository.EXTEND_BASE_OBJECT, Boolean.TRUE.toString());
		defaultSettings.setProperty("mdd.enableKirra", Boolean.TRUE.toString());
		defaultSettings.setProperty("mdd.modelWeaver", "kirraWeaver");
		defaultSettings.setProperty("mdd.runtime.nodestore", "jdbc");
		
		return defaultSettings;
	}

	protected INodeStoreCatalog getCatalog() {
		return RepositoryService.DEFAULT.getCurrentResource().getFeature(INodeStoreCatalog.class);
	}
}
