package com.abstratt.kirra.mdd.rest2;

import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Method;
import org.restlet.ext.crypto.CookieAuthenticator;
import org.restlet.security.SecretVerifier;
import org.restlet.security.Verifier;

import com.abstratt.kirra.mdd.runtime.KirraMDDConstants;
import com.abstratt.pluginutils.LogUtils;

public class KirraAuthenticator extends CookieAuthenticator {
    private final static ThreadLocal<Boolean> IS_OPTIONAL = new ThreadLocal<Boolean>();
    private final static ThreadLocal<Boolean> PROTECTED = new ThreadLocal<Boolean>();
    private final static ThreadLocal<String> WORKSPACE_NAME = new ThreadLocal<String>();

    private final static Verifier VERIFIER = new SecretVerifier() {
        @Override
        public int verify(Request request, Response response) {
            if (!KirraAuthenticator.PROTECTED.get()) {
                ChallengeResponse challengeResponse = new ChallengeResponse(ChallengeScheme.CUSTOM);
                challengeResponse.setIdentifier("guest");
                challengeResponse.setSecret("");
                request.setChallengeResponse(challengeResponse);
                return Verifier.RESULT_VALID;
            }
            return super.verify(request, response);
        }

        @Override
        public int verify(String identifier, char[] secret) {
            if (!KirraAuthenticator.PROTECTED.get())
                return Verifier.RESULT_VALID;
            if (StringUtils.isBlank(identifier)) {
                LogUtils.logInfo(getClass().getPackage().getName(), "User authentication for " + KirraAuthenticator.WORKSPACE_NAME.get()
                        + " failed, identifier not provided", null);
                return Verifier.RESULT_MISSING;
            }
            if (KirraAuthenticator.IS_OPTIONAL.get() && "guest".equalsIgnoreCase(identifier)) {
                LogUtils.logInfo(getClass().getPackage().getName(), "User authentication for " + KirraAuthenticator.WORKSPACE_NAME.get()
                        + " succeeded for: " + identifier, null);
                return Verifier.RESULT_VALID;
            }
            boolean success = Activator.getInstance().getAuthenticationService().authenticate(identifier, new String(secret));
            if (success) {
                LogUtils.logInfo(getClass().getPackage().getName(), "User authentication for " + KirraAuthenticator.WORKSPACE_NAME.get()
                        + " succeeded for: " + identifier, null);
                return Verifier.RESULT_VALID;
            }
            LogUtils.logInfo(getClass().getPackage().getName(), "User authentication for " + KirraAuthenticator.WORKSPACE_NAME.get()
                    + " failed, unknown user identifier: " + identifier, null);
            return Verifier.RESULT_UNKNOWN;
        }
    };

    public KirraAuthenticator() {
        super(null, "Cloudfier App", "u7YzXaKLlsq+KJ1z".getBytes());
        setVerifier(KirraAuthenticator.VERIFIER);
        setMultiAuthenticating(true);
    }

    @Override
    public String getCookieName() {
        return "cloudfier-" + KirraAuthenticator.WORKSPACE_NAME.get() + "-credentials";
    }

    @Override
    public String getLoginPath() {
        return "/" + KirraAuthenticator.WORKSPACE_NAME.get() + "/login";
    }

    @Override
    public String getLogoutPath() {
        return "/" + KirraAuthenticator.WORKSPACE_NAME.get() + "/logout";
    }

    @Override
    public String getRealm() {
        return KirraAuthenticator.WORKSPACE_NAME.get() + "-realm";
    }

    @Override
    public boolean isOptional() {
        return false;
    }

    @Override
    protected int beforeHandle(final Request request, final Response response) {
        String workspace = getWorkspace(request);
        KirraAuthenticator.WORKSPACE_NAME.set(workspace);
        Properties properties = KirraRESTUtils.getProperties(workspace);
        KirraAuthenticator.PROTECTED.set(Boolean.valueOf(properties.getProperty(KirraMDDConstants.LOGIN_REQUIRED, "true")));
        KirraAuthenticator.IS_OPTIONAL.set(KirraAuthenticator.PROTECTED.get()
                || Boolean.valueOf(properties.getProperty(KirraMDDConstants.ALLOW_ANONYMOUS, "true")));
        return super.beforeHandle(request, response);
    }

    @Override
    protected boolean isLoggingIn(Request request, Response response) {
        return isInterceptingLogin() && Method.POST.equals(request.getMethod()) && request.getResourceRef().toString().endsWith("/login");
    }

    @Override
    protected boolean isLoggingOut(Request request, Response response) {
        return isInterceptingLogout() && (Method.GET.equals(request.getMethod()) || Method.POST.equals(request.getMethod()))
                        && request.getResourceRef().toString().endsWith("/logout");
    }

    private String getWorkspace(final Request request) {
        IPath path = new Path(request.getResourceRef().getRemainingPart(true, false)).makeRelative();
        if (path.isEmpty())
            return KirraRESTUtils.getWorkspaceFromProjectPath(request);
        return path.segment(0);
    }
}