package com.abstratt.kirra.internal.auth.ldap;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

import com.abstratt.kirra.auth.AuthenticationService;
import com.abstratt.pluginutils.LogUtils;

public class LDAPAuthenticationService implements AuthenticationService {

    private DirContext initialContext;

    public LDAPAuthenticationService() throws NamingException {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, System.getProperty("ldap.providerUrl", "ldap://localhost:10389/dc=cloudfier,dc=com"));
        env.put(Context.SECURITY_AUTHENTICATION, System.getProperty("ldap.authentication", "simple"));
        env.put(Context.SECURITY_PRINCIPAL, System.getProperty("ldap.principal", "cn=admin,dc=cloudfier,dc=com"));
        env.put(Context.SECURITY_CREDENTIALS, System.getProperty("ldap.credentials", "Cloudfier123"));
        this.initialContext = new InitialDirContext(env);
    }

    @Override
    public boolean authenticate(String username, String password) {
        SearchControls controls = new SearchControls(SearchControls.SUBTREE_SCOPE, 0, 5000, new String[0], false, false);
        NamingEnumeration<SearchResult> results = null;
        try {
            results = initialContext.search("", "(mail=" + username + ")", controls);
            if (!results.hasMore())
                return false;
            SearchResult result = results.next();
            String matchName = result.getNameInNamespace();
            results.close();
            Hashtable environment = (Hashtable) initialContext.getEnvironment().clone();
            environment.put(Context.SECURITY_PRINCIPAL, matchName);
            environment.put(Context.SECURITY_CREDENTIALS, password);
            new InitialDirContext(environment).close();
            return true;
        } catch (NamingException e) {
            LogUtils.logWarning(AuthenticationService.class.getPackage().getName(), "Error authenticating " + username, e);
            return false;
        }
    }

    @Override
    public boolean createUser(String username, String password) {
        String encryptedPass = "{SHA}" + new String(Base64.encodeBase64(DigestUtils.sha(password)));

        Attributes matchAttrs = new BasicAttributes(true);
        matchAttrs.put(new BasicAttribute("userpassword", encryptedPass));
        matchAttrs.put(new BasicAttribute("cn", username));
        matchAttrs.put(new BasicAttribute("sn", username));
        matchAttrs.put(new BasicAttribute("mail", username));

        matchAttrs.put(new BasicAttribute("objectclass", "top"));
        matchAttrs.put(new BasicAttribute("objectclass", "person"));
        matchAttrs.put(new BasicAttribute("objectclass", "organizationalPerson"));
        matchAttrs.put(new BasicAttribute("objectclass", "inetorgperson"));
        try {
            this.initialContext.bind("cn=" + username + "," + System.getProperty("ldap.userContext", "ou=users,o=apps"), initialContext,
                    matchAttrs);
            return true;
        } catch (NamingException e) {
            LogUtils.logWarning(AuthenticationService.class.getPackage().getName(), "Error provisioning " + username, e);
            return false;
        }
    }

    @Override
    public boolean resetPassword(String username) {
        throw new UnsupportedOperationException();
    }

}
