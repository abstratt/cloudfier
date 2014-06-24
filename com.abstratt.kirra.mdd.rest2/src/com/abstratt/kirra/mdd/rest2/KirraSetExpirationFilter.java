package com.abstratt.kirra.mdd.rest2;

import java.util.Date;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.representation.Representation;
import org.restlet.routing.Filter;

public class KirraSetExpirationFilter extends Filter {
	public KirraSetExpirationFilter(Restlet wrapped) {
		setNext(wrapped);
	}

	@Override
	protected int doHandle(final Request request, final Response response) {
		int result = super.doHandle(request, response);
		// avoid caching
		final Representation entity = response.getEntity();
		if (entity != null)
			entity.setExpirationDate(new Date(0));
		return result;
	}

}
