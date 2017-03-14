package com.abstratt.mdd.core.runtime;

import java.util.List;

public interface ActorSelector {
    public RuntimeObject getCurrentActor(Runtime runtime);
    
    public List<RuntimeObject> getRoles(Runtime runtime);
}
