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
import de.fraunhofer.aisec.cpg.passes.order.RegisterExtraPass
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
                val pyTokens = (it.getValue("tokenList") as? ArrayList<*>) ?: TODO()
                addCommentsToCPG(tud, pyTokens)
            }
            return tud
        }
    }

    private fun addCommentsToCPG(tud: TranslationUnitDeclaration, pyTokens: ArrayList<*>) {
        val commentMatcher = CommentMatcher()
        for (token in pyTokens) {
            if (token !is List<*> || token.size != 5) {
                TODO()
            } else {
                if (token[0] as Long != 61.toLong()) {
                    continue
                } else {
                    val start = token[2] as List<*>
                    val end = token[2] as List<*>
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
        when (type) {
            null -> {
                // No type information -> we return an autoType to infer things magically
                return autoType()
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

                return objectType(name)
            }
            else -> {
                // The AST supplied us with some kind of type information, but we could not parse
                // it, so we
                // need to return the unknown type.
                return unknownType()
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
            val prefixLength = min(location.region.startColumn, lines[idx].length)
            if (idx == 0) {
                lines[idx] = lines[idx].substring(prefixLength)
            } else {

                for (j in 0..prefixLength - 1) {
                    if (lines[idx][0] == ' ' || lines[idx][0] == '\t') {
                        lines[idx] = lines[idx].substring(1)
                    } else {
                        break
                    }
                }
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
        val toRemove = lastLineLength - locationEndColumn
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
                    startColumn = astNode.col_offset,
                    endColumn = astNode.end_col_offset,
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
                ?: TODO() // could be one of ast.{Module,Interactive,Expression,FunctionType}

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

        return when (pyObject.getAttr("__class__").toString()) {
            "<class 'ast.Module'>" -> Python.ASTModule(pyObject)

            // statements
            "<class 'ast.FunctionDef'>" -> Python.ASTFunctionDef(pyObject)
            "<class 'ast.AsyncFunctionDef'>" -> Python.ASTAsyncFunctionDef(pyObject)
            "<class 'ast.ClassDef'>" -> Python.ASTClassDef(pyObject)
            "<class 'ast.Return'>" -> Python.ASTReturn(pyObject)
            "<class 'ast.Delete'>" -> Python.ASTDelete(pyObject)
            "<class 'ast.Assign'>" -> Python.ASTAssign(pyObject)
            "<class 'ast.AugAssign'>" -> Python.ASTAugAssign(pyObject)
            "<class 'ast.AnnAssign'>" -> Python.ASTAnnAssign(pyObject)
            "<class 'ast.For'>" -> Python.ASTFor(pyObject)
            "<class 'ast.AsyncFor'>" -> Python.ASTAsyncFor(pyObject)
            "<class 'ast.While'>" -> Python.ASTWhile(pyObject)
            "<class 'ast.If'>" -> Python.ASTIf(pyObject)
            "<class 'ast.With'>" -> Python.ASTWith(pyObject)
            "<class 'ast.AsyncWith'>" -> Python.ASTAsyncWith(pyObject)
            "<class 'ast.Match'>" -> Python.ASTMatch(pyObject)
            "<class 'ast.Raise'>" -> Python.ASTRaise(pyObject)
            "<class 'ast.Try'>" -> Python.ASTTry(pyObject)
            "<class 'ast.TryStar'>" -> Python.ASTTryStar(pyObject)
            "<class 'ast.Assert'>" -> Python.ASTAssert(pyObject)
            "<class 'ast.Import'>" -> Python.ASTImport(pyObject)
            "<class 'ast.ImportFrom'>" -> Python.ASTImportFrom(pyObject)
            "<class 'ast.Global'>" -> Python.ASTGlobal(pyObject)
            "<class 'ast.Nonlocal'>" -> Python.ASTNonlocal(pyObject)
            "<class 'ast.Expr'>" -> Python.ASTExpr(pyObject)
            "<class 'ast.Pass'>" -> Python.ASTPass(pyObject)
            "<class 'ast.Break'>" -> Python.ASTBreak(pyObject)
            "<class 'ast.Continue'>" -> Python.ASTContinue(pyObject)

            // `ast.expr`
            "<class 'ast.BoolOp'>" -> Python.ASTBoolOp(pyObject)
            "<class 'ast.NamedExpr'>" -> Python.ASTNamedExpr(pyObject)
            "<class 'ast.BinOp'>" -> Python.ASTBinOp(pyObject)
            "<class 'ast.UnaryOp'>" -> Python.ASTUnaryOp(pyObject)
            "<class 'ast.Lambda'>" -> Python.ASTLambda(pyObject)
            "<class 'ast.IfExp'>" -> Python.ASTIfExp(pyObject)
            "<class 'ast.Dict'>" -> Python.ASTDict(pyObject)
            "<class 'ast.Set'>" -> Python.ASTSet(pyObject)
            "<class 'ast.ListComp'>" -> Python.ASTListComp(pyObject)
            "<class 'ast.SetComp'>" -> Python.ASTSetComp(pyObject)
            "<class 'ast.DictComp'>" -> Python.ASTDictComp(pyObject)
            "<class 'ast.GeneratorExp'>" -> Python.ASTGeneratorExp(pyObject)
            "<class 'ast.Await'>" -> Python.ASTAwait(pyObject)
            "<class 'ast.Yield'>" -> Python.ASTYield(pyObject)
            "<class 'ast.YieldFrom'>" -> Python.ASTYieldFrom(pyObject)
            "<class 'ast.Compare'>" -> Python.ASTCompare(pyObject)
            "<class 'ast.Call'>" -> Python.ASTCall(pyObject)
            "<class 'ast.FormattedValue'>" -> Python.ASTFormattedValue(pyObject)
            "<class 'ast.JoinedStr'>" -> Python.ASTJoinedStr(pyObject)
            "<class 'ast.Constant'>" -> Python.ASTConstant(pyObject)
            "<class 'ast.Attribute'>" -> Python.ASTAttribute(pyObject)
            "<class 'ast.Subscript'>" -> Python.ASTSubscript(pyObject)
            "<class 'ast.Starred'>" -> Python.ASTStarred(pyObject)
            "<class 'ast.Name'>" -> Python.ASTName(pyObject)
            "<class 'ast.List'>" -> Python.ASTList(pyObject)
            "<class 'ast.Tuple'>" -> Python.ASTTuple(pyObject)
            "<class 'ast.Slice'>" -> Python.ASTSlice(pyObject)

            // `ast.boolop`
            "<class 'ast.And'>" -> Python.ASTAnd(pyObject)
            "<class 'ast.Or'>" -> Python.ASTOr(pyObject)

            // `ast.cmpop`
            "<class 'ast.Eq'>" -> Python.ASTEq(pyObject)
            "<class 'ast.NotEq'>" -> Python.ASTNotEq(pyObject)
            "<class 'ast.Lt'>" -> Python.ASTLt(pyObject)
            "<class 'ast.LtE'>" -> Python.ASTLtE(pyObject)
            "<class 'ast.Gt'>" -> Python.ASTGt(pyObject)
            "<class 'ast.GtE'>" -> Python.ASTGtE(pyObject)
            "<class 'ast.Is'>" -> Python.ASTIs(pyObject)
            "<class 'ast.IsNot'>" -> Python.ASTIsNot(pyObject)
            "<class 'ast.In'>" -> Python.ASTIn(pyObject)
            "<class 'ast.NotInt'>" -> Python.ASTNotIn(pyObject)

            // `ast.expr_context`
            "<class 'ast.Load'>" -> Python.ASTLoad(pyObject)
            "<class 'ast.Store'>" -> Python.ASTStore(pyObject)
            "<class 'ast.Del'>" -> Python.ASTDel(pyObject)

            // `ast.operator`
            "<class 'ast.Add'>" -> Python.ASTAdd(pyObject)
            "<class 'ast.Sub'>" -> Python.ASTSub(pyObject)
            "<class 'ast.Mult'>" -> Python.ASTMult(pyObject)
            "<class 'ast.MatMult'>" -> Python.ASTMatMult(pyObject)
            "<class 'ast.Div'>" -> Python.ASTDiv(pyObject)
            "<class 'ast.Mod'>" -> Python.ASTMod(pyObject)
            "<class 'ast.Pow'>" -> Python.ASTPow(pyObject)
            "<class 'ast.LShift'>" -> Python.ASTLShift(pyObject)
            "<class 'ast.RShift'>" -> Python.ASTRShift(pyObject)
            "<class 'ast.BitOr'>" -> Python.ASTBitOr(pyObject)
            "<class 'ast.BitXor'>" -> Python.ASTBitXor(pyObject)
            "<class 'ast.BitAnd'>" -> Python.ASTBitAnd(pyObject)
            "<class 'ast.FloorDiv'>" -> Python.ASTFloorDiv(pyObject)

            // `ast.pattern`
            "<class 'ast.MatchValue'>" -> Python.ASTMatchValue(pyObject)
            "<class 'ast.MatchSingleton'>" -> Python.ASTMatchSingleton(pyObject)
            "<class 'ast.MatchSequence'>" -> Python.ASTMatchSequence(pyObject)
            "<class 'ast.MatchMapping'>" -> Python.ASTMatchMapping(pyObject)
            "<class 'ast.MatchClass'>" -> Python.ASTMatchClass(pyObject)
            "<class 'ast.MatchStar'>" -> Python.ASTMatchStar(pyObject)
            "<class 'ast.MatchAs'>" -> Python.ASTMatchAs(pyObject)
            "<class 'ast.MatchOr'>" -> Python.ASTMatchOr(pyObject)

            // `ast.unaryop`
            "<class 'ast.Invert'>" -> Python.ASTInvert(pyObject)
            "<class 'ast.Not'>" -> Python.ASTNot(pyObject)
            "<class 'ast.UAdd'>" -> Python.ASTUAdd(pyObject)
            "<class 'ast.USub'>" -> Python.ASTUSub(pyObject)

            // misc
            "<class 'ast.alias'>" -> Python.ASTalias(pyObject)
            "<class 'ast.arg'>" -> Python.ASTarg(pyObject)
            "<class 'ast.arguments'>" -> Python.ASTarguments(pyObject)
            "<class 'ast.comprehension'>" -> Python.ASTcomprehension(pyObject)
            "<class 'ast.excepthandler'>" -> Python.ASTexcepthandler(pyObject)
            "<class 'ast.keyword'>" -> Python.ASTkeyword(pyObject)
            "<class 'ast.match_case'>" -> Python.ASTmatch_case(pyObject)
            "<class 'ast.type_ignore'>" -> Python.ASTtype_ignore(pyObject)
            "<class 'ast.withitem'>" -> Python.ASTwithitem(pyObject)

            // complex numbers
            "<class 'complex'>" -> TODO()
            else -> {
                TODO("Implement for ${pyObject.getAttr("__class__")}")
            }
        }
    }
}
