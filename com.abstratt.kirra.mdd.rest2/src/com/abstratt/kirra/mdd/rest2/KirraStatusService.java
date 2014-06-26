package com.abstratt.kirra.mdd.rest2;

import java.util.Date;
import java.util.List;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.data.Preference;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.service.StatusService;

import com.abstratt.kirra.KirraException;
import com.abstratt.nodestore.NodeStoreException;
import com.abstratt.pluginutils.UserFacingException;

public class KirraStatusService extends StatusService {

    @Override
    public Representation getRepresentation(Status status, Request request, Response response) {
        List<Preference<MediaType>> mediaTypes = request.getClientInfo().getAcceptedMediaTypes();
        for (Preference<MediaType> preference : mediaTypes)
            if (preference.getMetadata() == MediaType.APPLICATION_JSON)
                return new StringRepresentation("{ \"message\": \"" + status.getDescription() + "\"}", preference.getMetadata());
        return new StringRepresentation(status.getDescription());
    }

    @Override
    public Status getStatus(Throwable throwable, Request request, Response response) {
        if (response.isEntityAvailable())
            response.getEntity().setExpirationDate(new Date(0));
        Throwable original = throwable;
        while (isUserFacing(throwable.getCause()))
            throwable = throwable.getCause();
        if (throwable instanceof NodeStoreException)
            return new Status(Status.SERVER_ERROR_SERVICE_UNAVAILABLE, "Application database is not available");
        if (isUserFacing(throwable))
            return new Status(Status.SERVER_ERROR_SERVICE_UNAVAILABLE, getUserFacingMessage(throwable));
        return super.getStatus(original, request, response);
    }

    private String getUserFacingMessage(Throwable throwable) {
        if (throwable instanceof UserFacingException)
            return ((UserFacingException) throwable).getUserFacingMessage();
        return throwable.getMessage();
    }

    private boolean isUserFacing(Throwable throwable) {
        return throwable instanceof UserFacingException || throwable instanceof KirraException;
    }
}
