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
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.types.AutoType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.helpers.CommentMatcher
import de.fraunhofer.aisec.cpg.passes.PythonAddDeclarationsPass
import de.fraunhofer.aisec.cpg.passes.configuration.RegisterExtraPass
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.io.File
import java.net.URI
import jep.python.PyObject
import kotlin.io.path.Path
import kotlin.io.path.nameWithoutExtension
import kotlin.math.min

@RegisterExtraPass(PythonAddDeclarationsPass::class)
class PythonLanguageFrontend(language: Language<PythonLanguageFrontend>, ctx: TranslationContext) :
    LanguageFrontend<Python.AST.AST, Python.AST.AST?>(language, ctx) {
    private val lineSeparator = '\n' // TODO
    private val tokenTypeIndex = 0
    private val jep = JepSingleton // configure Jep

    // val declarationHandler = DeclarationHandler(this)
    // val specificationHandler = SpecificationHandler(this)
    internal var statementHandler = StatementHandler(this)
    internal var expressionHandler = ExpressionHandler(this)

    /**
     * fileContent contains the whole file can be stored as a class field because the CPG creates a
     * new [PythonLanguageFrontend] instance per file.
     */
    private lateinit var fileContent: String
    private lateinit var uri: URI

    @Throws(TranslationException::class)
    override fun parse(file: File): TranslationUnitDeclaration {
        fileContent = file.readText(Charsets.UTF_8)
        uri = file.toURI()

        jep.getInterp().use {
            it.set("content", fileContent)
            it.set("filename", file.absolutePath)
            it.exec("import ast")
            it.exec("parsed = ast.parse(content, filename=filename, type_comments=True)")

            val pyAST = it.getValue("parsed") as PyObject
            val tud = pythonASTtoCPG(pyAST, file.name)

            if (config.matchCommentsToNodes) {
                it.exec("import tokenize")
                it.exec("reader = tokenize.open(filename).readline")
                it.exec("tokens = tokenize.generate_tokens(reader)")
                it.exec("tokenList = list(tokens)")
                // This constant has to be retrieved from the system as it was changed in different
                // Python versions
                it.exec("commentCode = tokenize.COMMENT")

                val pyCommentCode =
                    (it.getValue("commentCode") as? Long) ?: TODO("Cannot get comment of $it")
                val pyTokens =
                    (it.getValue("tokenList") as? ArrayList<*>) ?: TODO("Cannot get tokens of $it")
                addCommentsToCPG(tud, pyTokens, pyCommentCode)
            }
            return tud
        }
    }

    private fun addCommentsToCPG(
        tud: TranslationUnitDeclaration,
        pyTokens: ArrayList<*>,
        pyCommentCode: Long
    ) {
        val commentMatcher = CommentMatcher()
        for (token in pyTokens) {
            if (token !is List<*> || token.size != 5) {
                TODO()
            } else {
                if (token[tokenTypeIndex] as Long != pyCommentCode) {
                    continue
                } else {
                    val start = token[2] as List<*>
                    val end = token[3] as List<*>
                    val startLine = start[0] as Long
                    val startCol = start[1] as Long
                    val endLine = end[0] as Long
                    val endCol = end[1] as Long

                    commentMatcher.matchCommentToNode(
                        token[1] as String,
                        Region(
                            startLine.toInt(),
                            (startCol + 1).toInt(),
                            endLine.toInt(),
                            (endCol + 1).toInt()
                        ),
                        tud
                    )
                }
            }
        }
    }

    /**
     * Type information is optional in python in form of annotations. So if a type annotation is
     * present, we parse it, otherwise we assume that it is dynamically typed and thus return an
     * [AutoType].
     */
    override fun typeOf(type: Python.AST.AST?): Type {
        return when (type) {
            null -> {
                // No type information -> we return an autoType to infer things magically
                autoType()
            }

            is Python.AST.Name -> {
                this.typeOf(type.id)
            }

            else -> {
                // The AST supplied us with some kind of type information, but we could not parse
                // it, so we need to return the unknown type.
                unknownType()
            }
        }
    }

    /**
     * Resolves a [Type] based on its string identifier.
     * test hook
     */
    fun typeOf(typeId: String): Type {
        // Check if the typeId contains a namespace delimiter for qualified types
        val name =
            if (language.namespaceDelimiter in typeId) {
                // TODO: This might create problem with nested classes
                parseName(typeId)
            } else {
                // Unqualified name, resolved by the type resolver
                typeId
            }

        return objectType(name)
    }

    /**
     * This functions extracts the source code from the input file given a location. This is a bit
     * tricky in Python, as indents are part of the syntax. We also don't want to include leading
     * whitespaces/tabs in case of extracting a nested code fragment. Thus, we use the following
     * approximation to retrieve the fragment's source code:
     * 1) Get the relevant source code lines
     * 2) Delete extra code at the end of the last line that is not part of the provided location
     * 3) Remove trailing whitespaces / tabs
     */
    override fun codeOf(astNode: Python.AST.AST): String? {
        return if (astNode is Python.AST.Module) {
            fileContent
        } else {
            val location = locationOf(astNode)
            if (location != null) {
                var lines = getRelevantLines(location)
                lines = removeExtraAtEnd(location, lines)
                lines = fixStartColumn(location, lines)

                lines.joinToString(separator = lineSeparator.toString())
            } else {
                null
            }
        }
    }

    private fun getRelevantLines(location: PhysicalLocation): MutableList<String> {
        val lines =
            fileContent
                .split(lineSeparator)
                .subList(location.region.startLine - 1, location.region.endLine)
        return lines.toMutableList()
    }

    private fun fixStartColumn(
        location: PhysicalLocation,
        lines: MutableList<String>
    ): MutableList<String> {
        for (idx in lines.indices) {
            // -1 to equalize for +1 in sarif
            val prefixLength = min(location.region.startColumn - 1, lines[idx].length)
            if (idx == 0) {
                lines[idx] = lines[idx].substring(prefixLength)
            }
        }
        return lines
    }

    private fun removeExtraAtEnd(
        location: PhysicalLocation,
        lines: MutableList<String>
    ): MutableList<String> {
        val lastLineIdx = lines.lastIndex
        val lastLineLength = lines[lastLineIdx].length
        val locationEndColumn = location.region.endColumn
        val toRemove = lastLineLength - locationEndColumn + 1
        if (toRemove > 0) {
            lines[lastLineIdx] = lines[lastLineIdx].dropLast(toRemove)
        }
        return lines
    }

    override fun locationOf(astNode: Python.AST.AST): PhysicalLocation? {
        return if (astNode is Python.AST.WithLocation) {
            PhysicalLocation(
                uri,
                Region(
                    startLine = astNode.lineno,
                    endLine = astNode.end_lineno,
                    startColumn = astNode.col_offset + 1,
                    endColumn = astNode.end_col_offset + 1,
                )
            )
        } else {
            null
        }
    }

    override fun setComment(node: Node, astNode: Python.AST.AST) {
        // will be invoked by native function
    }

    private fun pythonASTtoCPG(pyAST: PyObject, path: String): TranslationUnitDeclaration {
        val pythonASTModule =
            fromPython(pyAST) as? Python.AST.Module
                ?: TODO(
                    "Python ast of type ${fromPython(pyAST).javaClass} is not supported yet"
                ) // could be one of "ast.{Module,Interactive,Expression,FunctionType}

        val tud = newTranslationUnitDeclaration(path, rawNode = pythonASTModule)
        scopeManager.resetToGlobal(tud)

        val nsdName = Path(path).nameWithoutExtension
        val nsd = newNamespaceDeclaration(nsdName, rawNode = pythonASTModule)
        tud.addDeclaration(nsd)

        scopeManager.enterScope(nsd)
        for (stmt in pythonASTModule.body) {
            nsd.statements += statementHandler.handle(stmt)
        }
        scopeManager.leaveScope(nsd)

        scopeManager.addDeclaration(nsd)

        return tud
    }

    fun operatorToString(op: Python.AST.BaseOperator) =
        when (op) {
            is Python.AST.Add -> "+"
            is Python.AST.Sub -> "-"
            is Python.AST.Mult -> "*"
            is Python.AST.MatMult -> "*"
            is Python.AST.Div -> "/"
            is Python.AST.Mod -> "%"
            is Python.AST.Pow -> "**"
            is Python.AST.LShift -> "<<"
            is Python.AST.RShift -> ">>"
            is Python.AST.BitOr -> "|"
            is Python.AST.BitXor -> "^"
            is Python.AST.BitAnd -> "&"
            is Python.AST.FloorDiv -> "//"
        }

    fun operatorUnaryToString(op: Python.AST.BaseUnaryOp) =
        when (op) {
            is Python.AST.Invert -> "~"
            is Python.AST.Not -> "not"
            is Python.AST.UAdd -> "+"
            is Python.AST.USub -> "-"
        }
}

