package com.abstratt.mdd.target.doc

import com.abstratt.kirra.mdd.core.KirraHelper
import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.target.ITopLevelMapper
import com.abstratt.mdd.core.target.OutputHolder
import java.io.IOException
import java.io.InputStream
import java.util.LinkedHashMap
import org.eclipse.uml2.uml.Class

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import org.eclipse.uml2.uml.NamedElement
import java.util.Map
import java.util.Collection
import org.eclipse.uml2.uml.Package

class DataDictionaryMapper implements ITopLevelMapper<Class> {

	def InputStream getTemplateContents(String path) {
		val templatePath = '''/templates/«path»'''
		val templateContents = getClass().getResourceAsStream(templatePath)
		if (templateContents == null)
			throw new IOException("Resource not found: " + templatePath)
		return templateContents
	}

	override Map<String, OutputHolder<?>> mapMultiple(IRepository repository) {
		val classDiagramSettings = new LinkedHashMap(#{
			'showClasses' -> true.toString,
			'showAttributes' -> true.toString
		})
		val stateDiagramSettings = new LinkedHashMap(#{'showStateMachines' -> true.toString})
		
		val applicationName = getApplicationName(repository)
		val applicationDescription = getApplicationLabel(repository)
		val allPackages = repository.getOwnPackages(null).map [ #[it] + it.nestedPackages ].flatten()
		val appPackages = allPackages.applicationPackages
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
		val showDiagrams = Boolean.valueOf(repository.properties.getProperty('mdd.doc.showDiagrams', Boolean.TRUE.toString))
		appPackages.forEach[ package_ |
			result.put('''«package_.qualifiedName.replace(NamedElement.SEPARATOR, ".")».html''', new TextHolder(new DataDictionaryGenerator(repository, showDiagrams).generatePackage(package_)))	
		]
		result.put('data-dictionary.html', new TextHolder(new DataDictionaryGenerator(repository, showDiagrams).generatePackageIndex(appPackages)))
		if (showDiagrams) {
			val diagramGenerator = new DiagramGenerator(repository)
			appPackages.forEach [ appPackage |
				result.put(diagramGenerator.toJavaPackage(appPackage) + '-classes.png',
					new BinaryHolder(diagramGenerator.generateDiagramAsImage(classDiagramSettings, appPackage)))
				result.put(diagramGenerator.toJavaPackage(appPackage) + '-state.png',
					new BinaryHolder(diagramGenerator.generateDiagramAsImage(stateDiagramSettings, appPackage)))
			]
		}
		return result
	}
	
	override getKind() {
		return Kind.Binary
	}

}
