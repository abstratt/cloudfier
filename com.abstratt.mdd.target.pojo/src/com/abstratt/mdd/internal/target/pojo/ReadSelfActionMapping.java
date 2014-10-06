package com.abstratt.mdd.internal.target.pojo;

import org.eclipse.uml2.uml.ReadSelfAction;

import com.abstratt.mdd.core.target.IActionMapper;
import com.abstratt.mdd.core.target.IMappingContext;

public class ReadSelfActionMapping implements IActionMapper<ReadSelfAction> {
    public String map(ReadSelfAction action, IMappingContext context) {
        return "this";
    }
}
