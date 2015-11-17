package com.abstratt.mdd.frontend.web;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.abstratt.pluginutils.LogUtils;

public class Activator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        LogUtils.logInfo(WebFrontEnd.ID, 
                "Started endpoint\n" 
                        + "\tExternal: " + ReferenceUtils.EXTERNAL_BASE + "\n" 
                        + "\tInternal: " + ReferenceUtils.INTERNAL_BASE + "\n", null);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
    }

}
