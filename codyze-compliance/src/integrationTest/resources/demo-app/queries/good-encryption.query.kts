import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.calls
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.query.QueryTree
import de.fraunhofer.aisec.cpg.query.allExtended

fun statement1(tr: TranslationResult): QueryTree<Boolean> {
    val result = tr.allExtended<FunctionDeclaration>(sel = {
        it.name.localName.contains("encrypt") && !it.isInferred
    }) {
        QueryTree(it.calls.any {
            it.name.contains("very_good")
        })
    }
    return result
}