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
package de.fraunhofer.aisec.cpg.frontends.golang

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.SupportsParallelParsing
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.frontends.golang.GoStandardLibrary.Modfile
import de.fraunhofer.aisec.cpg.frontends.golang.GoStandardLibrary.Parser
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.DeclarationSequence
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.newNamespaceDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.types.FunctionType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.unknownType
import de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass
import de.fraunhofer.aisec.cpg.passes.GoEvaluationOrderGraphPass
import de.fraunhofer.aisec.cpg.passes.GoExtraPass
import de.fraunhofer.aisec.cpg.passes.order.RegisterExtraPass
import de.fraunhofer.aisec.cpg.passes.order.ReplacePass
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.io.File
import java.net.URI

/**
 * A language frontend for the [GoLanguage]. It makes use the internal
 * [go/ast](https://pkg.go.dev/go/ast) package of the Go runtime to parse the AST of a Go program.
 * We make use of JNA to call a dynamic library which exports C function wrappers around the Go API.
 * This is needed because we cannot directly export Go structs and pointers to C.
 */
@SupportsParallelParsing(false)
@RegisterExtraPass(GoExtraPass::class)
@ReplacePass(
    lang = GoLanguage::class,
    old = EvaluationOrderGraphPass::class,
    with = GoEvaluationOrderGraphPass::class
)
class GoLanguageFrontend(language: Language<GoLanguageFrontend>, ctx: TranslationContext) :
    LanguageFrontend<GoStandardLibrary.Ast.Node, GoStandardLibrary.Ast.Expr>(language, ctx) {

    private var currentFileSet: GoStandardLibrary.Ast.FileSet? = null
    private var currentModule: GoStandardLibrary.Modfile.File? = null
    private var commentMap: GoStandardLibrary.Ast.CommentMap? = null
    var currentFile: GoStandardLibrary.Ast.File? = null

    val declarationHandler = DeclarationHandler(this)
    val specificationHandler = SpecificationHandler(this)
    var statementHandler = StatementHandler(this)
    var expressionHandler = ExpressionHandler(this)

    @Throws(TranslationException::class)
    override fun parse(file: File): TranslationUnitDeclaration {
        // Make sure, that our top level is set either way
        val topLevel =
            if (config.topLevel != null) {
                config.topLevel
            } else {
                file.parentFile
            }!!

        val std = GoStandardLibrary.INSTANCE

        // Try to parse a possible go.mod
        val goModFile = topLevel.resolve("go.mod")
        if (goModFile.exists()) {
            currentModule = Modfile.parse(goModFile.absolutePath, goModFile.readText())
        }

        val fset = std.NewFileSet()
        val f = Parser.parseFile(fset, file.absolutePath, file.readText())

        this.commentMap = std.NewCommentMap(fset, f, f.comments)

        currentFile = f
        currentFileSet = fset

        val tu = newTranslationUnitDeclaration(file.absolutePath, rawNode = f)
        scopeManager.resetToGlobal(tu)
        currentTU = tu

        for (spec in f.imports) {
            val import = specificationHandler.handle(spec)
            scopeManager.addDeclaration(import)
        }

        val p = newNamespaceDeclaration(f.name.name)
        scopeManager.enterScope(p)

        try {
            // we need to construct the package "path" (e.g. "encoding/json") out of the
            // module path as well as the current directory in relation to the topLevel
            var packagePath = file.parentFile.relativeTo(topLevel)

            // If we are in a module, we need to prepend the module path to it
            currentModule?.let { packagePath = File(it.module.mod.path).resolve(packagePath) }

            p.path = packagePath.path
        } catch (ex: IllegalArgumentException) {
            log.error(
                "Could not relativize package path to top level. Cannot set package path.",
                ex
            )
        }

        for (decl in f.decls) {
            // Retrieve all top level declarations. One "Decl" could potentially
            // contain multiple CPG declarations.
            val declaration = declarationHandler.handle(decl)
            if (declaration is DeclarationSequence) {
                declaration.declarations.forEach { scopeManager.addDeclaration(it) }
            } else {
                scopeManager.addDeclaration(declaration)
            }
        }

        scopeManager.leaveScope(p)

        scopeManager.addDeclaration(p)

        return tu
    }

    override fun typeOf(type: GoStandardLibrary.Ast.Expr): Type {
        return when (type) {
            is GoStandardLibrary.Ast.Ident -> {
                val name: String =
                    if (isBuiltinType(type.name)) {
                        // Definitely not an FQN type
                        type.name
                    } else {
                        // FQN'ize this name (with the current file)
                        "${currentFile?.name?.name}.${type.name}" // this.File.Name.Name
                    }

                objectType(name)
            }
            is GoStandardLibrary.Ast.ArrayType -> {
                return typeOf(type.elt).array()
            }
            is GoStandardLibrary.Ast.ChanType -> {
                // Handle them similar to a map type (see below)
                return objectType("chan", listOf(typeOf(type.value)))
            }
            is GoStandardLibrary.Ast.FuncType -> {
                val paramTypes = type.params.list.map { typeOf(it.type) }
                val returnTypes = type.results?.list?.map { typeOf(it.type) } ?: listOf()
                val name = funcTypeName(paramTypes, returnTypes)

                return FunctionType(name, paramTypes, returnTypes, this.language)
            }
            is GoStandardLibrary.Ast.MapType -> {
                // We cannot properly represent Go's built-in map types, yet so we have
                // to make a shortcut here and represent it as a Java-like map<K, V> type.
                return objectType("map", listOf(typeOf(type.key), typeOf(type.value)))
            }
            is GoStandardLibrary.Ast.StarExpr -> {
                typeOf(type.x).pointer()
            }
            else -> {
                log.warn("Not parsing type of type ${type.goType} yet")
                unknownType()
            }
        }
    }

    private fun isBuiltinType(name: String): Boolean {
        return when (name) {
            "bool",
            "byte",
            "complex128",
            "complex64",
            "error",
            "float32",
            "float64",
            "int",
            "int8",
            "int16",
            "int32",
            "int64",
            "rune",
            "string",
            "uint",
            "uint8",
            "uint16",
            "uint32",
            "uint64",
            "uintptr" -> true
            else -> false
        }
    }

    override fun codeOf(astNode: GoStandardLibrary.Ast.Node): String? {
        return currentFileSet?.code(astNode)
    }

    override fun locationOf(astNode: GoStandardLibrary.Ast.Node): PhysicalLocation? {
        val start = currentFileSet?.position(astNode.pos) ?: return null
        val end = currentFileSet?.position(astNode.end) ?: return null
        val url = currentFileSet?.fileName(astNode.pos)?.let { URI(it) } ?: return null

        return PhysicalLocation(url, Region(start.line, start.column, end.line, end.column))
    }

    override fun setComment(node: Node, astNode: GoStandardLibrary.Ast.Node) {
        // Since we are potentially calling this function more than once on a node because of the
        // way go is structured (one decl can contain multiple specs), we need to make sure, that we
        // are not "overriding" more specific comments with more global ones.
        if (node.comment == null) {
            val comment = this.commentMap?.comment(astNode)
            node.comment = comment
        }
    }

    /**
     * This function produces a Go-style function type name such as `func(int, string) string` or
     * `func(int) (error, string)`
     */
    private fun funcTypeName(paramTypes: List<Type>, returnTypes: List<Type>): String {
        val rn = mutableListOf<String>()
        val pn = mutableListOf<String>()

        for (t in paramTypes) {
            pn += t.name.toString()
        }

        for (t in returnTypes) {
            rn += t.name.toString()
        }

        val rs =
            if (returnTypes.size > 1) {
                rn.joinToString(", ", prefix = " (", postfix = ")")
            } else if (returnTypes.isNotEmpty()) {
                rn.joinToString(", ", prefix = " ")
            } else {
                ""
            }

        return pn.joinToString(", ", prefix = "func(", postfix = ")$rs")
    }

    fun getImportName(spec: GoStandardLibrary.Ast.ImportSpec): String {
        val name = spec.name
        if (name != null) {
            return name.name
        }

        val path = expressionHandler.handle(spec.path) as? Literal<*>
        val paths = (path?.value as? String)?.split("/") ?: listOf()

        return paths.lastOrNull() ?: ""
    }
}
