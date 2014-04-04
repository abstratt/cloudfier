package com.abstratt.kirra.mdd.rest;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;

import com.abstratt.kirra.Entity;
import com.abstratt.kirra.Instance;
import com.abstratt.mdd.frontend.web.ResourceUtils;

public abstract class AbstractInstanceListResource extends AbstractKirraRepositoryResource {

	@Get
	public final Representation getInstances() {
		Entity targetEntity = getTargetEntity();
		ResourceUtils.ensure(targetEntity != null, null, Status.CLIENT_ERROR_NOT_FOUND);
			
		List<Instance> instances = sort(findInstances(targetEntity.getEntityNamespace(), targetEntity.getName()));
		return buildInstanceList(targetEntity, instances);
	}

	protected abstract List<Instance> findInstances(String entityNamespace, String entityName);

	protected List<Instance> sort(List<Instance> instances) {
		Collections.sort(instances, new Comparator<Instance>() {
			public int compare(Instance o1, Instance o2) {
				return o1.getObjectId().compareTo(o2.getObjectId());
			}
		});
		return instances;
	}
}
