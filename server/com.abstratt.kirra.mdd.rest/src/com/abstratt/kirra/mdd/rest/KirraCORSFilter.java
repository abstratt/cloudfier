package com.abstratt.kirra.mdd.rest;

import java.util.Map;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Header;
import org.restlet.routing.Filter;
import org.restlet.util.Series;

public class KirraCORSFilter extends Filter {
    public KirraCORSFilter(Restlet wrapped) {
        setNext(wrapped);
    }

    @Override
    protected int doHandle(final Request request, final Response response) {
        int result = super.doHandle(request, response);
        
        Map<String, Object> requestAttributes = request.getAttributes();
        Map<String, Object> responseAttributes = response.getAttributes();
        Series<Header> requestHeaders = (Series<Header>)requestAttributes.computeIfAbsent("org.restlet.http.headers", key -> new Series<Header>(Header.class));
        Series<Header> responseHeaders = (Series<Header>)responseAttributes.computeIfAbsent("org.restlet.http.headers", key -> new Series<Header>(Header.class)); 
        //TODO fix me
        responseHeaders.add("Access-Control-Allow-Origin", requestHeaders.getFirstValue("Origin"));
        responseHeaders.add("Access-Control-Allow-Credentials", "true");
        responseHeaders.add("Access-Control-Allow-Methods", "HEAD, GET, PUT, POST, DELETE, OPTIONS, TRACE");
        responseHeaders.add("Access-Control-Allow-Headers", "Content-Type, X-Requested-With");
        return result;
    }
}
