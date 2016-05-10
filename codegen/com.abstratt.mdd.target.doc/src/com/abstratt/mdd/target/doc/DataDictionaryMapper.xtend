package com.abstratt.mdd.target.doc

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*

import com.abstratt.kirra.mdd.core.KirraHelper
import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.target.ITopLevelMapper
import java.io.IOException
import java.io.InputStream
import java.util.LinkedHashMap
import org.eclipse.uml2.uml.Class
import com.abstratt.mdd.core.target.OutputHolder

class DataDictionaryMapper implements ITopLevelMapper<Class> {

	def InputStream getTemplateContents(String path) {
		val templatePath = '''/templates/«path»'''
		val templateContents = getClass().getResourceAsStream(templatePath)
		if (templateContents == null)
			throw new IOException("Resource not found: " + templatePath)
		return templateContents
	}

	override mapMultiple(IRepository repository) {
		val classDiagramSettings = new LinkedHashMap(#{
			'showClasses' -> true.toString,
			'showAttributes' -> true.toString
		})
		val stateDiagramSettings = new LinkedHashMap(#{'showStateMachines' -> true.toString})
		
		val applicationName = getApplicationName(repository)
		val applicationDescription = getApplicationLabel(repository)
		val appPackages = repository.getTopLevelPackages(null).applicationPackages
		val replacements = newLinkedHashMap(
			'applicationName' -> KirraHelper.getApplicationName(repository),
			'applicationDescription' -> applicationDescription,
			'groupId' -> applicationName,
			'groupPath' -> applicationName,
			'artifactId' -> applicationName,
			'version' -> '1.0'
		)
		repository.properties.forEach[key, value|replacements.put(key.toString(), value.toString)]
		val result = new LinkedHashMap<String, OutputHolder<?>>()
		result.put('data-dictionary.html', new TextHolder(new DataDictionaryGenerator(repository).generate()))
		appPackages.forEach [ appPackage |
			result.put(appPackage.name + '-classes.png',
				new BinaryHolder(new DiagramGenerator(repository).generateDiagramAsImage(classDiagramSettings, appPackage)))
			result.put(appPackage.name + '-state.png',
				new BinaryHolder(new DiagramGenerator(repository).generateDiagramAsImage(stateDiagramSettings, appPackage)))
		]
		return result
	}

	override getKind() {
		return Kind.Binary
	}

}
