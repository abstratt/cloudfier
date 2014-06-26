package com.abstratt.kirra.mdd.rest2;

import java.net.URI;

import org.restlet.data.Reference;

import com.abstratt.kirra.rest.resources.URITranslator;
import com.abstratt.mdd.frontend.web.ReferenceUtils;

public class KirraMddUriTranslator extends URITranslator {
    @Override
    public URI toExternalURI(URI toTranslate) {
        return ReferenceUtils.getExternal(new Reference(toTranslate)).toUri();
    }
}
