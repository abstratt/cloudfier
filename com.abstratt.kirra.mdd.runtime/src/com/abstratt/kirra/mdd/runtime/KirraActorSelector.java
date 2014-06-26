package com.abstratt.kirra.mdd.runtime;

import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.query.conditions.eobjects.EObjectCondition;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.UMLPackage;

import com.abstratt.kirra.mdd.core.KirraHelper;
import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.runtime.ActorSelector;
import com.abstratt.mdd.core.runtime.Runtime;
import com.abstratt.mdd.core.runtime.RuntimeObject;
import com.abstratt.mdd.core.runtime.types.BasicType;
import com.abstratt.mdd.core.util.FeatureUtils;

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
        for (Class userClass : userClasses)
            for (BasicType runtimeObject : runtime.getAllInstances(userClass)) {
                RuntimeObject user = (RuntimeObject) runtimeObject;
                BasicType value = user.getValue(FeatureUtils.findAttribute(userClass, "username", false, true));
                if (value != null) {
                    String mnemonicValue = value.toString();
                    if (userMnemonic.equalsIgnoreCase(mnemonicValue))
                        return user;
                }
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
