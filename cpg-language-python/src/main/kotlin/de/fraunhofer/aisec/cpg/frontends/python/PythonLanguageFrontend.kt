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
import de.fraunhofer.aisec.cpg.passes.PythonAddDeclarationsPass
import de.fraunhofer.aisec.cpg.passes.order.RegisterExtraPass
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.io.File
import java.net.URI
import jep.python.PyObject
import kotlin.io.path.Path
import kotlin.io.path.nameWithoutExtension

@SupportsParallelParsing(false) // TODO?
@RegisterExtraPass(PythonAddDeclarationsPass::class)
class PythonLanguageFrontend(language: Language<PythonLanguageFrontend>, ctx: TranslationContext) :
    LanguageFrontend<PythonAST.AST, Any>(language, ctx) {
    private val jep = JepSingleton // configure Jep

    // val declarationHandler = DeclarationHandler(this)
    // val specificationHandler = SpecificationHandler(this)
    private var statementHandler = StatementHandler(this)
    internal var expressionHandler = ExpressionHandler(this)

    private var uri2text: HashMap<URI, String> = HashMap()

    @Throws(TranslationException::class)
    override fun parse(file: File): TranslationUnitDeclaration {
        val fileContent = file.readText(Charsets.UTF_8)
        uri2text.putIfAbsent(file.toURI(), fileContent)

        val absolutePath = file.absolutePath
        jep.getInterp().use {
            // TODO: PYTHON VERSION CHECK!
            // TODO: add sanity check to [absolutePath] to avoid code injection

            it.eval("import ast")
            it.eval("import os")
            it.eval("fh = open(\"$absolutePath\", \"r\")")
            it.eval("parsed = ast.parse(fh.read(), filename=\"$absolutePath\", type_comments=True)")

            val pyAST = it.getValue("parsed") as PyObject
            return pythonASTtoCPG(pyAST, file.name, file.toURI())
        }
    }

    override fun typeOf(type: Any): Type {
        // TODO
        return unknownType()
    }

    override fun codeOf(astNode: PythonAST.AST): String? {
        val physicalLocation = locationOf(astNode)
        val fileContent = uri2text[physicalLocation?.artifactLocation?.uri]
        if (physicalLocation != null && fileContent != null) {
            val lines =
                fileContent
                    .split('\n') // TODO
                    .subList(physicalLocation.region.startLine - 1, physicalLocation.region.endLine)
            val mutableLines = lines.toMutableList()

            // remove not needed first characters of all lines (making the assumption, that we are
            // in an intended code block
            for (idx in mutableLines.indices) {
                mutableLines[idx] = mutableLines[idx].substring(physicalLocation.region.startColumn)
            }

            // remove not needed trailing characters of last line
            val lastLineIdx = mutableLines.lastIndex
            val toRemove =
                mutableLines[lastLineIdx].length + physicalLocation.region.startColumn -
                    physicalLocation.region.endColumn
            mutableLines[lastLineIdx] = mutableLines[lastLineIdx].dropLast(toRemove)
            return mutableLines.toString()
        }
        return null
    }

    override fun locationOf(astNode: PythonAST.AST): PhysicalLocation? {
        return if (astNode is PythonAST.WithPythonLocation) {
            PhysicalLocation(
                astNode.uri,
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

    override fun setComment(node: Node, astNode: PythonAST.AST) {
        // will be invoked by native function
    }

    private fun pythonASTtoCPG(
        pyAST: PyObject,
        path: String,
        uri: URI
    ): TranslationUnitDeclaration {
        val pythonASTModule =
            fromPython(pyAST, uri) as? PythonAST.Module
                ?: TODO() // could be one of ast.{Module,Interactive,Expression,FunctionType}
        val tud = newTranslationUnitDeclaration(path, rawNode = pythonASTModule) // TODO
        scopeManager.resetToGlobal(tud)
        val nsdName = Path(path).nameWithoutExtension
        val nsd = newNamespaceDeclaration(nsdName, rawNode = pythonASTModule) // TODO
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
fun fromPython(pyObject: PyObject, uri: URI): PythonAST.AST {

    return when (pyObject.getAttr("__class__").toString()) {
        "<class 'ast.Module'>" -> PythonAST.Module(pyObject, uri)

        // statements
        "<class 'ast.FunctionDef'>" -> PythonAST.FunctionDef(pyObject, uri)
        "<class 'ast.AsyncFunctionDef'>" -> PythonAST.AsyncFunctionDef(pyObject, uri)
        "<class 'ast.ClassDef'>" -> PythonAST.ClassDef(pyObject, uri)
        "<class 'ast.Return'>" -> PythonAST.Return(pyObject, uri)
        "<class 'ast.Delete'>" -> PythonAST.Delete(pyObject, uri)
        "<class 'ast.Assign'>" -> PythonAST.Assign(pyObject, uri)
        "<class 'ast.AugAssign'>" -> PythonAST.AugAssign(pyObject, uri)
        "<class 'ast.AnnAssign'>" -> PythonAST.AnnAssign(pyObject, uri)
        "<class 'ast.For'>" -> PythonAST.For(pyObject, uri)
        "<class 'ast.AsyncFor'>" -> PythonAST.AsyncFor(pyObject, uri)
        "<class 'ast.While'>" -> PythonAST.While(pyObject, uri)
        "<class 'ast.If'>" -> PythonAST.If(pyObject, uri)
        "<class 'ast.With'>" -> PythonAST.With(pyObject, uri)
        "<class 'ast.AsyncWith'>" -> PythonAST.AsyncWith(pyObject, uri)
        "<class 'ast.Match'>" -> PythonAST.Match(pyObject, uri)
        "<class 'ast.Raise'>" -> PythonAST.Raise(pyObject, uri)
        "<class 'ast.Try'>" -> PythonAST.Try(pyObject, uri)
        "<class 'ast.TryStar'>" -> PythonAST.TryStar(pyObject, uri)
        "<class 'ast.Assert'>" -> PythonAST.Assert(pyObject, uri)
        "<class 'ast.Import'>" -> PythonAST.Import(pyObject, uri)
        "<class 'ast.ImportFrom'>" -> PythonAST.ImportFrom(pyObject, uri)
        "<class 'ast.Global'>" -> PythonAST.Global(pyObject, uri)
        "<class 'ast.Nonlocal'>" -> PythonAST.Nonlocal(pyObject, uri)
        "<class 'ast.Expr'>" -> PythonAST.Expr(pyObject, uri)
        "<class 'ast.Pass'>" -> PythonAST.Pass(pyObject, uri)
        "<class 'ast.Break'>" -> PythonAST.Break(pyObject, uri)
        "<class 'ast.Continue'>" -> PythonAST.Continue(pyObject, uri)

        // `ast.expr`
        "<class 'ast.BoolOp'>" -> PythonAST.BoolOp(pyObject, uri)
        "<class 'ast.NamedExpr'>" -> PythonAST.NamedExpr(pyObject, uri)
        "<class 'ast.BinOp'>" -> PythonAST.BinOp(pyObject, uri)
        "<class 'ast.UnaryOp'>" -> PythonAST.UnaryOp(pyObject, uri)
        "<class 'ast.Lambda'>" -> PythonAST.Lambda(pyObject, uri)
        "<class 'ast.IfExp'>" -> PythonAST.IfExp(pyObject, uri)
        "<class 'ast.Dict'>" -> PythonAST.Dict(pyObject, uri)
        "<class 'ast.Set'>" -> PythonAST.Set(pyObject, uri)
        "<class 'ast.ListComp'>" -> PythonAST.ListComp(pyObject, uri)
        "<class 'ast.SetComp'>" -> PythonAST.SetComp(pyObject, uri)
        "<class 'ast.DictComp'>" -> PythonAST.DictComp(pyObject, uri)
        "<class 'ast.GeneratorExp'>" -> PythonAST.GeneratorExp(pyObject, uri)
        "<class 'ast.Await'>" -> PythonAST.Await(pyObject, uri)
        "<class 'ast.Yield'>" -> PythonAST.Yield(pyObject, uri)
        "<class 'ast.YieldFrom'>" -> PythonAST.YieldFrom(pyObject, uri)
        "<class 'ast.Compare'>" -> PythonAST.Compare(pyObject, uri)
        "<class 'ast.Call'>" -> PythonAST.Call(pyObject, uri)
        "<class 'ast.FormattedValue'>" -> PythonAST.FormattedValue(pyObject, uri)
        "<class 'ast.JoinedStr'>" -> PythonAST.JoinedStr(pyObject, uri)
        "<class 'ast.Constant'>" -> PythonAST.Constant(pyObject, uri)
        "<class 'ast.Attribute'>" -> PythonAST.Attribute(pyObject, uri)
        "<class 'ast.Subscript'>" -> PythonAST.Subscript(pyObject, uri)
        "<class 'ast.Starred'>" -> PythonAST.Starred(pyObject, uri)
        "<class 'ast.Name'>" -> PythonAST.Name(pyObject, uri)
        "<class 'ast.List'>" -> PythonAST.List(pyObject, uri)
        "<class 'ast.Tuple'>" -> PythonAST.Tuple(pyObject, uri)
        "<class 'ast.Slice'>" -> PythonAST.Slice(pyObject, uri)

        // `ast.boolop`
        "<class 'ast.And'>" -> PythonAST.And(pyObject, uri)
        "<class 'ast.Or'>" -> PythonAST.Or(pyObject, uri)

        // `ast.cmpop`
        "<class 'ast.Eq'>" -> PythonAST.Eq(pyObject, uri)
        "<class 'ast.NotEq'>" -> PythonAST.NotEq(pyObject, uri)
        "<class 'ast.Lt'>" -> PythonAST.Lt(pyObject, uri)
        "<class 'ast.LtE'>" -> PythonAST.LtE(pyObject, uri)
        "<class 'ast.Gt'>" -> PythonAST.Gt(pyObject, uri)
        "<class 'ast.GtE'>" -> PythonAST.GtE(pyObject, uri)
        "<class 'ast.Is'>" -> PythonAST.Is(pyObject, uri)
        "<class 'ast.IsNot'>" -> PythonAST.IsNot(pyObject, uri)
        "<class 'ast.In'>" -> PythonAST.In(pyObject, uri)
        "<class 'ast.NotInt'>" -> PythonAST.NotIn(pyObject, uri)

        // `ast.expr_context`
        "<class 'ast.Load'>" -> PythonAST.Load(pyObject, uri)
        "<class 'ast.Store'>" -> PythonAST.Store(pyObject, uri)
        "<class 'ast.Del'>" -> PythonAST.Del(pyObject, uri)

        // `ast.operator`
        "<class 'ast.Add'>" -> PythonAST.Add(pyObject, uri)
        "<class 'ast.Sub'>" -> PythonAST.Sub(pyObject, uri)
        "<class 'ast.Mult'>" -> PythonAST.Mult(pyObject, uri)
        "<class 'ast.MatMult'>" -> PythonAST.MatMult(pyObject, uri)
        "<class 'ast.Div'>" -> PythonAST.Div(pyObject, uri)
        "<class 'ast.Mod'>" -> PythonAST.Mod(pyObject, uri)
        "<class 'ast.Pow'>" -> PythonAST.Pow(pyObject, uri)
        "<class 'ast.LShift'>" -> PythonAST.LShift(pyObject, uri)
        "<class 'ast.RShift'>" -> PythonAST.RShift(pyObject, uri)
        "<class 'ast.BitOr'>" -> PythonAST.BitOr(pyObject, uri)
        "<class 'ast.BitXor'>" -> PythonAST.BitXor(pyObject, uri)
        "<class 'ast.BitAnd'>" -> PythonAST.BitAnd(pyObject, uri)
        "<class 'ast.FloorDiv'>" -> PythonAST.FloorDiv(pyObject, uri)

        // `ast.pattern`
        "<class 'ast.MatchValue'>" -> PythonAST.MatchValue(pyObject, uri)
        "<class 'ast.MatchSingleton'>" -> PythonAST.MatchSingleton(pyObject, uri)
        "<class 'ast.MatchSequence'>" -> PythonAST.MatchSequence(pyObject, uri)
        "<class 'ast.MatchMapping'>" -> PythonAST.MatchMapping(pyObject, uri)
        "<class 'ast.MatchClass'>" -> PythonAST.MatchClass(pyObject, uri)
        "<class 'ast.MatchStar'>" -> PythonAST.MatchStar(pyObject, uri)
        "<class 'ast.MatchAs'>" -> PythonAST.MatchAs(pyObject, uri)
        "<class 'ast.MatchOr'>" -> PythonAST.MatchOr(pyObject, uri)

        // `ast.unaryop`
        "<class 'ast.Invert'>" -> PythonAST.Invert(pyObject, uri)
        "<class 'ast.Not'>" -> PythonAST.Not(pyObject, uri)
        "<class 'ast.UAdd'>" -> PythonAST.UAdd(pyObject, uri)
        "<class 'ast.USub'>" -> PythonAST.USub(pyObject, uri)

        // misc
        "<class 'ast.alias'>" -> PythonAST.alias(pyObject, uri)
        "<class 'ast.arg'>" -> PythonAST.arg(pyObject, uri)
        "<class 'ast.arguments'>" -> PythonAST.arguments(pyObject, uri)
        "<class 'ast.comprehension'>" -> PythonAST.comprehension(pyObject, uri)
        "<class 'ast.excepthandler'>" -> PythonAST.excepthandler(pyObject, uri)
        "<class 'ast.keyword'>" -> PythonAST.keyword(pyObject, uri)
        "<class 'ast.match_case'>" -> PythonAST.match_case(pyObject, uri)
        "<class 'ast.type_ignore'>" -> PythonAST.type_ignore(pyObject, uri)
        "<class 'ast.withitem'>" -> PythonAST.withitem(pyObject, uri)
        else -> {
            TODO("Implement for ${pyObject.getAttr("__class__")}")
        }
    }
}
