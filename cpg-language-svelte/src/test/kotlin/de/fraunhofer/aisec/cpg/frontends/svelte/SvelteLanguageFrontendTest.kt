package de.fraunhofer.aisec.cpg.frontends.svelte

import de.fraunhofer.aisec.cpg.BaseTest // Or your project's base test class
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.test.analyze
import kotlin.test.*
import org.junit.jupiter.api.Disabled // Keep import if needed elsewhere

class SvelteLanguageFrontendTest : BaseTest() { // Ensure this inherits from your test base

    // Define the path to your test resources
    private val topLevel = "src/test/resources/svelte/"

    @Test
    // @Disabled // Remove this annotation once parsing is implemented - REMOVED
    fun testSimpleComponent() {
        val result = analyze(listOf(topLevel + "SimpleComponent.svelte"), topLevel, true) {
            // Ensure the enableSvelteFrontend property is set for the test runner if needed
            // or configure language directly if analyze allows:
            it.registerLanguage<SvelteLanguage>()
             .failOnError(true) // Optional: Fail test immediately on frontend error
        }

        assertNotNull(result)

        // --- Assertions based on current implementation --- 

        // Check for the TranslationUnitDeclaration
        val tu = result.components.firstOrNull()?.translationUnits?.firstOrNull()
        assertNotNull(tu, "Expected a TranslationUnitDeclaration")
        assertEquals("SimpleComponent.svelte", tu.name)

        // Check for the RecordDeclaration representing the component
        val record = tu.declarations.filterIsInstance<RecordDeclaration>().firstOrNull()
        assertNotNull(record, "Expected a RecordDeclaration for the component")
        assertEquals("SimpleComponent", record.name)
        assertEquals("class", record.kind) // As currently set in handleRoot

        // Check for the placeholder instance script method
        val instanceInitMethod = record.methods.firstOrNull { it.name.localName == "<instance-init>" }
        assertNotNull(instanceInitMethod, "Expected a placeholder method for the instance script")
        assertTrue(instanceInitMethod.body == null || instanceInitMethod.body?.statements?.isEmpty() == true, "Instance init method body should be empty for now")

        // Check for comments added by placeholder handlers
        val comments = record.comment?.split("\n") ?: emptyList()
        assertTrue(comments.any { it.startsWith("Instance script content needs parsing:") }, "Missing instance script comment")
        assertTrue(comments.any { it.startsWith("Style block content:") }, "Missing style block comment")
        assertTrue(comments.any { it.startsWith("Template text:") }, "Missing template text comment")
        assertTrue(comments.any { it.startsWith("Template expression needs parsing:") }, "Missing template expression comment")
        assertTrue(comments.any { it.startsWith("Template comment:") }, "Missing template comment comment")

        // TODO: Add more specific assertions as the frontend implementation progresses
        // e.g., Check for actual variable declarations from the script once parsed.
        // e.g., Check for CPG nodes representing the H1 element and the expression tag.
    }

    // TODO: Add more tests for various Svelte features:
    // - Props
    // - Event handlers (on:click)
    // - Bindings (bind:value)
    // - Reactive declarations ($:)
    // - Control flow (#if, #each, #await)
    // - Component imports and usage
    // - Style block handling (if implemented)
} 