package de.fraunhofer.aisec.cpg.frontends.python

import de.fraunhofer.aisec.cpg.ExperimentalPython
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import jep.*
import java.io.File
import java.lang.Exception
import java.nio.file.Path

@ExperimentalPython
class PythonLanguageFrontend(config: TranslationConfiguration, scopeManager: ScopeManager?) : LanguageFrontend(config, scopeManager, ".") {
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
        if(s == null)
            throw TranslationException("No code provided.")

        val topLevel = Path.of("python")
        val entryScript = topLevel.resolve("main.py").toAbsolutePath()

        val tud: TranslationUnitDeclaration

        try {
            JepSingleton // configure Jep
            val interp = SubInterpreter(JepConfig().setRedirectOutputStreams(true))

            // provide code to python (as global variable)
            interp.set("global_codeToParse", s)
            interp.set("global_fname", path)
            interp.set("global_scopemanager", this.getScopeManager())

            // load script
            interp.runScript(entryScript.toString())

            // run python function run()
            interp.exec("run()")

            // get result
            tud = interp.getValue("global_res") as TranslationUnitDeclaration
            
            // clean up
            interp.exec("del global_codeToParse")
            interp.exec("del global_fname")
            interp.exec("del global_res")
            interp.exec("del global_scopemanager")
            interp.close()
        } catch (e: JepException) {
            throw TranslationException("Python failed with message: $e")
        } catch (e: Exception) {
            throw e
        }
        return tud
    }
}