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
    LanguageFrontend<Python.AST, Python.AST?>(language, ctx) {
    private val lineSeparator = '\n' // TODO
    private val tokenTypeIndex = 0
    private val jep = JepSingleton // configure Jep

    // val declarationHandler = DeclarationHandler(this)
    // val specificationHandler = SpecificationHandler(this)
    private var statementHandler = StatementHandler(this)
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
    override fun typeOf(type: Python.AST?): Type {
        return when (type) {
            null -> {
                // No type information -> we return an autoType to infer things magically
                autoType()
            }
            is Python.ASTName -> {
                // We have some kind of name here; let's quickly check, if this is a primitive type
                val id = type.id
                if (id in language.primitiveTypeNames) {
                    return primitiveType(id)
                }

                // Otherwise, this could already be a fully qualified type
                val name =
                    if (language.namespaceDelimiter in id) {
                        // TODO: This might create problem with nested classes
                        parseName(id)
                    } else {
                        // If it is not, we want place it in the current namespace
                        scopeManager.currentNamespace.fqn(id)
                    }

                objectType(name)
            }
            else -> {
                // The AST supplied us with some kind of type information, but we could not parse
                // it, so we
                // need to return the unknown type.
                unknownType()
            }
        }
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
    override fun codeOf(astNode: Python.AST): String? {
        val location = locationOf(astNode)
        if (location != null) {
            var lines = getRelevantLines(location)
            lines = removeExtraAtEnd(location, lines)
            lines = fixStartColumn(location, lines)

            return lines.joinToString(separator = lineSeparator.toString())
        }
        return null
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

    override fun locationOf(astNode: Python.AST): PhysicalLocation? {
        return if (astNode is Python.WithPythonLocation) {
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

    override fun setComment(node: Node, astNode: Python.AST) {
        // will be invoked by native function
    }

    private fun pythonASTtoCPG(pyAST: PyObject, path: String): TranslationUnitDeclaration {
        val pythonASTModule =
            fromPython(pyAST) as? Python.ASTModule
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
}

/**
 * This function maps Python's `ast` objects to out internal [Python] representation.
 *
 * @param pyObject the Python object
 * @return our Kotlin view of the Python `ast` object
 */
fun fromPython(pyObject: Any?): Python.AST {
    if (pyObject !is PyObject) {
        TODO("Expected a PyObject")
    } else {
        var objectname =
            pyObject.getAttr("__class__").toString().substringAfter("'").substringBeforeLast("'")
        objectname = if (objectname.startsWith("_")) objectname.substringAfter("_") else objectname
        return when (objectname) {
            "ast.Module" -> Python.ASTModule(pyObject)

            // statements
            "ast.FunctionDef" -> Python.ASTFunctionDef(pyObject)
            "ast.AsyncFunctionDef" -> Python.ASTAsyncFunctionDef(pyObject)
            "ast.ClassDef" -> Python.ASTClassDef(pyObject)
            "ast.Return" -> Python.ASTReturn(pyObject)
            "ast.Delete" -> Python.ASTDelete(pyObject)
            "ast.Assign" -> Python.ASTAssign(pyObject)
            "ast.AugAssign" -> Python.ASTAugAssign(pyObject)
            "ast.AnnAssign" -> Python.ASTAnnAssign(pyObject)
            "ast.For" -> Python.ASTFor(pyObject)
            "ast.AsyncFor" -> Python.ASTAsyncFor(pyObject)
            "ast.While" -> Python.ASTWhile(pyObject)
            "ast.If" -> Python.ASTIf(pyObject)
            "ast.With" -> Python.ASTWith(pyObject)
            "ast.AsyncWith" -> Python.ASTAsyncWith(pyObject)
            "ast.Match" -> Python.ASTMatch(pyObject)
            "ast.Raise" -> Python.ASTRaise(pyObject)
            "ast.Try" -> Python.ASTTry(pyObject)
            "ast.TryStar" -> Python.ASTTryStar(pyObject)
            "ast.Assert" -> Python.ASTAssert(pyObject)
            "ast.Import" -> Python.ASTImport(pyObject)
            "ast.ImportFrom" -> Python.ASTImportFrom(pyObject)
            "ast.Global" -> Python.ASTGlobal(pyObject)
            "ast.Nonlocal" -> Python.ASTNonlocal(pyObject)
            "ast.Expr" -> Python.ASTExpr(pyObject)
            "ast.Pass" -> Python.ASTPass(pyObject)
            "ast.Break" -> Python.ASTBreak(pyObject)
            "ast.Continue" -> Python.ASTContinue(pyObject)

            // `"ast.expr`
            "ast.BoolOp" -> Python.ASTBoolOp(pyObject)
            "ast.NamedExpr" -> Python.ASTNamedExpr(pyObject)
            "ast.BinOp" -> Python.ASTBinOp(pyObject)
            "ast.UnaryOp" -> Python.ASTUnaryOp(pyObject)
            "ast.Lambda" -> Python.ASTLambda(pyObject)
            "ast.IfExp" -> Python.ASTIfExp(pyObject)
            "ast.Dict" -> Python.ASTDict(pyObject)
            "ast.Set" -> Python.ASTSet(pyObject)
            "ast.ListComp" -> Python.ASTListComp(pyObject)
            "ast.SetComp" -> Python.ASTSetComp(pyObject)
            "ast.DictComp" -> Python.ASTDictComp(pyObject)
            "ast.GeneratorExp" -> Python.ASTGeneratorExp(pyObject)
            "ast.Await" -> Python.ASTAwait(pyObject)
            "ast.Yield" -> Python.ASTYield(pyObject)
            "ast.YieldFrom" -> Python.ASTYieldFrom(pyObject)
            "ast.Compare" -> Python.ASTCompare(pyObject)
            "ast.Call" -> Python.ASTCall(pyObject)
            "ast.FormattedValue" -> Python.ASTFormattedValue(pyObject)
            "ast.JoinedStr" -> Python.ASTJoinedStr(pyObject)
            "ast.Constant" -> Python.ASTConstant(pyObject)
            "ast.Attribute" -> Python.ASTAttribute(pyObject)
            "ast.Subscript" -> Python.ASTSubscript(pyObject)
            "ast.Starred" -> Python.ASTStarred(pyObject)
            "ast.Name" -> Python.ASTName(pyObject)
            "ast.List" -> Python.ASTList(pyObject)
            "ast.Tuple" -> Python.ASTTuple(pyObject)
            "ast.Slice" -> Python.ASTSlice(pyObject)

            // `"ast.boolop`
            "ast.And" -> Python.ASTAnd(pyObject)
            "ast.Or" -> Python.ASTOr(pyObject)

            // `"ast.cmpop`
            "ast.Eq" -> Python.ASTEq(pyObject)
            "ast.NotEq" -> Python.ASTNotEq(pyObject)
            "ast.Lt" -> Python.ASTLt(pyObject)
            "ast.LtE" -> Python.ASTLtE(pyObject)
            "ast.Gt" -> Python.ASTGt(pyObject)
            "ast.GtE" -> Python.ASTGtE(pyObject)
            "ast.Is" -> Python.ASTIs(pyObject)
            "ast.IsNot" -> Python.ASTIsNot(pyObject)
            "ast.In" -> Python.ASTIn(pyObject)
            "ast.NotInt" -> Python.ASTNotIn(pyObject)

            // `"ast.expr_context`
            "ast.Load" -> Python.ASTLoad(pyObject)
            "ast.Store" -> Python.ASTStore(pyObject)
            "ast.Del" -> Python.ASTDel(pyObject)

            // `"ast.operator`
            "ast.Add" -> Python.ASTAdd(pyObject)
            "ast.Sub" -> Python.ASTSub(pyObject)
            "ast.Mult" -> Python.ASTMult(pyObject)
            "ast.MatMult" -> Python.ASTMatMult(pyObject)
            "ast.Div" -> Python.ASTDiv(pyObject)
            "ast.Mod" -> Python.ASTMod(pyObject)
            "ast.Pow" -> Python.ASTPow(pyObject)
            "ast.LShift" -> Python.ASTLShift(pyObject)
            "ast.RShift" -> Python.ASTRShift(pyObject)
            "ast.BitOr" -> Python.ASTBitOr(pyObject)
            "ast.BitXor" -> Python.ASTBitXor(pyObject)
            "ast.BitAnd" -> Python.ASTBitAnd(pyObject)
            "ast.FloorDiv" -> Python.ASTFloorDiv(pyObject)

            // `"ast.pattern`
            "ast.MatchValue" -> Python.ASTMatchValue(pyObject)
            "ast.MatchSingleton" -> Python.ASTMatchSingleton(pyObject)
            "ast.MatchSequence" -> Python.ASTMatchSequence(pyObject)
            "ast.MatchMapping" -> Python.ASTMatchMapping(pyObject)
            "ast.MatchClass" -> Python.ASTMatchClass(pyObject)
            "ast.MatchStar" -> Python.ASTMatchStar(pyObject)
            "ast.MatchAs" -> Python.ASTMatchAs(pyObject)
            "ast.MatchOr" -> Python.ASTMatchOr(pyObject)

            // `"ast.unaryop`
            "ast.Invert" -> Python.ASTInvert(pyObject)
            "ast.Not" -> Python.ASTNot(pyObject)
            "ast.UAdd" -> Python.ASTUAdd(pyObject)
            "ast.USub" -> Python.ASTUSub(pyObject)

            // misc
            "ast.alias" -> Python.ASTalias(pyObject)
            "ast.arg" -> Python.ASTarg(pyObject)
            "ast.arguments" -> Python.ASTarguments(pyObject)
            "ast.comprehension" -> Python.ASTcomprehension(pyObject)
            "ast.excepthandler" -> Python.ASTexcepthandler(pyObject)
            "ast.keyword" -> Python.ASTkeyword(pyObject)
            "ast.match_case" -> Python.ASTmatch_case(pyObject)
            "ast.type_ignore" -> Python.ASTtype_ignore(pyObject)
            "ast.withitem" -> Python.ASTwithitem(pyObject)

            // complex numbers
            "complex" -> TODO("Complex numbers are not supported yet")
            else -> {
                TODO("Implement for ${pyObject.getAttr("__class__")}")
            }
        }
    }
}
