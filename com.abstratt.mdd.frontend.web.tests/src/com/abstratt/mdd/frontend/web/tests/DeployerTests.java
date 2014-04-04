package com.abstratt.mdd.frontend.web.tests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import com.abstratt.mdd.core.tests.harness.AbstractRepositoryBuildingTests;
import com.abstratt.mdd.core.tests.harness.FixtureHelper;
import com.abstratt.mdd.frontend.web.BuildDirectoryUtils;

public class DeployerTests extends AbstractRepositoryBuildingTests {
	
	public DeployerTests(String name) {
		super(name);
	}

	protected RestHelper restHelper;
	
	@Override
	protected void tearDown() throws Exception {
		restHelper.dispose();
		super.tearDown();
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		restHelper = new RestHelper(getName());
	}
	
	public void testDeploy() throws HttpException, IOException, CoreException {
		String source = "";
		source += "package foo;\n";
		source += "class Class1\n";
		source += "end;\n";
		source += "end.\n";
		
		IFileStore projectRoot = BuildDirectoryUtils.getSourcePath(new Path("/test/" + getName()));
		
		saveSettings(projectRoot, createDefaultSettings());
		new FixtureHelper().createProject(projectRoot, Collections.singletonMap("foo.tuml", source));
		
		String deployerUri = restHelper.getDeployerUri().toString();
		PostMethod deploy = new PostMethod(deployerUri);
		restHelper.executeMethod(200, deploy);
		
		GetMethod getInfo = new GetMethod(deployerUri);
		restHelper.executeMethod(200, getInfo);
	}
	
	public void testMultithreadedDeploy() throws Exception {
		String source = "";
		source += "package foo;\n";
		source += "class Class1\n";
		source += "end;\n";
		source += "end.\n";
		
		IFileStore projectRoot = BuildDirectoryUtils.getSourcePath(new Path("/test/" + getName()));
		
		saveSettings(projectRoot, createDefaultSettings());
		new FixtureHelper().createProject(projectRoot, Collections.singletonMap("foo.tuml", source));
		
		int threadCount = 10;
		final CyclicBarrier barrier = new CyclicBarrier(threadCount + 1);
		final String deployerUri = restHelper.getDeployerUri().toString();
		final AtomicBoolean wait = new AtomicBoolean(false);
		Callable<Boolean> deployer = new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				if (wait.get()) 
					barrier.await();
				PostMethod deploy = new PostMethod(deployerUri);
				restHelper.executeMethod(new HttpClient(), 200, deploy);
				return true;
			}
		};
		
		assertTrue(deployer.call());
		wait.set(true);
		
		ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(10);
		ScheduledFuture<Boolean> scheduled;
		List<ScheduledFuture<Boolean>> allScheduled = new ArrayList<ScheduledFuture<Boolean>>();
		for (int i = 0; i < threadCount; i++) {
			scheduled = executor.schedule(deployer, 0, TimeUnit.SECONDS);
			allScheduled.add(scheduled);
		}
		barrier.await();
		for (ScheduledFuture<Boolean> scheduledFuture : allScheduled)
			assertTrue(scheduledFuture.get());
	}

	@Override
	protected IFileStore computeBaseDir() {
		String deployDir = BuildDirectoryUtils.getBaseDeployDirectory().getAbsolutePath();
		return EFS.getLocalFileSystem().getStore(new Path(deployDir ));
	}
}
