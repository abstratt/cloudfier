package com.abstratt.kirra.mdd.rest;

import com.abstratt.kirra.mdd.runtime.KirraActorSelector;
import com.abstratt.mdd.core.runtime.Runtime;
import com.abstratt.mdd.core.runtime.RuntimeObject;

public class KirraRESTActorSelector extends KirraActorSelector {
    private RuntimeObject cachedActor;

    private boolean cached = false;

    public void clearCache() {
        cachedActor = null;
        cached = false;
    }

    @Override
    public RuntimeObject getCurrentActor(Runtime runtime) {
        if (!cached || (cachedActor == null && KirraRESTUtils.getCurrentUserName() != null)) {
            cachedActor = super.getCurrentActor(runtime);
            cached = true;
        }
        return cachedActor;
    }

    @Override
    public String getUserMnemonic() {
        return KirraRESTUtils.getCurrentUserName();
    }
}
