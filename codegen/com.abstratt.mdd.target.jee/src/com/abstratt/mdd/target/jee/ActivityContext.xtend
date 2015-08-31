package com.abstratt.mdd.target.jee

import java.util.List
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.OutputPin
import com.google.common.base.Function

class ActivityContext {
    final static protected ThreadLocal<List<ActivityContext>> activityContexts = new ThreadLocal<List<ActivityContext>>() {
		override protected initialValue() {
			newLinkedList()
		}
	}
	
    public final Activity activity
    public final OutputPin self

    /** 
     * @param activity possibly null (if context has no behavior in the input model)
     */
    new(Activity activity, OutputPin self) {
        this.activity = activity
        this.self = self
    }
    
    def static void newActivityContext(Activity activity, OutputPin self) {
        activityContexts.get().add(new ActivityContext(activity, self))
    }

    def static void dropActivityContext() {
        activityContexts.get().remove(activityContexts.get().tail)
    }

    def static ActivityContext getCurrent() {
        activityContexts.get().last
    }
    
    def static CharSequence generateInNewContext(Activity activity, OutputPin self, Function<Void, CharSequence> generator) {
        newActivityContext(activity, self)
        try {
			return generator.apply(null)
        } finally {
            ActivityContext.dropActivityContext
        }
    }
}
