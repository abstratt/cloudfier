package com.abstratt.kirra.mdd.rest;

import java.util.Map;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.engine.header.Header;
import org.restlet.routing.Filter;
import org.restlet.util.Series;

public class KirraCORSFilter extends Filter {
    public KirraCORSFilter(Restlet wrapped) {
        setNext(wrapped);
    }

    @Override
    protected int doHandle(final Request request, final Response response) {
        int result = super.doHandle(request, response);
        
        Map<String, Object> responseAttributes = response.getAttributes(); 
        Series<Header> headers = (Series<Header>)responseAttributes.get("org.restlet.http.headers"); 
        if (headers == null) { 
            headers = new Series<Header>(Header.class); 
            responseAttributes.put("org.restlet.http.headers", headers); 
        } 
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "HEAD, GET, PUT, POST, DELETE, OPTIONS, TRACE");
        headers.add("Access-Control-Allow-Headers", "Content-Type");
        return result;
    }
}
