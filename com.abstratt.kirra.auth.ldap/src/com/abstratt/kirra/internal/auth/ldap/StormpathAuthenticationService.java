package com.abstratt.kirra.internal.auth.ldap;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import com.abstratt.kirra.auth.AuthenticationService;
import com.abstratt.pluginutils.LogUtils;

public class StormpathAuthenticationService implements AuthenticationService {

    private class CachedAuthentication {
        String username;
        String password;
        long timestamp;

        public CachedAuthentication(String username, String password) {
            this.username = username;
            this.password = password;
            touch();
        }

        public boolean confirm(String username, String password) {
            if (matches(username, password) && !isExpired()) {
                touch();
                return true;
            }
            return false;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - this.timestamp > StormpathAuthenticationService.expiryPeriod;
        }

        public boolean matches(String username, String password) {
            return this.username.equals(username) && this.password.equals(password);
        }

        private long touch() {
            return timestamp = System.currentTimeMillis();
        }
    }

    private Map<String, CachedAuthentication> cachedAuthentications = new LRUMap(1000);

    private static final long expiryPeriod = 5 * 60 * 1000;

    @Override
    public boolean authenticate(String username, String password) {
        if (wasAuthenticated(username, password))
            return true;
        Map<String, Object> request = new HashMap<String, Object>();
        String encodedCredentials;
        try {
            encodedCredentials = new String(Base64.encodeBase64((username + ":" + password + "\n").getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            LogUtils.logWarning(getClass().getPackage().getName(), "Error authenticating user " + username, e);
            return false;
        }
        request.put("type", "basic");
        request.put("value", encodedCredentials);
        boolean result = sendRequest("applications/2aGBdXfJOnXHCKSMarwcZL/loginAttempts", request, 200);
        if (result) {
            rememberAuthentication(username, password);
            return true;
        }
        return false;
    }

    @Override
    public boolean createUser(String username, String password) {
        forgetAuthentication(username);
        Map<String, Object> request = new HashMap<String, Object>();
        request.put("username", username);
        request.put("email", username);
        request.put("givenName", "Cloudfier user");
        request.put("surname", username);
        request.put("email", username);
        request.put("password", password);
        return sendRequest("directories/2aFq1HCOdFBEJiLgk2vRyR/accounts", request, 201);
    }

    @Override
    public boolean resetPassword(String username) {
        Map<String, Object> request = new HashMap<String, Object>();
        request.put("email", username);
        return sendRequest("applications/2aGBdXfJOnXHCKSMarwcZL/passwordResetTokens", request, 200);
    }

    private synchronized void forgetAuthentication(String username) {
        cachedAuthentications.remove(username);
    }

    private synchronized void rememberAuthentication(String username, String password) {
        cachedAuthentications.put(username, new CachedAuthentication(username, password));
    }

    private boolean sendRequest(String path, Map<String, Object> request, int expected) {
        try {
            HttpClient client = new HttpClient();
            client.getState().setCredentials(new AuthScope("api.stormpath.com", 443),
                    new UsernamePasswordCredentials("1P119F48A43RCO4XQ519PJCFH", "epMuyOKd2Jz4nvFsohoX+5RKql+xmsKQXFUqGQ7eupY"));
            PostMethod send = new PostMethod("https://api.stormpath.com/v1/" + path);
            String requestAsJson = toJSON(request);
            System.out.println(requestAsJson);
            send.setRequestEntity(new StringRequestEntity(requestAsJson, "application/json", "UTF-8"));
            int status = client.executeMethod(send);
            LogUtils.logInfo(AuthenticationService.class.getPackage().getName(),
                    "Status: " + status + " - " + send.getResponseBodyAsString(), null);
            return status == expected;
        } catch (IOException e) {
            LogUtils.logWarning(getClass().getPackage().getName(), "Error invoking " + path, e);
        }
        return false;

    }

    private String toJSON(Map<String, Object> object) {
        StringBuilder result = new StringBuilder("{");
        for (Map.Entry pair : object.entrySet()) {
            result.append("\"" + pair.getKey() + "\": ");
            result.append(pair.getValue() instanceof String ? "\"" + pair.getValue() + "\"" : pair.getValue());
            result.append(",");
        }
        if (!object.isEmpty())
            result.deleteCharAt(result.length() - 1);
        result.append("}");
        return result.toString();
    }

    private synchronized boolean wasAuthenticated(String username, String password) {
        CachedAuthentication authentication = cachedAuthentications.get(username);
        if (authentication == null)
            return false;
        if (authentication.confirm(username, password))
            return true;
        return false;
    }
}
