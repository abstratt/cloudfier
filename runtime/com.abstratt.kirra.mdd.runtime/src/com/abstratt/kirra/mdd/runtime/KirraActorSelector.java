package com.abstratt.kirra.mdd.runtime;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.query.conditions.eobjects.EObjectCondition;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.UMLPackage;

import com.abstratt.kirra.mdd.core.KirraHelper;
import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.runtime.ActorSelector;
import com.abstratt.mdd.core.runtime.Runtime;
import com.abstratt.mdd.core.runtime.RuntimeObject;
import com.abstratt.mdd.core.runtime.types.BasicType;
import com.abstratt.mdd.core.runtime.types.StringType;

public abstract class KirraActorSelector implements ActorSelector {

    public static RuntimeObject findUserInstance(final String userMnemonic, final Runtime runtime) {
        final IRepository mddRepository = runtime.getRepository();
        if (userMnemonic == null)
            return null;
        List<Class> userClasses = mddRepository.findAll(new EObjectCondition() {
            @Override
            public boolean isSatisfied(EObject eObject) {
                if (UMLPackage.Literals.CLASS != eObject.eClass())
                    return false;
                Class umlClass = (Class) eObject;
                return KirraHelper.isUser(umlClass) && KirraHelper.isConcrete(umlClass);
            }
        }, false);
        if (userClasses.isEmpty())
            return null;
        for (Class userClass : userClasses) {
            Property usernameAttribute = KirraHelper.getUsernameProperty(userClass);
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
        RuntimeObject found = KirraActorSelector.findUserInstance(userMnemonic, runtime);
        return found;
    }

    public abstract String getUserMnemonic();
}
