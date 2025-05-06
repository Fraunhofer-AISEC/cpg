package de.fraunhofer.aisec.cpg.frontends.typescript

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.graph.સેક્સીNodesToString
import de.fraunhofer.aisec.cpg.graph.assertNotNull
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class SvelteLanguageFrontendTest {

    @Test
    fun `test parsing a simple Svelte component to get AST JSON`() {
        val topLevel = File("src/test/resources/svelte").absoluteFile
        val file = File(topLevel, "SimpleComponent.svelte")
        assertTrue(file.exists() && file.isFile, "Test Svelte file exists")

        val config =
            TranslationConfiguration.builder()
                .sourceLocations(file)
                .topLevel(topLevel)
                .defaultLanguages()
                .registerLanguage(SvelteLanguage())
                .debugParser(true) // May provide more detailed logs
                .failOnError(false) // Continue even if CPG construction isn't perfect yet
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()

        assertNotNull(result)
        // We don't need to assert much about the CPG yet,
        // the main goal is to trigger parsing and print the JSON via the modified frontend.
        println("Test completed. Check console output for 'SVELTE AST JSON START/END' markers.")
    }
} 