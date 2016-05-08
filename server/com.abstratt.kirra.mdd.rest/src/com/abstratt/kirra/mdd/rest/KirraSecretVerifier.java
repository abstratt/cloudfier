package com.abstratt.kirra.mdd.rest;

import org.apache.commons.lang.StringUtils;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.security.SecretVerifier;
import org.restlet.security.Verifier;

import com.abstratt.pluginutils.LogUtils;

public class KirraSecretVerifier extends SecretVerifier implements KirraAuthenticationContext {
//    @Override
//    public int verify(Request request, Response response) {
//        if (!PROTECTED.get()) {
//            ChallengeResponse challengeResponse = new ChallengeResponse(ChallengeScheme.CUSTOM);
//            challengeResponse.setIdentifier("guest");
//            challengeResponse.setSecret("");
//            request.setChallengeResponse(challengeResponse);
//            return Verifier.RESULT_VALID;
//        }
//        return super.verify(request, response);
//    }

    @Override
    public int verify(String identifier, char[] secret) {
        if (StringUtils.isBlank(identifier)) {
            LogUtils.logInfo(getClass().getPackage().getName(), "User authentication for " + WORKSPACE_NAME.get()
                    + " failed, identifier not provided", null);
            return Verifier.RESULT_MISSING;
        }
        if (ALLOWS_ANONYMOUS.get() && "guest".equalsIgnoreCase(identifier)) {
            LogUtils.logInfo(getClass().getPackage().getName(), "User authentication for " + WORKSPACE_NAME.get()
                    + " succeeded for: " + identifier, null);
            return Verifier.RESULT_VALID;
        }
        boolean success = Activator.getInstance().getAuthenticationService().authenticate(identifier, new String(secret));
        if (success) {
            LogUtils.logInfo(getClass().getPackage().getName(), "User authentication for " + WORKSPACE_NAME.get()
                    + " succeeded for: " + identifier, null);
            return Verifier.RESULT_VALID;
        }
        LogUtils.logInfo(getClass().getPackage().getName(), "User authentication for " + WORKSPACE_NAME.get()
                + " failed, unknown user identifier: " + identifier, null);
        return Verifier.RESULT_UNKNOWN;
    }
    
    @Override
    	protected String getIdentifier(Request request, Response response) {
    		// TODO Auto-generated method stub
    		return super.getIdentifier(request, response);
    	}
}