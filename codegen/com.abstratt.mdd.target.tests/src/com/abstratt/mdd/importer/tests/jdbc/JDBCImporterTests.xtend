package com.abstratt.mdd.importer.tests.jdbc

import com.abstratt.mdd.core.tests.harness.AbstractRepositoryBuildingTests
import com.abstratt.mdd.importer.jdbc.JDBCImporter
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.util.Properties
import org.apache.commons.io.IOUtils
import org.junit.Test

class JDBCImporterTests extends AbstractRepositoryBuildingTests {

	new(String name) {
		super(name)
	}

	@Test
	def void testBasic() {
		val jdbcImporterProperties = new Properties()
		jdbcImporterProperties.put("mdd.importer.jdbc.schema", "BOOKS")
		importSnapshot(jdbcImporterProperties)
		getClass("books::Books")
	}

	def importSnapshot(Properties jdbcImporterProperties) {
		val resourceAsStream = JDBCImporterTests.getResourceAsStream("schemacrawler.data.xml")
		val snapshotContents = new InputStreamReader(new ByteArrayInputStream(IOUtils.toByteArray(resourceAsStream)))
		resourceAsStream.close()
		val generated = new JDBCImporter(jdbcImporterProperties).importModelFromSnapshot(snapshotContents)
		val sources = generated.filter[key, value | key.endsWith(".tuml")].values.map[it.toString].toList
		sources.forEach[println(it)]
		parseAndCheck(sources.toList.toArray(newArrayOfSize(0)))
	}
}
