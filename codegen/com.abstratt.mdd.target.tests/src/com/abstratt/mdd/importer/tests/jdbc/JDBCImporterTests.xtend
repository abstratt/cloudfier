package com.abstratt.mdd.importer.tests.jdbc

import com.abstratt.mdd.core.tests.harness.AbstractRepositoryBuildingTests
import com.abstratt.mdd.modelimporter.jdbc.JDBCImporter
import java.util.Properties
import org.junit.Test
import schemacrawler.schema.Catalog
import schemacrawler.tools.integration.serialization.XmlSerializedCatalog
import java.io.Reader

class JDBCImporterTests extends AbstractRepositoryBuildingTests {
	
	new(String name) {
		super(name)
	}
	
	
    @Test
    def void testBasic() {
    	//TODO-RC working here
		val reader = null as Reader
		val jdbcImporterProperties = new Properties()
		val Catalog catalog = new XmlSerializedCatalog(reader)
    	val generated = new JDBCImporter(jdbcImporterProperties).generateTextUMLModel(catalog)
    	assertNotNull(generated.get('mdd.properties')) 	
    }
}