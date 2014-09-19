package com.abstratt.kirra.internal.auth.ldap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import com.abstratt.kirra.auth.AuthenticationService;
import com.abstratt.kirra.auth.EmailService;
import com.abstratt.pluginutils.LogUtils;

public class MandrillEmailService implements EmailService {

    @Override
    public boolean send(String addresseeEmail, String addresseeName, String sender, String subject, String content) {
        Map<String, Object> request = new HashMap<String, Object>();
        request.put("api", System.getProperty("KIRRA_MANDRILL_API_KEY"));

        Map<String, Object> message = new HashMap<String, Object>();
        request.put("message", message);
        message.put("html", content);
        message.put("subject", subject);
        message.put("from_email", System.getProperty("KIRRA_FROM_EMAIL"));
        message.put("from_name", System.getProperty("KIRRA_FROM_NAME"));

        Map<String, String> to = new HashMap<String, String>();
        message.put("to", to);
        to.put("email", addresseeEmail);
        if (addresseeName != null)
            to.put("name", addresseeName);

        try {
            HttpClient client = new HttpClient();
            PostMethod send = new PostMethod("https://mandrillapp.com/api/1.0/messages/send.json");

            send.setRequestEntity(new StringRequestEntity(toJSON(request), "application/json", "UTF-8"));
            int status = client.executeMethod(send);
            LogUtils.logInfo(AuthenticationService.class.getPackage().getName(), send.getResponseBodyAsString(), null);
            return status == 200;
        } catch (IOException e) {
            LogUtils.logWarning(AuthenticationService.class.getPackage().getName(), "Error sending email to " + addresseeEmail, e);
        }
        return false;
    }

    private String toJSON(Map<String, Object> object) {
        StringBuilder result = new StringBuilder();
        for (Map.Entry pair : object.entrySet()) {

        }
        return result.toString();
    }

}
