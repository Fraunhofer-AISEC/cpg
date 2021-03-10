package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.BaseTest
import de.fraunhofer.aisec.cpg.ExperimentalGraph
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExperimentalGraph
class TranslationResultTest : BaseTest() {
    @Test
    fun testFromTranslationUnit() {
        val file = File("src/test/resources/compiling/RecordDeclaration.java")

        val config = TranslationConfiguration.builder()
                .sourceLocations(listOf(file))
                .topLevel(file.parentFile)
                .defaultPasses()
                .debugParser(true)
                .failOnError(true)
                .build()

        val analyzer = TranslationManager.builder().config(config).build()

        val result = analyzer.analyze().get()

        val graph = result.graph
        assertNotNull(graph)

        var nodes = graph.query("MATCH (m:MethodDeclaration) RETURN m")
        // returns the method declaration as well as the constructor declaration
        assertEquals(2, nodes.size)

        nodes = graph.query("MATCH (l:Literal) WHERE l.value = 0 RETURN l")
        assertEquals(2, nodes.size)
    }
}