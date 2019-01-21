package com.abstratt.mdd.target.doc

import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.target.ITopLevelMapper
import com.abstratt.mdd.core.target.OutputHolder
import java.util.LinkedHashMap
import java.util.Map
import org.eclipse.uml2.uml.Class
import static extension com.abstratt.kirra.mdd.core.KirraHelper.*

class OpenAPIMapper implements ITopLevelMapper<Class> {
	override Map<String, OutputHolder<?>> mapMultiple(IRepository repository) {
		val allPackages = repository.getOwnPackages(null).map [ #[it] + it.nestedPackages ].flatten()
		val relevantPackages = allPackages.applicationPackages
		val result = new LinkedHashMap<String, OutputHolder<?>>()
		result.put('swagger-api.json', new TextHolder(new OASGenerator(repository).generatePackages(relevantPackages)))
		return result
	}	
}