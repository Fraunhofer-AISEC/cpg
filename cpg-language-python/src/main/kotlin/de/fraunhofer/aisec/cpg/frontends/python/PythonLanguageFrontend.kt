/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *                    $$$$$$\  $$$$$$$\   $$$$$$\
 *                   $$  __$$\ $$  __$$\ $$  __$$\
 *                   $$ /  \__|$$ |  $$ |$$ /  \__|
 *                   $$ |      $$$$$$$  |$$ |$$$$\
 *                   $$ |      $$  ____/ $$ |\_$$ |
 *                   $$ |  $$\ $$ |      $$ |  $$ |
 *                   \$$$$$   |$$ |      \$$$$$   |
 *                    \______/ \__|       \______/
 *
 */
package de.fraunhofer.aisec.cpg.frontends.python

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.SupportsParallelParsing
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.io.File
import java.net.URI
import jep.python.PyObject
import kotlin.io.path.Path

@SupportsParallelParsing(false)
class PythonLanguageFrontend(language: Language<PythonLanguageFrontend>, ctx: TranslationContext) :
    LanguageFrontend<PythonAST.Node, Any>(language, ctx) {
    private val jep = JepSingleton // configure Jep

    // val declarationHandler = DeclarationHandler(this)
    // val specificationHandler = SpecificationHandler(this)
    private var statementHandler = StatementHandler(this)
    // var expressionHandler = ExpressionHandler(this)

    @Throws(TranslationException::class)
    override fun parse(file: File): TranslationUnitDeclaration {
        val fileContent = file.readText(Charsets.UTF_8)
        val absolutePath = file.absolutePath
        jep.getInterp().use {
            // TODO: PYTHON VERSION CHECK!
            // TODO: add sanity check to [absolutePath] to avoid code injection

            it.eval("import ast")
            it.eval("import os")
            it.eval("fh = open(\"$absolutePath\", \"r\")")
            it.eval("parsed = ast.parse(fh.read(), filename=\"$absolutePath\", type_comments=True)")

            val pyAST = it.getValue("parsed") as PyObject
            return pythonASTtoCPG(pyAST, fileContent)
        }
    }

    override fun typeOf(type: Any): Type {
        // TODO
        return unknownType()
    }

    override fun codeOf(astNode: PythonAST.Node): String? {
        return null
        // TODO
    }

    override fun locationOf(astNode: PythonAST.Node): PhysicalLocation? {
        return if (astNode is PythonAST.WithPythonLocation) {
            PhysicalLocation(
                URI("file:///TODO"), // TODO
                Region(
                    startLine = astNode.lineno,
                    endLine = astNode.end_lineno,
                    startColumn = astNode.col_offset,
                    endColumn = astNode.end_col_offset,
                )
            )
        } else {
            null
        }
    }

    override fun setComment(node: Node, astNode: PythonAST.Node) {
        // will be invoked by native function
    }

    private fun pythonASTtoCPG(pyAST: PyObject, path: String): TranslationUnitDeclaration {
        val pythonASTModule =
            fromPython(pyAST) as? PythonAST.Module
                ?: TODO() // could be one of ast.{Module,Interactive,Expression,FunctionType}
        val tud = newTranslationUnitDeclaration(path, null, pythonASTModule) // TODO
        scopeManager.resetToGlobal(tud)
        val nsdName = Path(path).fileName.toString()
        val nsd = newNamespaceDeclaration(nsdName, null, pythonASTModule) // TODO
        tud.addDeclaration(nsd)
        scopeManager.enterScope(nsd)
        for (stmt in pythonASTModule.body) {
            val handledStmt = statementHandler.handle(stmt)
            nsd.addStatement(handledStmt)
        }
        scopeManager.leaveScope(nsd)
        scopeManager.addDeclaration(nsd)
        return tud
    }
}

/**
 * This function maps Python's `ast` objects to out internal [PythonAST] representation.
 *
 * @param pyObject the Python object
 * @return our Kotlin view of the Python `ast` object
 */
fun fromPython(pyObject: Any): PythonAST.Node {
    if (pyObject !is PyObject) {
        TODO("Expected a PyObject")
    } else {

        return when (pyObject.getAttr("__class__").toString()) {
            "<class 'ast.Module'>" -> PythonAST.Module(pyObject)
            "<class 'ast.ClassDef'>" -> PythonAST.ClassDef(pyObject)
            "<class 'ast.FunctionDef'>" -> PythonAST.FunctionDef(pyObject)
            "<class 'ast.arguments'>" -> PythonAST.Arguments(pyObject)
            "<class 'ast.ImportFrom'>" -> PythonAST.ImportFrom(pyObject)
            "<class 'ast.Assign'>" -> PythonAST.Assign(pyObject)
            "<class 'ast.If'>" -> PythonAST.If(pyObject)
            "<class 'ast.AnnAssign'>" -> PythonAST.AnnAssign(pyObject)
            "<class 'ast.arg'>" -> PythonAST.arg(pyObject)
            "<class 'ast.Expr'>" -> PythonAST.Expr(pyObject)
            "<class 'ast.Expression'>" -> PythonAST.Expression(pyObject)
            "<class 'ast.Pass'>" -> PythonAST.Pass(pyObject)
            "<class 'ast.For'>" -> PythonAST.For(pyObject)
            "<class 'ast.Return'>" -> PythonAST.Return(pyObject)
            "<class 'ast.Name'>" -> PythonAST.Name(pyObject)
            "<class 'ast.While'>" -> PythonAST.While(pyObject)
            else -> {
                TODO("Implement for ${pyObject.getAttr("__class__")}")
            }
        }
    }
}
