package com.abstratt.mdd.importer.tests.jdbc

import com.abstratt.mdd.importer.jdbc.JDBCImporter
import java.util.Properties
import junit.framework.TestCase
import org.junit.Test

class JDBCImporterNamingTests extends TestCase {

	@Test
	def void testClassNameMapping() {
		val jdbcImporterProperties = new Properties()
		val importer = new JDBCImporter(jdbcImporterProperties)
		assertEquals("WaterService", importer.toClassName("Water_Service"))
		assertEquals("WaterService", importer.toClassName("water_service"))
	}
	
	@Test
	def void testAttributeNameMapping() {
		val jdbcImporterProperties = new Properties()
		val importer = new JDBCImporter(jdbcImporterProperties)
		assertEquals("inspectionsAdded", importer.toAttributeName("Inspections_Added"))
		assertEquals("inspectionsAdded", importer.toAttributeName("inspections_added"))
	}

	@Test
	def void testTableRename() {
		val jdbcImporterProperties = new Properties()
		jdbcImporterProperties.put("mdd.importer.jdbc.table.fixcase.fragments", "books,authors")
		jdbcImporterProperties.put("mdd.importer.jdbc.table.rename.books", "book")
		jdbcImporterProperties.put("mdd.importer.jdbc.table.rename.authors", "author")
		
		assertEquals("Book", new JDBCImporter(jdbcImporterProperties).toClassName("books"))
	}
	
	@Test
	def void testSchemaRename() {
		val jdbcImporterProperties = new Properties()
		jdbcImporterProperties.put("mdd.importer.jdbc.schema.rename.dbo", "myapp")
		
		assertEquals("myapp", new JDBCImporter(jdbcImporterProperties).toPackageName("dbo"))
	}	
	
	@Test
	def void testFragmentInTableName() {
		val jdbcImporterProperties = new Properties()
		jdbcImporterProperties.put("mdd.importer.jdbc.table.fixcase.fragments", "books,authors")
		jdbcImporterProperties.put("mdd.importer.jdbc.table.rename.books", "book")
		jdbcImporterProperties.put("mdd.importer.jdbc.table.rename.authors", "author")
		
		assertEquals("Book", new JDBCImporter(jdbcImporterProperties).toClassName("books"))
	}
	
	@Test
	def void testTableNameFilter() {
		val jdbcImporterProperties = new Properties()
		jdbcImporterProperties.put("mdd.importer.jdbc.table.filter.inclusion", "p1.*,p3.*")
		jdbcImporterProperties.put("mdd.importer.jdbc.table.filter.exclusion", "p2.*")
		val importer = new JDBCImporter(jdbcImporterProperties)
		assertTrue(importer.isTableIncluded("p1foo"))
		assertTrue(!importer.isTableIncluded("p2bar"))
		assertTrue(importer.isTableIncluded("p3zed"))
	}
	
	@Test
	def void testTableNameFilter_NoInclusionPatterns() {
		val jdbcImporterProperties = new Properties()
		jdbcImporterProperties.put("mdd.importer.jdbc.table.filter.exclusion", "p2.*")
		val importer = new JDBCImporter(jdbcImporterProperties)
		assertTrue(importer.isTableIncluded("p1foo"))
		assertTrue(!importer.isTableIncluded("p2bar"))
		assertTrue(importer.isTableIncluded("p3zed"))
	}	
}
