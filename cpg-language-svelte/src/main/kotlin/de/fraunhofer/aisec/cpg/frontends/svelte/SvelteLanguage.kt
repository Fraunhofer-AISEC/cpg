package de.fraunhofer.aisec.cpg.frontends.svelte

import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.types.* // Assuming standard types might be reused initially
import kotlin.reflect.KClass
import org.neo4j.ogm.annotation.Transient

/** The Svelte language. */
class SvelteLanguage : Language<SvelteLanguageFrontend>() {

    override val fileExtensions = listOf("svelte")
    override val namespaceDelimiter = "." // Default, might need adjustment based on Svelte's module system specifics

    @Transient
    override val frontend: KClass<out SvelteLanguageFrontend> =
        SvelteLanguageFrontend::class

    // Initially, we can inherit or leave out operator/built-in type definitions.
    // These would need refinement based on how Svelte's <script> context behaves
    // compared to standard JavaScript. For now, let's keep it simple.

    // Example if inheriting JS operators:
    // override val conjunctiveOperators = listOf("&&", "&&=", "??", "??=")
    // override val disjunctiveOperators = listOf("||", "||=")
    // override val compoundAssignmentOperators = setOf(...)

    // Example for built-in types (might be same as JS initially):
    // @Transient
    // override val builtInTypes = mapOf(...)
} 