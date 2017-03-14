package com.abstratt.kirra.mdd.runtime;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Property;

import com.abstratt.kirra.Entity;
import com.abstratt.kirra.Instance;
import com.abstratt.kirra.Repository;
import com.abstratt.kirra.mdd.core.KirraHelper;
import com.abstratt.mdd.core.runtime.ActorSelector;
import com.abstratt.mdd.core.runtime.Runtime;
import com.abstratt.mdd.core.runtime.RuntimeObject;
import com.abstratt.mdd.core.runtime.types.BasicType;
import com.abstratt.mdd.core.runtime.types.StringType;

public abstract class KirraActorSelector implements ActorSelector {

    public static RuntimeObject findUserInstance(final String userMnemonic, final Runtime runtime) {
        if (userMnemonic == null)
            return null;
		Class userClass = KirraHelper.getUserClass();
        if (userClass == null)
            return null;
        Property usernameAttribute = KirraHelper.getUsernameProperty(userClass);
        if (usernameAttribute != null) {
            Map<Property, List<BasicType>> filter = Collections.singletonMap(usernameAttribute, Collections.singletonList(new StringType(userMnemonic)));
            RuntimeObject user = runtime.findOneInstance(userClass, filter);
            if (user != null)
                return user;
        }
        return null;
    }

    @Override
    public RuntimeObject getCurrentActor(Runtime runtime) {
        String userMnemonic = getUserMnemonic();
        RuntimeObject found = findUserInstance(userMnemonic, runtime);
        return found;
    }
    
    @Override
    public List<RuntimeObject> getRoles(Runtime runtime) {
    	String userMnemonic = getUserMnemonic();
        RuntimeObject actor = findUserInstance(userMnemonic, runtime);
        if (actor == null)
        	return Collections.emptyList();
        List<Class> roleClasses = KirraHelper.getRoleEntities(Arrays.asList(runtime.getRepository().getTopLevelPackages(null)));
        Stream<RuntimeObject> roles = roleClasses.stream().map(roleClass -> runtime.getRoleForActor(actor, roleClass));
    	return roles.collect(Collectors.toList());
    }

    public abstract String getUserMnemonic();
}
