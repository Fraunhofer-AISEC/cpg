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
    @Throws(TranslationException::class)
    override fun parse(file: File): TranslationUnitDeclaration {
        return parseInternal(file.readText(Charsets.UTF_8), file.path)
    }

    // TODO
    override fun <T> getCodeFromRawNode(astNode: T): String? {
        return null
    }

    // TODO
    override fun <T> getLocationFromRawNode(astNode: T): PhysicalLocation? {
        return null
    }

    // TODO
    override fun <S, T> setComment(s: S, ctx: T) {}

    private fun parseInternal(s: String?, path: String): TranslationUnitDeclaration {
        if (s == null) throw TranslationException("No code provided.")

        val topLevel = Path.of("src/main/python")
        val entryScript = topLevel.resolve("main.py").toAbsolutePath()

        val tu: TranslationUnitDeclaration

        try {
            JepSingleton // configure Jep
            val interp = SubInterpreter(JepConfig().setRedirectOutputStreams(true))

            // TODO: extract into an actual python module and call it

            // load script
            interp.runScript(entryScript.toString())

            // run python function run()
            tu = interp.invoke("parseCode", s, path, this) as TranslationUnitDeclaration
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
