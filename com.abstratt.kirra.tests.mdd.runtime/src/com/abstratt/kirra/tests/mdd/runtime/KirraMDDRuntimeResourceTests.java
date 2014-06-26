package com.abstratt.kirra.tests.mdd.runtime;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;

import com.abstratt.kirra.Repository;
import com.abstratt.kirra.mdd.core.KirraHelper;
import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.RepositoryService;
import com.abstratt.mdd.core.runtime.Runtime;
import com.abstratt.mdd.core.tests.harness.AbstractRepositoryBuildingTests;
import com.abstratt.pluginutils.ISharedContextRunnable;
import com.abstratt.resman.Resource;

public class KirraMDDRuntimeResourceTests extends AbstractRepositoryBuildingTests {

    public KirraMDDRuntimeResourceTests(String name) {
        super(name);
    }

    public void testFeatures() throws CoreException {
        TestCase.assertFalse(RepositoryService.DEFAULT.isInSession());
        RepositoryService.DEFAULT.runInRepository(getRepositoryURI(), new ISharedContextRunnable<IRepository, Object>() {
            @Override
            public Object runInContext(IRepository context) {
                TestCase.assertTrue(RepositoryService.DEFAULT.isInSession());
                Resource<?> currentResource = RepositoryService.DEFAULT.getCurrentResource();
                TestCase.assertNotNull(currentResource);
                TestCase.assertNotNull(currentResource.getFeature(IRepository.class));
                TestCase.assertNotNull(currentResource.getFeature(KirraHelper.Metadata.class));
                TestCase.assertNotNull(currentResource.getFeature(Repository.class));
                TestCase.assertNotNull(currentResource.getFeature(Runtime.class));
                return null;
            }
        });
    }

    @Override
    protected void runTest() throws Throwable {
        originalRunTest();
    }

}
