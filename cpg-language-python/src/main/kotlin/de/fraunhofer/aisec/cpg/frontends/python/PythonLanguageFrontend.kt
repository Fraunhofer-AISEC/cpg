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
            return pythonASTtoCPG(pyAST, file.name)
        }
    }

    override fun typeOf(type: Any): Type {
        // TODO
        return unknownType()
    }

    override fun codeOf(astNode: PythonAST.AST): String? {
        return null
        // TODO
    }

    override fun locationOf(astNode: PythonAST.AST): PhysicalLocation? {
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

    override fun setComment(node: Node, astNode: PythonAST.AST) {
        // will be invoked by native function
    }

    private fun pythonASTtoCPG(pyAST: PyObject, path: String): TranslationUnitDeclaration {
        val pythonASTModule =
            fromPython(pyAST) as? PythonAST.Module
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
fun fromPython(pyObject: Any?): PythonAST.AST {
    if (pyObject !is PyObject) {
        TODO("Expected a PyObject")
    } else {

        return when (pyObject.getAttr("__class__").toString()) {
            "<class 'ast.Module'>" -> PythonAST.Module(pyObject)

            // statements
            "<class 'ast.FunctionDef'>" -> PythonAST.FunctionDef(pyObject)
            "<class 'ast.AsyncFunctionDef'>" -> PythonAST.AsyncFunctionDef(pyObject)
            "<class 'ast.ClassDef'>" -> PythonAST.ClassDef(pyObject)
            "<class 'ast.Return'>" -> PythonAST.Return(pyObject)
            "<class 'ast.Delete'>" -> PythonAST.Delete(pyObject)
            "<class 'ast.Assign'>" -> PythonAST.Assign(pyObject)
            "<class 'ast.AugAssign'>" -> PythonAST.AugAssign(pyObject)
            "<class 'ast.AnnAssign'>" -> PythonAST.AnnAssign(pyObject)
            "<class 'ast.For'>" -> PythonAST.For(pyObject)
            "<class 'ast.AsyncFor'>" -> PythonAST.AsyncFor(pyObject)
            "<class 'ast.While'>" -> PythonAST.While(pyObject)
            "<class 'ast.If'>" -> PythonAST.If(pyObject)
            "<class 'ast.With'>" -> PythonAST.With(pyObject)
            "<class 'ast.AsyncWith'>" -> PythonAST.AsyncWith(pyObject)
            "<class 'ast.Match'>" -> PythonAST.Match(pyObject)
            "<class 'ast.Raise'>" -> PythonAST.Raise(pyObject)
            "<class 'ast.Try'>" -> PythonAST.Try(pyObject)
            "<class 'ast.TryStar'>" -> PythonAST.TryStar(pyObject)
            "<class 'ast.Assert'>" -> PythonAST.Assert(pyObject)
            "<class 'ast.Import'>" -> PythonAST.Import(pyObject)
            "<class 'ast.ImportFrom'>" -> PythonAST.ImportFrom(pyObject)
            "<class 'ast.Global'>" -> PythonAST.Global(pyObject)
            "<class 'ast.Nonlocal'>" -> PythonAST.Nonlocal(pyObject)
            "<class 'ast.Expr'>" -> PythonAST.Expr(pyObject)
            "<class 'ast.Pass'>" -> PythonAST.Pass(pyObject)
            "<class 'ast.Break'>" -> PythonAST.Break(pyObject)
            "<class 'ast.Continue'>" -> PythonAST.Continue(pyObject)

            // `ast.expr`
            "<class 'ast.BoolOp'>" -> PythonAST.BoolOp(pyObject)
            "<class 'ast.NamedExpr'>" -> PythonAST.NamedExpr(pyObject)
            "<class 'ast.BinOp'>" -> PythonAST.BinOp(pyObject)
            "<class 'ast.UnaryOp'>" -> PythonAST.UnaryOp(pyObject)
            "<class 'ast.Lambda'>" -> PythonAST.Lambda(pyObject)
            "<class 'ast.IfExp'>" -> PythonAST.IfExp(pyObject)
            "<class 'ast.Dict'>" -> PythonAST.Dict(pyObject)
            "<class 'ast.Set'>" -> PythonAST.Set(pyObject)
            "<class 'ast.ListComp'>" -> PythonAST.ListComp(pyObject)
            "<class 'ast.SetComp'>" -> PythonAST.SetComp(pyObject)
            "<class 'ast.DictComp'>" -> PythonAST.DictComp(pyObject)
            "<class 'ast.GeneratorExp'>" -> PythonAST.GeneratorExp(pyObject)
            "<class 'ast.Await'>" -> PythonAST.Await(pyObject)
            "<class 'ast.Yield'>" -> PythonAST.Yield(pyObject)
            "<class 'ast.YieldFrom'>" -> PythonAST.YieldFrom(pyObject)
            "<class 'ast.Compare'>" -> PythonAST.Compare(pyObject)
            "<class 'ast.Call'>" -> PythonAST.Call(pyObject)
            "<class 'ast.FormattedValue'>" -> PythonAST.FormattedValue(pyObject)
            "<class 'ast.JoinedStr'>" -> PythonAST.JoinedStr(pyObject)
            "<class 'ast.Constant'>" -> PythonAST.Constant(pyObject)
            "<class 'ast.Attribute'>" -> PythonAST.Attribute(pyObject)
            "<class 'ast.Subscript'>" -> PythonAST.Subscript(pyObject)
            "<class 'ast.Starred'>" -> PythonAST.Starred(pyObject)
            "<class 'ast.Name'>" -> PythonAST.Name(pyObject)
            "<class 'ast.List'>" -> PythonAST.List(pyObject)
            "<class 'ast.Tuple'>" -> PythonAST.Tuple(pyObject)
            "<class 'ast.Slice'>" -> PythonAST.Slice(pyObject)

            // `ast.boolop`
            "<class 'ast.And'>" -> PythonAST.And(pyObject)
            "<class 'ast.Or'>" -> PythonAST.Or(pyObject)

            // `ast.cmpop`
            "<class 'ast.Eq'>" -> PythonAST.Eq(pyObject)
            "<class 'ast.NotEq'>" -> PythonAST.NotEq(pyObject)
            "<class 'ast.Lt'>" -> PythonAST.Lt(pyObject)
            "<class 'ast.LtE'>" -> PythonAST.LtE(pyObject)
            "<class 'ast.Gt'>" -> PythonAST.Gt(pyObject)
            "<class 'ast.GtE'>" -> PythonAST.GtE(pyObject)
            "<class 'ast.Is'>" -> PythonAST.Is(pyObject)
            "<class 'ast.IsNot'>" -> PythonAST.IsNot(pyObject)
            "<class 'ast.In'>" -> PythonAST.In(pyObject)
            "<class 'ast.NotInt'>" -> PythonAST.NotIn(pyObject)

            // `ast.expr_context`
            "<class 'ast.Load'>" -> PythonAST.Load(pyObject)
            "<class 'ast.Store'>" -> PythonAST.Store(pyObject)
            "<class 'ast.Del'>" -> PythonAST.Del(pyObject)

            // `ast.operator`
            "<class 'ast.Add'>" -> PythonAST.Add(pyObject)
            "<class 'ast.Sub'>" -> PythonAST.Sub(pyObject)
            "<class 'ast.Mult'>" -> PythonAST.Mult(pyObject)
            "<class 'ast.MatMult'>" -> PythonAST.MatMult(pyObject)
            "<class 'ast.Div'>" -> PythonAST.Div(pyObject)
            "<class 'ast.Mod'>" -> PythonAST.Mod(pyObject)
            "<class 'ast.Pow'>" -> PythonAST.Pow(pyObject)
            "<class 'ast.LShift'>" -> PythonAST.LShift(pyObject)
            "<class 'ast.RShift'>" -> PythonAST.RShift(pyObject)
            "<class 'ast.BitOr'>" -> PythonAST.BitOr(pyObject)
            "<class 'ast.BitXor'>" -> PythonAST.BitXor(pyObject)
            "<class 'ast.BitAnd'>" -> PythonAST.BitAnd(pyObject)
            "<class 'ast.FloorDiv'>" -> PythonAST.FloorDiv(pyObject)

            // `ast.pattern`
            "<class 'ast.MatchValue'>" -> PythonAST.MatchValue(pyObject)
            "<class 'ast.MatchSingleton'>" -> PythonAST.MatchSingleton(pyObject)
            "<class 'ast.MatchSequence'>" -> PythonAST.MatchSequence(pyObject)
            "<class 'ast.MatchMapping'>" -> PythonAST.MatchMapping(pyObject)
            "<class 'ast.MatchClass'>" -> PythonAST.MatchClass(pyObject)
            "<class 'ast.MatchStar'>" -> PythonAST.MatchStar(pyObject)
            "<class 'ast.MatchAs'>" -> PythonAST.MatchAs(pyObject)
            "<class 'ast.MatchOr'>" -> PythonAST.MatchOr(pyObject)

            // `ast.unaryop`
            "<class 'ast.Invert'>" -> PythonAST.Invert(pyObject)
            "<class 'ast.Not'>" -> PythonAST.Not(pyObject)
            "<class 'ast.UAdd'>" -> PythonAST.UAdd(pyObject)
            "<class 'ast.USub'>" -> PythonAST.USub(pyObject)

            // misc
            "<class 'ast.alias'>" -> PythonAST.alias(pyObject)
            "<class 'ast.arg'>" -> PythonAST.arg(pyObject)
            "<class 'ast.arguments'>" -> PythonAST.arguments(pyObject)
            "<class 'ast.comprehension'>" -> PythonAST.comprehension(pyObject)
            "<class 'ast.excepthandler'>" -> PythonAST.excepthandler(pyObject)
            "<class 'ast.keyword'>" -> PythonAST.keyword(pyObject)
            "<class 'ast.match_case'>" -> PythonAST.match_case(pyObject)
            "<class 'ast.type_ignore'>" -> PythonAST.type_ignore(pyObject)
            "<class 'ast.withitem'>" -> PythonAST.withitem(pyObject)
            else -> {
                TODO("Implement for ${pyObject.getAttr("__class__")}")
            }
        }
    }
}
