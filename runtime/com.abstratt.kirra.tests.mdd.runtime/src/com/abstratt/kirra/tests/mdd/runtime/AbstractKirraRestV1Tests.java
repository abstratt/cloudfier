package com.abstratt.kirra.tests.mdd.runtime;

import java.io.IOException;
import java.net.URI;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

public abstract class AbstractKirraRestV1Tests extends AbstractKirraRestTests {

	public AbstractKirraRestV1Tests(String name) {
		super(name);
	}

    protected void signUp(String username, String password, int expected) throws HttpException, IOException {
        URI sessionURI = getWorkspaceBaseURI();
        URI sigupUri = sessionURI.resolve(com.abstratt.mdd.frontend.web.Paths.SIGNUP);
		PostMethod signUpMethod = new PostMethod(sigupUri.toString());
        signUpMethod.setRequestEntity(new StringRequestEntity("login=" + username + "&password=" + password,
                "application/x-www-form-urlencoded", "UTF-8"));
        restHelper.executeMethod(expected, signUpMethod);
    }
    
    protected void signUp(String username, String password) throws HttpException, IOException {
        signUp(username, password, 204);
    }

    protected void login(String username, String password) throws HttpException, IOException {
        URI sessionURI = getWorkspaceBaseURI();
        PostMethod loginMethod = new PostMethod(sessionURI.resolve(com.abstratt.mdd.frontend.web.Paths.LOGIN).toString());
        loginMethod.setRequestEntity(new StringRequestEntity("login=" + username + "&password=" + password,
                "application/x-www-form-urlencoded", "UTF-8"));
        restHelper.executeMethod(204, loginMethod);
    }

    protected void logout() throws HttpException, IOException {
        URI sessionURI = getWorkspaceBaseURI();
        PostMethod logoutMethod = new PostMethod(sessionURI.resolve(com.abstratt.mdd.frontend.web.Paths.LOGOUT).toString());
        restHelper.executeMethod(200, logoutMethod);
    }
    

}
