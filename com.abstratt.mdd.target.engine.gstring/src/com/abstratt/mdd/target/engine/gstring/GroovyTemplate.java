package com.abstratt.mdd.target.engine.gstring;

import java.util.List;

import org.eclipse.uml2.uml.NamedElement;

public abstract class GroovyTemplate {
    public Object generate(Object target) {
        return "Template is missing a generate(def toMap) function";
    }

    public Object generateAll(List<?> target) {
        return "Template is missing a generateAll(def toMap) function";
    }

    public Object generateFileName(Object toMap) {
        return ((NamedElement) toMap).getName() + ".txt";
    }

    public boolean matches(Object toMatch) {
        return true;
    }
}
