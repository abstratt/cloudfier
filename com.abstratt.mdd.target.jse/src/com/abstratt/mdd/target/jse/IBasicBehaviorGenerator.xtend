package com.abstratt.mdd.target.jse

import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.Parameter
import java.util.List

public interface IBasicBehaviorGenerator {
    def CharSequence generateActivity(Activity activity)

    def CharSequence generateActivityAsExpression(Activity toGenerate, boolean asClosure, List<Parameter> parameters)

    def CharSequence generateActivityAsExpression(Activity toGenerate, boolean asClosure)

    def CharSequence generateActivityAsExpression(Activity toGenerate)
}
