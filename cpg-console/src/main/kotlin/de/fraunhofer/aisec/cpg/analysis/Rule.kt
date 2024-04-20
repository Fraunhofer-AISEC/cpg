package de.fraunhofer.aisec.cpg.analysis

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.query.*
import io.github.detekt.sarif4k.Level

interface Rule {
    /**
     * the query result
     */
    val queryResult: QueryTree<*>?

    // consider https://github.com/microsoft/sarif-tutorials/blob/main/docs/Authoring-rule-metadata-and-result-messages.md
    // TODO: descriptive or "correct" names for the fields?
    // TODO: consider metadatea structure wrt output formats (SARIF but potentially others)
    //  rn the fields are quite specific which might not be ideal with multiple output formats
    /** stable and opaque identifier for the query */
    val id: String
    /** human readable name of the query */
    val name: String
    val shortDescription: String
    val mdShortDescription: String?
    val level: Level // TODO: custom enum, handle conversion in the specific reporter
    val message: String?
    val mdMessage: String?
    val messageArguments: List<String>?

    /**
     * executes the query on the given result. Stores the result in the [queryResult] field of the respective rule.
     * Should populate the [queryResult] field.
     * @param result the result of a translation
     */
    fun run(result: TranslationResult)
}