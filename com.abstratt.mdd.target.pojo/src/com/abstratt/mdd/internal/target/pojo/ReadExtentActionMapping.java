package com.abstratt.mdd.internal.target.pojo;

import org.eclipse.uml2.uml.ReadExtentAction;

import com.abstratt.mdd.core.target.IActionMapper;
import com.abstratt.mdd.core.target.IMappingContext;

public class ReadExtentActionMapping implements IActionMapper<ReadExtentAction> {
    public String map(ReadExtentAction action, IMappingContext context) {
        return "null /* FIXME: ReadExtent not supported for POJO mapping*/";
    }
}
