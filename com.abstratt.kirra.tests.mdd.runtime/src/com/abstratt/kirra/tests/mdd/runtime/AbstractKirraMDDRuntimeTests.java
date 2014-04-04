package com.abstratt.kirra.tests.mdd.runtime;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import com.abstratt.kirra.BehaviorScope;
import com.abstratt.kirra.Instance;
import com.abstratt.kirra.KirraException;
import com.abstratt.kirra.NamedElement;
import com.abstratt.kirra.Operation;
import com.abstratt.mdd.core.tests.runtime.AbstractRuntimeTests;

public class AbstractKirraMDDRuntimeTests extends AbstractRuntimeTests {

	public AbstractKirraMDDRuntimeTests(String name) {
		super(name);
	}
	
//	@Override
//	protected void runTest() throws Throwable {
//		final Throwable[] exception = {null};
//		getKirra().getRuntime().runInRuntime(new RuntimeRunnable() {
//			public void run() {
//				try {
//					AbstractKirraMDDRuntimeTests.super.runTest();
//				} catch (Throwable e) {
//					exception[0] = e;
//				}
//			}
//		});
//		if (exception[0] != null)
//			throw exception[0];
//	}

	protected Instance findById(List<Instance> all, String objectId) {
		for (Instance instance : all)
			if (objectId.equals(instance.getObjectId()))
				return instance;
		return null;
	}

	protected void sortNamedElements(List<? extends NamedElement> entities) {
		Collections.sort(entities, new Comparator<NamedElement>() {
			public int compare(NamedElement o1, NamedElement o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
	}
	
	protected <NE extends NamedElement> NE findNamedElement(List<NE> elements, String name) {
		for (NE ne : elements)
			if (ne.getName().equals(name))
				return ne;
		return null;
	}
	
	protected void sortInstances(List<Instance> instances) {
		Collections.sort(instances, new Comparator<Instance>() {
			public int compare(Instance o1, Instance o2) {
				return o1.getObjectId().compareTo(o2.getObjectId());
			}
		});
	}
	
	protected List<?> executeKirraOperation(String namespace, String entityOrService,
			String objectId, String operationName, List<?> arguments) throws CoreException {
		BehaviorScope targetType;
		try {
			targetType = getKirra().getEntity(namespace, entityOrService);
		} catch (KirraException e) {
			targetType = getKirra().getService(namespace, entityOrService);
		}
		Operation operation = targetType.getOperation(operationName);
		assertNotNull("No " + operationName + " in " + targetType.getTypeRef(), operation);
		return getKirra().executeOperation(operation, objectId, arguments);
	}
}
