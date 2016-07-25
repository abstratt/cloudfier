package com.abstratt.mdd.importer.tests.jdbc

import com.abstratt.mdd.importer.jdbc.JDBCImporter
import java.util.Properties
import org.junit.Test
import static org.junit.Assert.*
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4)
class JDBCImporterNamingTests {

	@Test
	def void testTableRename() {
		val jdbcImporterProperties = new Properties()
		jdbcImporterProperties.put("mdd.importer.jdbc.table.fixcase.fragments", "books,authors")
		jdbcImporterProperties.put("mdd.importer.jdbc.table.rename.books", "book")
		jdbcImporterProperties.put("mdd.importer.jdbc.table.rename.authors", "author")
		
		assertEquals("Book", new JDBCImporter(jdbcImporterProperties).toClassName("books"))
	}
	
	@Test
	def void testFragmentInTableName() {
		val jdbcImporterProperties = new Properties()
		jdbcImporterProperties.put("mdd.importer.jdbc.table.fixcase.fragments", "books,authors")
		jdbcImporterProperties.put("mdd.importer.jdbc.table.rename.books", "book")
		jdbcImporterProperties.put("mdd.importer.jdbc.table.rename.authors", "author")
		
		assertEquals("Book", new JDBCImporter(jdbcImporterProperties).toClassName("books"))
	}
}
