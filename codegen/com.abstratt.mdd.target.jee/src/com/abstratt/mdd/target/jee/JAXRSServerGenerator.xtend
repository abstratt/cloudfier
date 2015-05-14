package com.abstratt.mdd.target.jee

import com.abstratt.mdd.target.jse.AbstractGenerator
import com.abstratt.mdd.core.IRepository
import static extension com.abstratt.kirra.mdd.core.KirraHelper.*


class JAXRSServerGenerator extends AbstractGenerator {
    
    new(IRepository repository) {
        super(repository)
    }
    
    def CharSequence generate() {
        '''
package resource.«applicationName»;

import java.io.IOException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.webapp.WebAppContext;

public class RESTServer extends AbstractHandler {

    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
        response.getWriter().println("<h1>Hello World</h1>");
    }

    public static void main(String[] args) throws Exception {
        new RESTServer().run();
    }

    public void run() throws Exception {
        int port = 8888;
        Server server = new Server(port);
        URL webXml = RESTServer.class.getResource("/WEB-INF/web.xml");
        WebAppContext context = new WebAppContext();
        context.setResourceBase(webXml.toURI().resolve("..").toString());
        context.setDescriptor(webXml.toString());
        context.setContextPath("/");
        context.setParentLoaderPriority(true);
        server.setHandler(context);
        server.start();
        server.join();
    }
}
        '''
    }    
}