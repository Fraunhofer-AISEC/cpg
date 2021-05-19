package de.fraunhofer.aisec.cpg.frontends.golang

import de.fraunhofer.aisec.cpg.ExperimentalGolang
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.File
import kotlin.Throws

@ExperimentalGolang
class GoLanguageFrontend(config: TranslationConfiguration, scopeManager: ScopeManager?) :
    LanguageFrontend(config, scopeManager, ".") {
    companion object {
        @kotlin.jvm.JvmField var GOLANG_EXTENSIONS: List<String> = listOf(".go")

        init {
            System.loadLibrary("cpgo")
        }
    }

    @Throws(TranslationException::class)
    override fun parse(file: File): TranslationUnitDeclaration {
        TypeManager.getInstance().setLanguageFrontend(this)
        return parseInternal(file.readText(Charsets.UTF_8))
    }

    override fun <T> getCodeFromRawNode(astNode: T): String? {
        // this is handled by native code
        return null
    }

    override fun <T> getLocationFromRawNode(astNode: T): PhysicalLocation? {
        // this is handled by native code
        return null
    }

    override fun <S, T> setComment(s: S, ctx: T) {}

    private external fun parseInternal(s: String?): TranslationUnitDeclaration
}
