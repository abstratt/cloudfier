package com.abstratt.mdd.importer.tests.jdbc

import com.abstratt.mdd.importer.jdbc.JDBCImporter
import java.util.Properties
import junit.framework.TestCase
import org.junit.Test

class JDBCImporterNamingTests extends TestCase {

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
