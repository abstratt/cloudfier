package com.abstratt.kirra.mdd.rest;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.restlet.Application;
import org.restlet.Context;
import org.restlet.ext.jaxrs.JaxRsApplication;
import org.restlet.ext.servlet.ServerServlet;

import com.abstratt.kirra.rest.resources.KirraJaxRsApplication;

public class KirraRESTServlet extends ServerServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected Application createApplication(Context parentContext) {
        JaxRsApplication jaxRsApplication = new JaxRsApplication(parentContext);
        KirraJaxRsApplication jaxApplication = new KirraJaxRsApplication();
        jaxRsApplication.add(jaxApplication);
        return jaxRsApplication;
    }
    
    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        super.service(request, response);
    }
}
