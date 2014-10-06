package com.abstratt.mdd.target.sql.mappers.select;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.CallOperationAction;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.Type;

import com.abstratt.mdd.core.target.IActionMapper;
import com.abstratt.mdd.core.target.ILanguageMapper;
import com.abstratt.mdd.core.target.IMapper;
import com.abstratt.mdd.core.target.IMappingContext;
import com.abstratt.mdd.core.target.IMappingContext.Style;
import com.abstratt.mdd.core.target.spi.MapperFinder;
import com.abstratt.mdd.core.target.spi.MappingContext;

public class SelectMapper implements IActionMapper<Action> {
	private MapperFinder mapperFinder = new MapperFinder(SelectMapper.class);

	public String map(Action target, IMappingContext context) {
		IActionMapper mapping = mapperFinder.getMapping(target);
		return (mapping != null) ? mapping.map(target, context) : "/*FIXME: mapping for " + target.eClass().getName() + " not implemented*/";
	}
}
