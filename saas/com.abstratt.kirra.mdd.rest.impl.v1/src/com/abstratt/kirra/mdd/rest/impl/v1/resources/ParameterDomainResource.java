package com.abstratt.kirra.mdd.rest.impl.v1.resources;

import java.util.List;

import org.restlet.data.Status;

import com.abstratt.kirra.Entity;
import com.abstratt.kirra.Instance;
import com.abstratt.kirra.Operation;
import com.abstratt.kirra.Parameter;
import com.abstratt.mdd.frontend.web.ResourceUtils;

/**
 * For a given instance, action and parameter, determines which entity instances
 * are valid choices.
 */
public class ParameterDomainResource extends AbstractInstanceListResource {

    @Override
    protected List<Instance> findInstances(String entityNamespace, String entityName) {
        Entity entity = getRepository().getEntity(getEntityNamespace(), getEntityName());
        ResourceUtils.ensure(entity != null, "Entity not found: " + getEntityName(), Status.CLIENT_ERROR_NOT_FOUND);
        String actionName = (String) getRequestAttributes().get("actionName");
        Operation action = entity.getOperation(actionName);
        ResourceUtils.ensure(action != null, "Action not found: " + actionName, Status.CLIENT_ERROR_NOT_FOUND);
        ResourceUtils
                .ensure(action.isInstanceOperation(), "Action is not an instance action: " + actionName, Status.CLIENT_ERROR_NOT_FOUND);

        String parameterName = (String) getRequestAttributes().get("parameterName");
        Parameter parameter = action.getParameter(parameterName);
        ResourceUtils.ensure(parameter != null, "Parameter not found: " + parameterName, Status.CLIENT_ERROR_NOT_FOUND);

        String objectId = (String) getRequestAttributes().get("objectId");
        return getRepository().getParameterDomain(entity, objectId, action, parameter);
    }
}
