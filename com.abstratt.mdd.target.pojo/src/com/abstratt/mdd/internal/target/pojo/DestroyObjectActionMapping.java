package com.abstratt.mdd.internal.target.pojo;

import org.eclipse.uml2.uml.DestroyObjectAction;

import com.abstratt.mdd.core.target.IActionMapper;
import com.abstratt.mdd.core.target.IMappingContext;


public class DestroyObjectActionMapping implements IActionMapper<DestroyObjectAction> {

	public String map(DestroyObjectAction action, IMappingContext context) {
		// it is an error to generate DestroyObjectAction for POJOs
		return "if (true) throw new UnsupportedOperationException(\"Cannot destroy objects in POJOs\");"; 
    }

}
