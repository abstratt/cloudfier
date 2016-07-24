package com.abstratt.mdd.importer.tests.jdbc

import com.abstratt.mdd.core.tests.harness.AbstractRepositoryBuildingTests
import com.abstratt.mdd.importer.jdbc.JDBCImporter
import java.io.InputStreamReader
import java.util.Properties
import org.junit.Test

class JDBCImporterTests extends AbstractRepositoryBuildingTests {
	
	new(String name) {
		super(name)
	}
	
	
    @Test
    def void testBasic() {
    	val snapshotContents = new InputStreamReader(JDBCImporterTests.getResourceAsStream("schemacrawler.data"))
    	try {
			val jdbcImporterProperties = new Properties()
	    	val generated = new JDBCImporter(jdbcImporterProperties).importModelFromSnapshot(snapshotContents)
	    	assertNotNull(generated.get('mdd.properties')) 	
    	} finally {
    		snapshotContents.close()
    	}
    }
}