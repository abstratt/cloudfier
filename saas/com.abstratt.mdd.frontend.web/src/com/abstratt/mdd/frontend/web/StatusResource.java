package com.abstratt.mdd.frontend.web;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class StatusResource extends ServerResource {
    @Get
    public Representation getServiceStatus() {
        double allocationThreshold = 0.95;
        double availabilityThreshold = 0.1;
        String availabilityThresholdParam = getQuery().getValues("available");
        String allocationThresholdParam = getQuery().getValues("allocated");
        try {
            if (availabilityThresholdParam != null)
                availabilityThreshold = Double.parseDouble(availabilityThresholdParam) / 100;
            if (allocationThresholdParam != null)
                allocationThreshold = Double.parseDouble(allocationThresholdParam) / 100;
        } catch (NumberFormatException e) {
            //
        }
        // ensure we check more stable values
        System.gc();
        System.gc();
        long maxMemory = Runtime.getRuntime().maxMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        double available = (double) freeMemory / totalMemory;
        double allocated = (double) totalMemory / maxMemory;

        NumberFormat percentageFmt = new DecimalFormat("##0.##%");
        DecimalFormat memoryFmt = new DecimalFormat("##0.##MB");

        double megaByte = Math.pow(2, 20);

        String allocatedPercentage = percentageFmt.format(allocated);
        String availablePercentage = percentageFmt.format(available);

        if (allocated > allocationThreshold && available < availabilityThreshold) {
            setStatus(Status.SERVER_ERROR_INTERNAL, "allocation: " + allocatedPercentage + " - available: " + availablePercentage);
            return new EmptyRepresentation();
        }
        StringBuffer result = new StringBuffer();
        result.append("<html><body>");
        result.append("<p>Max memory: " + memoryFmt.format(maxMemory / megaByte) + "</p>");
        result.append("<p>Total memory: " + memoryFmt.format(totalMemory / megaByte) + "</p>");
        result.append("<p>Free memory: " + memoryFmt.format(freeMemory / megaByte) + "</p>");
        result.append("<p>Allocated: " + allocatedPercentage + "</p>");
        result.append("<p>Available: " + availablePercentage + "</p>");
        return new StringRepresentation(result.toString(), MediaType.TEXT_HTML);
    }
}