/**
 * This function maps Python's `ast` objects to out internal [Python] representation.
 *
 * @param pyObject the Python object
 * @return our Kotlin view of the Python `ast` object
 */
fun fromPython(pyObject: Any?): Python.BaseObject {
    if (pyObject !is PyObject) {
        TODO("Expected a PyObject")
    } else {
        var objectname =
            pyObject.getAttr("__class__").toString().substringAfter("'").substringBeforeLast("'")
        objectname = if (objectname.startsWith("_")) objectname.substringAfter("_") else objectname
        return when (objectname) {
            "ast.Module" -> Python.AST.Module(pyObject)

            // statements
            "ast.FunctionDef" -> Python.AST.FunctionDef(pyObject)
            "ast.AsyncFunctionDef" -> Python.AST.AsyncFunctionDef(pyObject)
            "ast.ClassDef" -> Python.AST.ClassDef(pyObject)
            "ast.Return" -> Python.AST.Return(pyObject)
            "ast.Delete" -> Python.AST.Delete(pyObject)
            "ast.Assign" -> Python.AST.Assign(pyObject)
            "ast.AugAssign" -> Python.AST.AugAssign(pyObject)
            "ast.AnnAssign" -> Python.AST.AnnAssign(pyObject)
            "ast.For" -> Python.AST.For(pyObject)
            "ast.AsyncFor" -> Python.AST.AsyncFor(pyObject)
            "ast.While" -> Python.AST.While(pyObject)
            "ast.If" -> Python.AST.If(pyObject)
            "ast.With" -> Python.AST.With(pyObject)
            "ast.AsyncWith" -> Python.AST.AsyncWith(pyObject)
            "ast.Match" -> Python.AST.Match(pyObject)
            "ast.Raise" -> Python.AST.Raise(pyObject)
            "ast.Try" -> Python.AST.Try(pyObject)
            "ast.TryStar" -> Python.AST.TryStar(pyObject)
            "ast.Assert" -> Python.AST.Assert(pyObject)
            "ast.Import" -> Python.AST.Import(pyObject)
            "ast.ImportFrom" -> Python.AST.ImportFrom(pyObject)
            "ast.Global" -> Python.AST.Global(pyObject)
            "ast.Nonlocal" -> Python.AST.Nonlocal(pyObject)
            "ast.Expr" -> Python.AST.Expr(pyObject)
            "ast.Pass" -> Python.AST.Pass(pyObject)
            "ast.Break" -> Python.AST.Break(pyObject)
            "ast.Continue" -> Python.AST.Continue(pyObject)

            // `"ast.expr`
            "ast.BoolOp" -> Python.AST.BoolOp(pyObject)
            "ast.NamedExpr" -> Python.AST.NamedExpr(pyObject)
            "ast.BinOp" -> Python.AST.BinOp(pyObject)
            "ast.UnaryOp" -> Python.AST.UnaryOp(pyObject)
            "ast.Lambda" -> Python.AST.Lambda(pyObject)
            "ast.IfExp" -> Python.AST.IfExp(pyObject)
            "ast.Dict" -> Python.AST.Dict(pyObject)
            "ast.Set" -> Python.AST.Set(pyObject)
            "ast.ListComp" -> Python.AST.ListComp(pyObject)
            "ast.SetComp" -> Python.AST.SetComp(pyObject)
            "ast.DictComp" -> Python.AST.DictComp(pyObject)
            "ast.GeneratorExp" -> Python.AST.GeneratorExp(pyObject)
            "ast.Await" -> Python.AST.Await(pyObject)
            "ast.Yield" -> Python.AST.Yield(pyObject)
            "ast.YieldFrom" -> Python.AST.YieldFrom(pyObject)
            "ast.Compare" -> Python.AST.Compare(pyObject)
            "ast.Call" -> Python.AST.Call(pyObject)
            "ast.FormattedValue" -> Python.AST.FormattedValue(pyObject)
            "ast.JoinedStr" -> Python.AST.JoinedStr(pyObject)
            "ast.Constant" -> Python.AST.Constant(pyObject)
            "ast.Attribute" -> Python.AST.Attribute(pyObject)
            "ast.Subscript" -> Python.AST.Subscript(pyObject)
            "ast.Starred" -> Python.AST.Starred(pyObject)
            "ast.Name" -> Python.AST.Name(pyObject)
            "ast.List" -> Python.AST.List(pyObject)
            "ast.Tuple" -> Python.AST.Tuple(pyObject)
            "ast.Slice" -> Python.AST.Slice(pyObject)

            // `"ast.boolop`
            "ast.And" -> Python.AST.And(pyObject)
            "ast.Or" -> Python.AST.Or(pyObject)

            // `"ast.cmpop`
            "ast.Eq" -> Python.AST.Eq(pyObject)
            "ast.NotEq" -> Python.AST.NotEq(pyObject)
            "ast.Lt" -> Python.AST.Lt(pyObject)
            "ast.LtE" -> Python.AST.LtE(pyObject)
            "ast.Gt" -> Python.AST.Gt(pyObject)
            "ast.GtE" -> Python.AST.GtE(pyObject)
            "ast.Is" -> Python.AST.Is(pyObject)
            "ast.IsNot" -> Python.AST.IsNot(pyObject)
            "ast.In" -> Python.AST.In(pyObject)
            "ast.NotIn" -> Python.AST.NotIn(pyObject)

            // `"ast.expr_context`
            "ast.Load" -> Python.AST.Load(pyObject)
            "ast.Store" -> Python.AST.Store(pyObject)
            "ast.Del" -> Python.AST.Del(pyObject)

            // `"ast.operator`
            "ast.Add" -> Python.AST.Add(pyObject)
            "ast.Sub" -> Python.AST.Sub(pyObject)
            "ast.Mult" -> Python.AST.Mult(pyObject)
            "ast.MatMult" -> Python.AST.MatMult(pyObject)
            "ast.Div" -> Python.AST.Div(pyObject)
            "ast.Mod" -> Python.AST.Mod(pyObject)
            "ast.Pow" -> Python.AST.Pow(pyObject)
            "ast.LShift" -> Python.AST.LShift(pyObject)
            "ast.RShift" -> Python.AST.RShift(pyObject)
            "ast.BitOr" -> Python.AST.BitOr(pyObject)
            "ast.BitXor" -> Python.AST.BitXor(pyObject)
            "ast.BitAnd" -> Python.AST.BitAnd(pyObject)
            "ast.FloorDiv" -> Python.AST.FloorDiv(pyObject)

            // `"ast.pattern`
            "ast.MatchValue" -> Python.AST.MatchValue(pyObject)
            "ast.MatchSingleton" -> Python.AST.MatchSingleton(pyObject)
            "ast.MatchSequence" -> Python.AST.MatchSequence(pyObject)
            "ast.MatchMapping" -> Python.AST.MatchMapping(pyObject)
            "ast.MatchClass" -> Python.AST.MatchClass(pyObject)
            "ast.MatchStar" -> Python.AST.MatchStar(pyObject)
            "ast.MatchAs" -> Python.AST.MatchAs(pyObject)
            "ast.MatchOr" -> Python.AST.MatchOr(pyObject)

            // `"ast.unaryop`
            "ast.Invert" -> Python.AST.Invert(pyObject)
            "ast.Not" -> Python.AST.Not(pyObject)
            "ast.UAdd" -> Python.AST.UAdd(pyObject)
            "ast.USub" -> Python.AST.USub(pyObject)

            // misc
            "ast.alias" -> Python.AST.alias(pyObject)
            "ast.arg" -> Python.AST.arg(pyObject)
            "ast.arguments" -> Python.AST.arguments(pyObject)
            "ast.comprehension" -> Python.AST.comprehension(pyObject)
            "ast.excepthandler" -> Python.AST.excepthandler(pyObject)
            "ast.keyword" -> Python.AST.keyword(pyObject)
            "ast.match_case" -> Python.AST.match_case(pyObject)
            "ast.type_ignore" -> Python.AST.type_ignore(pyObject)
            "ast.withitem" -> Python.AST.withitem(pyObject)

            // complex numbers
            "complex" -> Python.Complex(pyObject)
            "ellipsis" -> Python.Ellipsis(pyObject)
            else -> {
                TODO("Implement for ${pyObject.getAttr("__class__")}")
            }
        }
    }
}
