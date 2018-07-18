package com.abstratt.mdd.target.doc

import com.abstratt.graphviz.GraphViz
import com.abstratt.kirra.mdd.target.base.AbstractGenerator
import com.abstratt.kirra.mdd.target.base.AbstractGenerator
import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.core.util.MDDUtil
import com.abstratt.mdd.modelrenderer.MapBackedSettingsSource
import com.abstratt.mdd.modelrenderer.RenderingSettings
import com.abstratt.mdd.modelrenderer.uml2dot.UML2DOT
import java.awt.Color
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.util.Arrays
import java.util.Map
import java.util.Properties
import javax.imageio.ImageIO
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.text.WordUtils
import org.eclipse.core.runtime.CoreException
import org.eclipse.core.runtime.Path
import org.eclipse.uml2.uml.Package
import java.awt.Graphics
import java.awt.font.FontRenderContext
import java.awt.font.LineMetrics

class DiagramGenerator extends AbstractGenerator {
	
	private static final String DIAGRAM_SETTING_PREFIX = "mdd.diagram.";
	
	new(IRepository repository) {
		super(repository)
	}
	
	def byte[] generateDiagramAsDot(Map<String, String> diagramSettings, Package packageToRender) throws CoreException {
        val File repositoryLocation = new File(repository.getBaseURI().toFileString());
        // project settings win
        loadDiagramSettings(diagramSettings, new File(repositoryLocation, IRepository.MDD_PROPERTIES));
        val RenderingSettings settings = new RenderingSettings(new MapBackedSettingsSource(diagramSettings));
        // need to support top-level and child packages as well
        val packageUri = packageToRender.eResource().getURI();
    	return UML2DOT.generateDOTFromModel(MDDUtil.fromEMFToJava(packageUri), Arrays.asList(packageToRender), settings);
    }
    
    def byte[] generateDiagramAsImage(Map<String, String> diagramSettings, Package packageToRender) throws CoreException {
    	val asDot = generateDiagramAsDot(diagramSettings, packageToRender)
    	return convertDotToImageUsingGraphviz(asDot)    	 
    }
    
    def byte[] generateTextAsImage(String message) throws CoreException {
    	val lines = message.split("\\n").map[WordUtils.wrap(it, 60).split("\\n").toList].flatten()
		val fontSize = 14
		val longestLine = lines.reduce[a, b | if (a.length > b.length) a else b]
		val Font font = new Font(Font.MONOSPACED, 0, fontSize)
		val lineMetrics = font.getStringBounds(longestLine, new FontRenderContext(null, false, false))
		val lineWidth = lineMetrics.bounds.width
		val lineHeight = lineMetrics.bounds.height
		
		val width = lineWidth + 20
		val height = (lines.size) * lineHeight + 20
		val BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
		val Graphics2D ig2 = bi.createGraphics()
		
		ig2.setFont(font)
		ig2.background = Color.white
		ig2.fillRect(0, 0, width, height)
		ig2.setPaint(Color.black)
		lines.forEach[ line, index |
			ig2.drawString(line, 10, 10 + ((1 + index) * (lineHeight)).intValue)
		]
		val byteArrayStream = new ByteArrayOutputStream
		ImageIO.write(bi, "PNG", byteArrayStream)
		return byteArrayStream.toByteArray
	}

    private def void loadDiagramSettings(Map<String, String> diagramSettings, File propertiesFile) {
        if (propertiesFile.isFile()) {
            val properties = new Properties();
            properties.load(new ByteArrayInputStream(FileUtils.readFileToByteArray(propertiesFile)));
            for (Object property : properties.keySet()) {
                val propertyAsString = property as String;
                if (propertyAsString.startsWith(DIAGRAM_SETTING_PREFIX) && propertyAsString.length() > DIAGRAM_SETTING_PREFIX.length()) {
                    val diagramSetting = propertyAsString.substring(DIAGRAM_SETTING_PREFIX.length());
                    diagramSettings.put(diagramSetting, properties.getProperty(propertyAsString));
                }
            }
		}
    }

    private def byte[] convertDotToImageUsingGraphviz(byte[] dotContents) {
        val baseDir = new File(System.getProperty("java.io.tmpdir")).toPath().resolve("kirra");
        Files.createDirectories(baseDir);
        val outputLocation = Files.createTempFile(baseDir, "graphviz", ".png");
        GraphViz.generate(new ByteArrayInputStream(dotContents), "png", 0, 0, new Path(outputLocation.toString()));
        try {
        	return FileUtils.readFileToByteArray(outputLocation.toFile());
        } finally {
            outputLocation.toFile().delete();
        }
    }
	
	
}