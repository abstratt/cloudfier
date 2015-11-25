package com.abstratt.mdd.target.jee

import com.google.common.base.Function
import java.util.List
import java.util.function.Supplier
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.OutputPin

class ActivityContext {
    final static protected ThreadLocal<List<ActivityContext>> activityContexts = new ThreadLocal<List<ActivityContext>>() {
		override protected initialValue() {
			newLinkedList()
		}
	}
	
    public final Activity activity
    private final Supplier<CharSequence> self

    /** 
     * @param activity possibly null (if context has no behavior in the input model)
     */
    new(Activity activity, Supplier<CharSequence> self) {
        this.activity = activity
        this.self = self
    }
    
    def private static void newActivityContext(Activity activity, Supplier<CharSequence> self) {
        activityContexts.get().add(new ActivityContext(activity, self))
    }

    def private static void dropActivityContext() {
        activityContexts.get().remove(activityContexts.get().tail)
    }

    def static ActivityContext getCurrent() {
        activityContexts.get().last
    }
    
    def static CharSequence generateInNewContext(Activity activity, Supplier<CharSequence> self, Supplier<CharSequence> generator) {
        newActivityContext(activity, self)
        try {
			return generator.get()
        } finally {
            ActivityContext.dropActivityContext
        }
    }
    
    def static CharSequence generateInNewContext(Activity activity, OutputPin self, Supplier<CharSequence> generator) {
    	generateInNewContext(activity, [ JPAHelper.alias(self) ] as Supplier, generator)
    }
	
	def static CharSequence generateSelf() {
		val generatedSelf = current.self.get()
		return generatedSelf ?:  'NO SELF!'
	}
    
}
