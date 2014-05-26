package com.abstratt.kirra.mdd.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Package;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

import com.abstratt.kirra.mdd.core.KirraHelper;
import com.abstratt.kirra.mdd.rest.AbstractKirraRepositoryResource;
import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.runtime.Runtime;
import com.abstratt.mdd.core.target.ITargetPlatform;
import com.abstratt.mdd.core.target.ITopLevelMapper;
import com.abstratt.mdd.core.target.MappingException;
import com.abstratt.mdd.core.target.TargetCore;
import com.abstratt.mdd.core.util.MDDUtil;
import com.abstratt.mdd.frontend.web.ResourceUtils;
import com.abstratt.pluginutils.LogUtils;

/**
 * Provides access to an application session.
 */
public abstract class AbstractUIResource<T extends NamedElement> extends AbstractKirraRepositoryResource {
	@Get
	public Representation generateCode() {
//        List<String> namespaces = getRepository().getNamespaces();
//        ResourceUtils.ensure(!namespaces.isEmpty(), "No entity namespaces found", Status.CLIENT_ERROR_NOT_FOUND);
		try {
			IRepository coreRepository = Runtime.getCurrentRuntime().getRepository();
			Properties properties = new Properties();
			properties.put(IRepository.TARGET_ENGINE, "gstring");
			properties.put("mdd.target.ui-js.template", getClass().getResource("/" + getTemplateFileName() ).toString());
			String utilClassName = getClass().getPackage().getName() + ".Util";
			String importUtil = "import static " + utilClassName + ".*;";
			String importKirraUIHelper = "import static " + KirraUIHelper.class.getName() + ".*;";
			properties.put("mdd.target.ui-js.imports", StringUtils.join(Arrays.asList(importKirraUIHelper, importUtil), "\n"));
			properties.put("mdd.target.ui-js.helperUris", getClass().getResource("/" + utilClassName.replace('.',  '/') + ".groovy").toString());
	        ITargetPlatform platform  = TargetCore.getPlatform(properties, "ui-js");
	        ITopLevelMapper<T> mapper = platform.getMapper(MDDUtil.fromEMFToJava(coreRepository.getBaseURI()));
	        
	        String generated = map(coreRepository, mapper);
			StringRepresentation applicationSource = new StringRepresentation(generated, MediaType.APPLICATION_JAVASCRIPT);
			applicationSource.setExpirationDate(new Date(0));
			return applicationSource;
		} catch (MappingException e) {
			LogUtils.log(IStatus.ERROR, getClass().getPackage().getName(), "Generation error", e);
			ResourceUtils.ensure(false, e.getMessage(), Status.SERVER_ERROR_INTERNAL);
			return null;
		}
	}

	protected abstract String map(IRepository coreRepository, ITopLevelMapper<T> mapper);

	public String mapNamespaces(IRepository coreRepository, ITopLevelMapper<Package> mapper) {
		Collection<Package> applicationPackages = KirraHelper.getApplicationPackages(coreRepository.getTopLevelPackages(null));
		String generated = mapper.mapAll(new ArrayList<Package>(applicationPackages));
		return generated;
	}
	
	public String mapElement(IRepository coreRepository, ITopLevelMapper<T> mapper, T element) {
		String generated = mapper.map(element);
		return generated;
	}


	protected abstract String getTemplateFileName();
}