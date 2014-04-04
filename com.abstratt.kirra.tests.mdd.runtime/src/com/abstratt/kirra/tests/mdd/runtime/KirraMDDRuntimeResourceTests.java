package com.abstratt.kirra.tests.mdd.runtime;

import org.eclipse.core.runtime.CoreException;

import com.abstratt.kirra.Repository;
import com.abstratt.kirra.mdd.core.KirraHelper;
import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.RepositoryService;
import com.abstratt.mdd.core.tests.harness.AbstractRepositoryBuildingTests;
import com.abstratt.pluginutils.ISharedContextRunnable;
import com.abstratt.resman.Resource;
import com.abstratt.mdd.core.runtime.Runtime;

public class KirraMDDRuntimeResourceTests extends AbstractRepositoryBuildingTests {
	
	public KirraMDDRuntimeResourceTests(String name) {
		super(name);
	}
	
    public void testFeatures() throws CoreException {
    	assertFalse(RepositoryService.DEFAULT.isInSession());
    	RepositoryService.DEFAULT.runInRepository(getRepositoryURI(), new ISharedContextRunnable<IRepository, Object>() {
    		public Object runInContext(IRepository context) {
    			assertTrue(RepositoryService.DEFAULT.isInSession());
    			Resource<?> currentResource = RepositoryService.DEFAULT.getCurrentResource();
				assertNotNull(currentResource);
    			assertNotNull(currentResource.getFeature(IRepository.class));
    			assertNotNull(currentResource.getFeature(KirraHelper.Metadata.class));
    			assertNotNull(currentResource.getFeature(Repository.class));
    			assertNotNull(currentResource.getFeature(Runtime.class));
    			return null;
    		}
		});
    }
    
    @Override
    protected void runTest() throws Throwable {
    	originalRunTest();
    }
    
}
