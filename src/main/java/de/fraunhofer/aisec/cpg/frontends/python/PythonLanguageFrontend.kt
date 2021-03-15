package de.fraunhofer.aisec.cpg.frontends.python

import de.fraunhofer.aisec.cpg.ExperimentalPython
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import jep.SharedInterpreter
import java.io.File
import java.lang.Exception
import java.nio.file.Path

@ExperimentalPython
class PythonLanguageFrontend(config: TranslationConfiguration, scopeManager: ScopeManager?) : LanguageFrontend(config, scopeManager, ".") {
    @Throws(TranslationException::class)
    override fun parse(file: File): TranslationUnitDeclaration {
        return parseInternal(file.readText(Charsets.UTF_8))
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

    private fun parseInternal(s: String?): TranslationUnitDeclaration {
        if(s.isNullOrEmpty())
            throw TranslationException("No code provided.")

        val topLevel = Path.of("python")
        val entryScript = topLevel.resolve("main.py").toAbsolutePath()

        val tud: TranslationUnitDeclaration

        try {
            // TODO
            jep.MainInterpreter.setJepLibraryPath("/home/maximilian/.virtualenvs/jep/lib/python3.9/site-packages/jep/libjep.so")

            val interp: SharedInterpreter = SharedInterpreter()

            // provide code to python (as global variable)
            interp.set("codeToParse", s)

            // load script
            interp.runScript(entryScript.toString())

            // run python function run()
            interp.exec("run()")

            // get result
            tud = interp.getValue("res") as TranslationUnitDeclaration
            
            // clean up
            interp.exec("del codeToParse")
            interp.exec("del res")
            interp.close()
        } catch (e: Exception) {
            throw TranslationException("Python failed with message: $e")
        }
        return tud
    }
}