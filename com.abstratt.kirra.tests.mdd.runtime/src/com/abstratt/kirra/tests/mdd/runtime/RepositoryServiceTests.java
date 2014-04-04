package com.abstratt.kirra.tests.mdd.runtime;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;

import com.abstratt.kirra.Repository;
import com.abstratt.kirra.SchemaManagement;
import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.runtime.Runtime;

import com.abstratt.mdd.core.RepositoryService;
import com.abstratt.mdd.core.tests.harness.AbstractRepositoryBuildingTests;
import com.abstratt.nodestore.INodeStoreCatalog;
import com.abstratt.pluginutils.ISharedContextRunnable;
import com.abstratt.resman.Resource;

public class RepositoryServiceTests extends AbstractRepositoryBuildingTests {

	public RepositoryServiceTests(String name) {
		super(name);
	}
	
	@Override
	protected void runTest() throws Throwable {
		originalRunTest();
	}
	
	public void testInSession() throws CoreException {
		assertFalse(RepositoryService.DEFAULT.isInSession());
		boolean inSession = RepositoryService.DEFAULT.runInRepository(getRepositoryURI(), new ISharedContextRunnable<IRepository, Boolean>() {
			@Override
			public Boolean runInContext(IRepository context) {
				return RepositoryService.DEFAULT.isInSession();
			}
		});
		assertTrue(inSession);
	}
	
	public void testFeatures() throws CoreException {
		RepositoryService.DEFAULT.runInRepository(getRepositoryURI(), new ISharedContextRunnable<IRepository, Object>() {
			@Override
			public Object runInContext(IRepository context) {
				Resource<?> current = RepositoryService.DEFAULT.getCurrentResource();
				current.getFeature(IRepository.class);
				current.getFeature(Runtime.class);
				current.getFeature(SchemaManagement.class);
				current.getFeature(Repository.class);
				current.getFeature(INodeStoreCatalog.class);
				return null;
			}
		});
	}
	public static Test suite() {
		return new TestSuite(RepositoryServiceTests.class);
	}
}
