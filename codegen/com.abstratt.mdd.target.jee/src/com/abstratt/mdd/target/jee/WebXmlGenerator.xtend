package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.target.jse.AbstractGenerator
import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import com.abstratt.kirra.mdd.core.KirraHelper

class WebXmlGenerator extends AbstractGenerator {
    
    new(IRepository repository) {
        super(repository)
    }
    
    def CharSequence generateWebXml() {
    	val roles = entities.filter[concrete]
        val applicationName = KirraHelper.getApplicationName(repository)
    	'''
    	<?xml version="1.0" encoding="UTF-8"?>
    	<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    		xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_3_1.xsd"
    		version="3.1">
    	
    		<context-param>
    			<param-name>resteasy.scan</param-name>
    			<param-value>true</param-value>
    		</context-param>
    		<context-param>
    			<param-name>resteasy.role.based.security</param-name>
    			<param-value>«if (roles.empty) 'false' else 'true'»</param-value>
    		</context-param>
    		<context-param>
    			<param-name>resteasy.servlet.mapping.prefix</param-name>
    			<param-value>/</param-value>
    		</context-param>
    		<context-param>
    			<param-name>javax.ws.rs.Application</param-name>
    			<param-value>resource.«applicationName».Application</param-value>
    		</context-param>
    	
    		<listener>
    			<listener-class>org.jboss.resteasy.plugins.server.servlet.ResteasyBootstrap</listener-class>
    		</listener>
    	
    		<servlet>
    			<servlet-name>Resteasy</servlet-name>
    			<servlet-class>org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher</servlet-class>
    			<load-on-startup>1</load-on-startup>
    		</servlet>
    	
    		<servlet-mapping>
    			<servlet-name>Resteasy</servlet-name>
    			<url-pattern>/*</url-pattern>
    		</servlet-mapping>

            «IF !roles.empty»    

    		<security-constraint>
    			<web-resource-collection>
    				<web-resource-name>Resteasy</web-resource-name>
    				<url-pattern>/</url-pattern>
    				«#["HEAD", "GET", "PUT", "POST", "DELETE"].map['''
					<http-method>«it»</http-method>
					'''].join()»
    			</web-resource-collection>
    			<auth-constraint>
    				«FOR role : roles»
    				<role-name>«role.name»</role-name>
    				«ENDFOR»
    			</auth-constraint>
    		</security-constraint>
    	
    		<login-config>
    			<auth-method>BASIC</auth-method>
    			<realm-name>«applicationName»-realm</realm-name>
    		</login-config>
    	
    		«FOR role : roles»
    		<security-role>
    			<role-name>«role.name»</role-name>
    		</security-role>
    		«ENDFOR»

    		«ENDIF»
    	
    	</web-app>
    	'''
    }
	
}