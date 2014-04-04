package com.abstratt.mdd.core.target;

import java.util.List;

import org.eclipse.uml2.uml.Element;

/** Transforms a UML element into text. */ 
public interface IMapper<E extends Element> {
    public String map(E toMap);
    public String mapAll(List<E> toMap);
}
