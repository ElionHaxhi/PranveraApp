package com.pranveraapp.common.util;

import com.pranveraapp.common.web.PranveraAppRequestContext;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by elion on 26/02/16.
 */
public class ELRequestUtils {

    private static String OK_TO_USE_SESSION = "blOkToUseSession";


    public static boolean isOKtoUseSession(WebRequest request) {
        Boolean useSessionForRequestProcessing = (Boolean) request.getAttribute(OK_TO_USE_SESSION, WebRequest.SCOPE_REQUEST);
        if (useSessionForRequestProcessing == null) {
            // by default we will use the session
            return true;
        } else {
            return useSessionForRequestProcessing.booleanValue();
        }
    }

    /**
     * Takes {@link #isOKtoUseSession(WebRequest)} into account when retrieving session attributes. If it's not ok, this
     * will return null
     */
    public static Object getSessionAttributeIfOk(WebRequest request, String attribute) {
        if (isOKtoUseSession(request)) {
            return request.getAttribute(attribute, WebRequest.SCOPE_GLOBAL_SESSION);
        }
        return null;
    }

    /**
     * Takes {@link #isOKtoUseSession(WebRequest)} into account when setting a session attribute
     *
     * @return <b>true</b> if this set the session attribute, <b>false</b> otherwise
     */
    public static boolean setSessionAttributeIfOk(WebRequest request, String attribute, Object value) {
        if (isOKtoUseSession(request)) {
            request.setAttribute(attribute, value, WebRequest.SCOPE_GLOBAL_SESSION);
            return true;
        }
        return false;
    }

    /**
     * Sets whether or not PranveraApp can utilize the session in request processing.   Used by the REST API
     * flow so that RESTful calls do not utilize the session.
     *
     */
    public static void setOKtoUseSession(WebRequest request, Boolean value) {
        request.setAttribute(OK_TO_USE_SESSION, value, WebRequest.SCOPE_REQUEST);
    }

    /**
     * Get header or url parameter.    Will obtain the parameter from a header variable or a URL parameter, preferring
     * header values if they are set.
     *
     */
    public static String getURLorHeaderParameter(WebRequest request, String varName) {
        String returnValue = request.getHeader(varName);
        if (returnValue == null) {
            returnValue = request.getParameter(varName);
        }
        return returnValue;
    }

    /**
     * Convenience method to obtain the server prefix of the current request.
     * Useful for many modules that configure Relative URL's and need to send absolute URL's
     * to Third Party Gateways.
     */
    public static String getRequestedServerPrefix() {
        HttpServletRequest request = PranveraAppRequestContext.getPranveraAppRequestContext().getRequest();
        String scheme = request.getScheme();
        StringBuilder serverPrefix = new StringBuilder(scheme);
        serverPrefix.append("://");
        serverPrefix.append(request.getServerName());
        if ((scheme.equalsIgnoreCase("http") && request.getServerPort() != 80) || (scheme.equalsIgnoreCase("https") && request.getServerPort() != 443)) {
            serverPrefix.append(":");
            serverPrefix.append(request.getServerPort());
        }
        return serverPrefix.toString();
    }
}
