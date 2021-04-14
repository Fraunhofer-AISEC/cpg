package de.fraunhofer.aisec.cpg.frontends.python

import de.fraunhofer.aisec.cpg.ExperimentalPython
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.File
import java.lang.Exception
import java.nio.file.Path
import jep.*

@ExperimentalPython
class PythonLanguageFrontend(config: TranslationConfiguration, scopeManager: ScopeManager?) :
    LanguageFrontend(config, scopeManager, ".") {
    companion object {
        @kotlin.jvm.JvmField var PY_EXTENSIONS: List<String> = listOf(".py")
    }

    @Throws(TranslationException::class)
    override fun parse(file: File): TranslationUnitDeclaration {
        return parseInternal(file.readText(Charsets.UTF_8), file.path)
    }

    override fun <T> getCodeFromRawNode(astNode: T): String? {
        // will be invoked by native function
        return null
    }

    override fun <T> getLocationFromRawNode(astNode: T): PhysicalLocation? {
        // will be invoked by native function
        return null
    }

    override fun <S, T> setComment(s: S, ctx: T) {
        // will be invoked by native function
    }

    private fun parseInternal(code: String, path: String): TranslationUnitDeclaration {
        val topLevel = Path.of("src/main/python")
        val entryScript = topLevel.resolve("main.py").toAbsolutePath()

        val tu: TranslationUnitDeclaration

        try {
            JepSingleton // configure Jep
            val interp = SubInterpreter(JepConfig().setRedirectOutputStreams(true))

            // TODO: extract main.py in a real python module with multiple files

            // load script
            interp.runScript(entryScript.toString())

            // run python function parse_code()
            tu = interp.invoke("parse_code", code, path, this) as TranslationUnitDeclaration
            interp.close()
        } catch (e: JepException) {
            e.printStackTrace()
            throw TranslationException("Python failed with message: $e")
        } catch (e: Exception) {
            throw e
        }

        return tu
    }
}
