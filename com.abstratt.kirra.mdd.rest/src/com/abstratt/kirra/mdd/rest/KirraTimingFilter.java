package com.abstratt.kirra.mdd.rest;

import org.apache.commons.lang.time.StopWatch;
import org.eclipse.core.runtime.IStatus;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.routing.Filter;

import com.abstratt.pluginutils.LogUtils;

/**
 * A filter that times requests.
 */
public class KirraTimingFilter extends Filter {
    public static Restlet monitor(Restlet toMonitor) {
        KirraTimingFilter monitor = new KirraTimingFilter();
        monitor.setNext(toMonitor);
        return monitor;
    }

    @Override
    protected int doHandle(final Request request, final Response response) {
        StopWatch watch = new StopWatch();
        watch.start();
        int result = super.doHandle(request, response);
        watch.stop();
        LogUtils.log(IStatus.INFO, Activator.ID, request.toString() + " - " + watch.getTime() + "ms", null);
        return result;
    }
}
