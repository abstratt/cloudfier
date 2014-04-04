package com.abstratt.kirra.tests.performance;

import static java.util.Collections.singletonMap;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang.time.StopWatch;
import org.codehaus.jackson.node.ObjectNode;
import org.eclipse.core.runtime.CoreException;

import com.abstratt.kirra.Instance;
import com.abstratt.kirra.Repository;
import com.abstratt.kirra.tests.mdd.runtime.AbstractKirraRestTests;

public class InstancePerformanceTests extends AbstractKirraRestTests {
	
	public InstancePerformanceTests(String name) {
		super(name);
	}
	
	private void buildSimpleModel(int attributeCount) throws IOException,
			CoreException {
		String model = "";
		model += "package mypackage;\n";
		model += "apply kirra;\n";
		model += "import base;\n";
		int classCount = 10;
		for (int j = 0; j < classCount; j++) {
			model += "[Entity] class MyClass" + j + "\n";
			for (int i = 0; i < attributeCount; i++)
				model += "    attribute attr" + i + " : String;\n";
			model += "end;\n";
		}
		model += "end.";

		buildProjectAndLoadRepository(
				singletonMap("test.tuml", model.getBytes()), true);
	}

	private List<String> createInstances(int instanceCount, int attributeCount)
			throws IOException, HttpException, UnsupportedEncodingException,
			CoreException {

		Repository repository = getFeature(Repository.class);
		Instance instance = repository.newInstance("mypackage", "MyClass1");

		for (int i = 0; i < attributeCount; i++)
			instance.setValue("attr" + i, "value" + i);

		List<String> ids = new ArrayList<String>();
		for (int i = 0; i < instanceCount; i++) {
			Instance created = repository.createInstance(instance);
			ids.add(created.getObjectId());
		}
		return ids;
	}

	private void measure(Callable<?> toMeasure) throws Exception {
		System.gc();
		System.gc();
		final int blockSize = 1;
		final int iterations = 10;
		final int OFFSET = 2;
		assertEquals(0, iterations % blockSize);
		final int blockCount = iterations / blockSize;
		assertTrue(blockCount > 2 * OFFSET);

		StopWatch s = new StopWatch();

		List<Long> measurements = new ArrayList<Long>(blockCount);
		for (int i = 0; i < blockCount; i++) {
			s.start();
			for (int j = 0; j < blockSize; j++) {
				toMeasure.call();
			}
			s.stop();
			measurements.add(s.getTime());
			s.reset();
		}
		Collections.sort(measurements);
		long elapsed = 0;
		for (int i = OFFSET; i < measurements.size() - OFFSET; i++)
			elapsed += measurements.get(i);
		System.out.println("Average for " + getName() + ": "
				+ (elapsed / (double) (blockCount - 2 * OFFSET) / blockSize));
	}

	private void testCreateInstances(int instanceCount) throws Exception {
		int attributeCount = 30;
		buildSimpleModel(attributeCount);
		URI sessionURI = getWorkspaceURI();

		GetMethod getTemplateInstance = new GetMethod(sessionURI.resolve(
				"instances/mypackage.MyClass1/_template").toASCIIString());

		ObjectNode template = (ObjectNode) executeJsonMethod(200,
				getTemplateInstance);

		ObjectNode values = (ObjectNode) template.get("values");
		for (int i = 0; i < attributeCount; i++)
			values.put("attr" + i, "value" + i);

		final PostMethod createMethod = new PostMethod(sessionURI.resolve(
				"instances/mypackage.MyClass1/").toString());
		createMethod.setRequestEntity(new StringRequestEntity(template
				.toString(), "application/json", "UTF-8"));

		Callable<?> toMeasure = new Callable<Object>() {
			@Override
			public Object call() throws HttpException, IOException {
				return executeJsonMethod(201, createMethod);
			}
		};

		measure(toMeasure);
	}

	public void testCreateInstances_010() throws Exception {
		testCreateInstances(10);
	}

	public void testCreateInstances_100() throws Exception {
		testCreateInstances(100);
	}

	private void testRetrieveInstances(int instanceCount) throws Exception {
		int attributeCount = 30;
		buildSimpleModel(attributeCount);
		createInstances(instanceCount, attributeCount);
		URI sessionURI = getWorkspaceURI();
		final GetMethod getInstances = new GetMethod(sessionURI.resolve(
				"instances/mypackage.MyClass1/").toASCIIString());

		Callable<?> toMeasure = new Callable<Object>() {
			@Override
			public Object call() throws HttpException, IOException {
				return executeMethod(200, getInstances);
			}
		};

		measure(toMeasure);
	}

	public void testRetrieveInstances_010() throws Exception {
		testRetrieveInstances(10);
	}

	public void testRetrieveInstances_100() throws Exception {
		testRetrieveInstances(100);
	}

	private void testRetrieveOneInstance(int instanceCount) throws Exception {
		int attributeCount = 30;
		buildSimpleModel(attributeCount);
		List<String> instanceIds = createInstances(instanceCount,
				attributeCount);

		URI sessionURI = getWorkspaceURI();

		final GetMethod getOneInstance = new GetMethod(sessionURI.resolve(
				"instances/mypackage.MyClass1/" + instanceIds.get(0))
				.toASCIIString());

		Callable<?> toMeasure = new Callable<Object>() {
			@Override
			public Object call() throws HttpException, IOException {
				return executeMethod(200, getOneInstance);
			}
		};

		measure(toMeasure);
	}

	public void testRetrieveOneInstance_010() throws Exception {
		testRetrieveOneInstance(10);
	}
}
