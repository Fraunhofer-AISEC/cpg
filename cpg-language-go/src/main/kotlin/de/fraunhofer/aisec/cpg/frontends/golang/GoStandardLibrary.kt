/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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

import com.sun.jna.*
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import java.io.FileOutputStream

/**
 * This interface represents parts of the Go standard library used by C wrapper functions and JNA.
 */
interface GoStandardLibrary : Library {
    open class GoObject(p: Pointer? = Pointer.NULL) : PointerType(p) {
        val goType: String
            get() {
                return INSTANCE.GetType(this.pointer)
            }
    }

    /**
     * This class represents the Go `go/token` package and contains classes representing structs in
     * this package.
     */
    class Token {}

    /**
     * This class represents the Go `go/parser` package and contains classes representing structs in
     * this package.
     */
    object Parser {
        fun parseFile(fileSet: Ast.FileSet, path: String): Ast.File {
            return INSTANCE.goParserParseFile(fileSet, path)
        }
    }

    object Modfile {
        class File(p: Pointer? = Pointer.NULL) : GoObject(p) {
            val module: Module by lazy { INSTANCE.modfileGetFileModule(this) }
        }

        class Module(p: Pointer? = Pointer.NULL) : GoObject(p) {
            val mod: GoStandardLibrary.Module.Version by lazy { INSTANCE.modfileGetModuleMod(this) }
        }

        fun parse(file: String, bytes: String): File {
            return INSTANCE.modfileParse(file, bytes)
        }
    }

    object Module {

        class Version(p: Pointer? = Pointer.NULL) : GoObject(p) {
            val path: String by lazy { INSTANCE.moduleGetVersionPath(this) }
        }
    }

    fun modfileParse(file: String, bytes: String): Modfile.File

    fun modfileGetFileModule(file: Modfile.File): Modfile.Module

    fun modfileGetModuleMod(file: Modfile.Module): Module.Version

    fun moduleGetVersionPath(version: Module.Version): String

    /**
     * This class represents the Go go/ast package and contains classes representing structs in this
     * package.
     */
    interface Ast {
        open class Node(p: Pointer? = Pointer.NULL) : GoObject(p) {
            val pos: Int by lazy { INSTANCE.GetNodePos(this) }

            val end: Int by lazy { INSTANCE.GetNodeEnd(this) }
        }

        class FieldList(p: Pointer? = Pointer.NULL) : Node(p) {
            val list: List<Field> by lazy {
                list(INSTANCE::GetNumFieldListList, INSTANCE::GetFieldListList)
            }
        }

        class Field(p: Pointer? = Pointer.NULL) : Node(p) {
            val names: List<Ident> by lazy {
                list(INSTANCE::GetNumFieldNames, INSTANCE::GetFieldName)
            }

            val type: Expr by lazy { INSTANCE.GetFieldType(this) }
        }

        open class Decl(p: Pointer? = Pointer.NULL) : Node(p) {

            override fun fromNative(nativeValue: Any?, context: FromNativeContext?): Any {
                if (nativeValue !is Pointer) {
                    return super.fromNative(nativeValue, context)
                }

                return when (INSTANCE.GetType(nativeValue)) {
                    "*ast.GenDecl" -> {
                        GenDecl(nativeValue)
                    }
                    "*ast.FuncDecl" -> {
                        FuncDecl(nativeValue)
                    }
                    else -> {
                        super.fromNative(nativeValue, context)
                    }
                }
            }
        }

        class GenDecl(p: Pointer? = Pointer.NULL) : Decl(p) {
            val specs: List<Spec> by lazy {
                list(INSTANCE::GetNumGenDeclSpecs, INSTANCE::GetGenDeclSpec)
            }

            val tok: Int by lazy { INSTANCE.GetGenDeclTok(this) }
        }

        class FuncDecl(p: Pointer? = Pointer.NULL) : Decl(p) {
            val recv: FieldList? by lazy { INSTANCE.GetFuncDeclRecv(this) }

            val type: FuncType by lazy { INSTANCE.GetFuncDeclType(this) }

            val name: Ident by lazy { INSTANCE.GetFuncDeclName(this) }

            val body: BlockStmt? by lazy { INSTANCE.GetFuncDeclBody(this) }
        }

        open class Spec(p: Pointer? = Pointer.NULL) : Node(p) {
            override fun fromNative(nativeValue: Any?, context: FromNativeContext?): Any {
                if (nativeValue !is Pointer) {
                    return super.fromNative(nativeValue, context)
                }

                return when (INSTANCE.GetType(nativeValue)) {
                    "*ast.ImportSpec" -> ImportSpec(nativeValue)
                    "*ast.TypeSpec" -> TypeSpec(nativeValue)
                    "*ast.ValueSpec" -> ValueSpec(nativeValue)
                    else -> super.fromNative(nativeValue, context)
                }
            }
        }

        class TypeSpec(p: Pointer? = Pointer.NULL) : Spec(p) {
            val name: Ident by lazy { INSTANCE.GetTypeSpecName(this) }

            val assign: Int by lazy { INSTANCE.GetTypeSpecAssign(this) }

            val type: Expr by lazy { INSTANCE.GetTypeSpecType(this) }
        }

        class ImportSpec(p: Pointer? = Pointer.NULL) : Spec(p) {
            val name: Ident? by lazy { INSTANCE.GetImportSpecName(this) }

            val path: BasicLit by lazy { INSTANCE.GetImportSpecPath(this) }
        }

        class ValueSpec(p: Pointer? = Pointer.NULL) : Spec(p) {
            val names: List<Ident> by lazy {
                list(INSTANCE::GetNumValueSpecNames, INSTANCE::GetValueSpecName)
            }

            val type: Expr? by lazy { INSTANCE.GetValueSpecType(this) }

            val values: List<Expr> by lazy {
                list(INSTANCE::GetNumValueSpecValues, INSTANCE::GetValueSpecValue)
            }
        }

        open class Expr(p: Pointer? = Pointer.NULL) : Node(p) {
            override fun fromNative(nativeValue: Any?, context: FromNativeContext?): Any? {
                if (nativeValue !is Pointer) {
                    return super.fromNative(nativeValue, context)
                }

                return when (INSTANCE.GetType(nativeValue)) {
                    "*ast.BasicLit" -> BasicLit(nativeValue)
                    "*ast.BinaryExpr" -> BinaryExpr(nativeValue)
                    "*ast.CallExpr" -> CallExpr(nativeValue)
                    "*ast.CompositeLit" -> CompositeLit(nativeValue)
                    "*ast.Ellipsis" -> Ellipsis(nativeValue)
                    "*ast.FuncLit" -> FuncLit(nativeValue)
                    "*ast.Ident" -> Ident(nativeValue)
                    "*ast.IndexExpr" -> IndexExpr(nativeValue)
                    "*ast.IndexListExpr" -> IndexListExpr(nativeValue)
                    "*ast.KeyValueExpr" -> KeyValueExpr(nativeValue)
                    "*ast.ParenExpr" -> ParenExpr(nativeValue)
                    "*ast.SelectorExpr" -> SelectorExpr(nativeValue)
                    "*ast.StarExpr" -> StarExpr(nativeValue)
                    "*ast.SliceExpr" -> SliceExpr(nativeValue)
                    "*ast.TypeAssertExpr" -> TypeAssertExpr(nativeValue)
                    "*ast.UnaryExpr" -> UnaryExpr(nativeValue)
                    "*ast.ArrayType" -> ArrayType(nativeValue)
                    "*ast.ChanType" -> ChanType(nativeValue)
                    "*ast.FuncType" -> FuncType(nativeValue)
                    "*ast.InterfaceType" -> InterfaceType(nativeValue)
                    "*ast.MapType" -> MapType(nativeValue)
                    "*ast.StructType" -> StructType(nativeValue)
                    else -> super.fromNative(nativeValue, context)
                }
            }
        }

        class BasicLit(p: Pointer? = Pointer.NULL) : Expr(p) {

            enum class Kind(val i: Int) {
                IDENT(4),
                INT(5),
                FLOAT(6),
                IMAG(7),
                CHAR(8),
                STRING(9),
            }

            val value: String by lazy { INSTANCE.GetBasicLitValue(this) }

            val kind: Kind by lazy { Kind.entries.first { it.i == INSTANCE.GetBasicLitKind(this) } }
        }

        class BinaryExpr(p: Pointer? = Pointer.NULL) : Expr(p) {
            val x: Expr by lazy { INSTANCE.GetBinaryExprX(this) }

            val opString: String by lazy { INSTANCE.GetBinaryExprOpString(this) }

            val y: Expr by lazy { INSTANCE.GetBinaryExprY(this) }
        }

        class CallExpr(p: Pointer? = Pointer.NULL) : Expr(p) {
            val args: List<Expr> by lazy {
                list(INSTANCE::GetNumCallExprArgs, INSTANCE::GetCallExprArg)
            }

            val `fun`: Expr by lazy { INSTANCE.GetCallExprFun(this) }
        }

        class CompositeLit(p: Pointer? = Pointer.NULL) : Expr(p) {
            val type: Expr? by lazy { INSTANCE.GetCompositeLitType(this) }

            val elts: List<Expr> by lazy {
                list(INSTANCE::GetNumCompositeLitElts, INSTANCE::GetCompositeLitElt)
            }
        }

        class KeyValueExpr(p: Pointer? = Pointer.NULL) : Expr(p) {
            val key: Expr by lazy { INSTANCE.GetKeyValueExprKey(this) }

            val value: Expr by lazy { INSTANCE.GetKeyValueExprValue(this) }
        }

        class ParenExpr(p: Pointer? = Pointer.NULL) : Expr(p) {
            val x: Expr by lazy { INSTANCE.GetParenExprX(this) }
        }

        class FuncLit(p: Pointer? = Pointer.NULL) : Expr(p) {
            fun toDecl(): FuncDecl {
                return INSTANCE.MakeFuncDeclFromFuncLit(this)
            }
        }

        class Ellipsis(p: Pointer? = Pointer.NULL) : Expr(p) {
            val elt: Expr by lazy { INSTANCE.GetEllipsisElt(this) }
        }

        class Ident(p: Pointer? = Pointer.NULL) : Expr(p) {
            val isUnexported: Boolean by lazy { name.isNotEmpty() && name[0].isLowerCase() }

            val name: String by lazy { INSTANCE.GetIdentName(this) }

            override fun toString(): String {
                return name
            }
        }

        class IndexExpr(p: Pointer? = Pointer.NULL) : Expr(p) {
            val x: Expr by lazy { INSTANCE.GetIndexExprX(this) }

            val index: Expr by lazy { INSTANCE.GetIndexExprIndex(this) }
        }

        class IndexListExpr(p: Pointer? = Pointer.NULL) : Expr(p) {
            val x: Expr by lazy { INSTANCE.GetIndexListExprX(this) }

            val indices: List<Expr> by lazy {
                list(INSTANCE::GetNumIndexListExprIndices, INSTANCE::GetIndexListExprIndex)
            }
        }

        class SelectorExpr(p: Pointer? = Pointer.NULL) : Expr(p) {
            val x: Expr by lazy { INSTANCE.GetSelectorExprX(this) }

            val sel: Ident by lazy { INSTANCE.GetSelectorExprSel(this) }
        }

        class StarExpr(p: Pointer? = Pointer.NULL) : Expr(p) {
            val x: Expr by lazy { INSTANCE.GetStarExprX(this) }
        }

        class SliceExpr(p: Pointer? = Pointer.NULL) : Expr(p) {
            val x: Expr by lazy { INSTANCE.GetSliceExprX(this) }

            val low: Expr? by lazy { INSTANCE.GetSliceExprLow(this) }

            val high: Expr? by lazy { INSTANCE.GetSliceExprHigh(this) }

            val max: Expr? by lazy { INSTANCE.GetSliceExprMax(this) }
        }

        class TypeAssertExpr(p: Pointer? = Pointer.NULL) : Expr(p) {
            val x: Expr by lazy { INSTANCE.GetTypeAssertExprX(this) }

            val type: Expr? by lazy { INSTANCE.GetTypeAssertExprType(this) }
        }

        class UnaryExpr(p: Pointer? = Pointer.NULL) : Expr(p) {
            val opString: String by lazy { INSTANCE.GetUnaryExprOpString(this) }

            val x: Expr by lazy { INSTANCE.GetUnaryExprX(this) }
        }

        class ArrayType(p: Pointer? = Pointer.NULL) : Expr(p) {
            val elt: Expr by lazy { INSTANCE.GetArrayTypeElt(this) }
        }

        class ChanType(p: Pointer? = Pointer.NULL) : Expr(p) {
            val value: Expr by lazy { INSTANCE.GetChanTypeValue(this) }
        }

        class InterfaceType(p: Pointer? = Pointer.NULL) : Expr(p) {
            val methods: FieldList by lazy { INSTANCE.GetInterfaceTypeMethods(this) }

            val incomplete: Boolean by lazy { INSTANCE.GetInterfaceTypeIncomplete(this) }
        }

        class FuncType(p: Pointer? = Pointer.NULL) : Expr(p) {
            val typeParams: FieldList? by lazy { INSTANCE.GetFuncTypeTypeParams(this) }

            val params: FieldList by lazy { INSTANCE.GetFuncTypeParams(this) }

            val results: FieldList? by lazy { INSTANCE.GetFuncTypeResults(this) }
        }

        class MapType(p: Pointer? = Pointer.NULL) : Expr(p) {
            val key: Expr by lazy { INSTANCE.GetMapTypeKey(this) }

            val value: Expr by lazy { INSTANCE.GetMapTypeValue(this) }
        }

        class StructType(p: Pointer? = Pointer.NULL) : Expr(p) {
            val fields: FieldList by lazy { INSTANCE.GetStructTypeFields(this) }

            val incomplete: Boolean by lazy { INSTANCE.GetStructTypeIncomplete(this) }
        }

        open class Stmt(p: Pointer? = Pointer.NULL) : Node(p) {
            override fun fromNative(nativeValue: Any?, context: FromNativeContext?): Any? {
                if (nativeValue !is Pointer) {
                    return super.fromNative(nativeValue, context)
                }

                return when (INSTANCE.GetType(nativeValue)) {
                    "*ast.AssignStmt" -> AssignStmt(nativeValue)
                    "*ast.BlockStmt" -> BlockStmt(nativeValue)
                    "*ast.BranchStmt" -> BranchStmt(nativeValue)
                    "*ast.CaseClause" -> CaseClause(nativeValue)
                    "*ast.DeferStmt" -> DeferStmt(nativeValue)
                    "*ast.DeclStmt" -> DeclStmt(nativeValue)
                    "*ast.ExprStmt" -> ExprStmt(nativeValue)
                    "*ast.GoStmt" -> GoStmt(nativeValue)
                    "*ast.ForStmt" -> ForStmt(nativeValue)
                    "*ast.IfStmt" -> IfStmt(nativeValue)
                    "*ast.IncDecStmt" -> IncDecStmt(nativeValue)
                    "*ast.LabeledStmt" -> LabeledStmt(nativeValue)
                    "*ast.RangeStmt" -> RangeStmt(nativeValue)
                    "*ast.ReturnStmt" -> ReturnStmt(nativeValue)
                    "*ast.SendStmt" -> SendStmt(nativeValue)
                    "*ast.SwitchStmt" -> SwitchStmt(nativeValue)
                    "*ast.TypeSwitchStmt" -> TypeSwitchStmt(nativeValue)
                    else -> super.fromNative(nativeValue, context)
                }
            }
        }

        class AssignStmt(p: Pointer? = Pointer.NULL) : Stmt(p) {
            val lhs: List<Expr> by lazy {
                this.list(INSTANCE::GetNumAssignStmtLhs, INSTANCE::GetAssignStmtLhs)
            }

            val tok: Int by lazy { INSTANCE.GetAssignStmtTok(this) }

            val rhs: List<Expr> by lazy {
                this.list(INSTANCE::GetNumAssignStmtRhs, INSTANCE::GetAssignStmtRhs)
            }
        }

        class BranchStmt(p: Pointer? = Pointer.NULL) : Stmt(p) {

            val tokString: String by lazy { INSTANCE.GetBranchStmtTokString(this) }

            val label: Ident? by lazy { INSTANCE.GetBranchStmtLabel(this) }
        }

        class BlockStmt(p: Pointer? = Pointer.NULL) : Stmt(p) {
            val list: List<Stmt> by lazy {
                this.list(INSTANCE::GetNumBlockStmtList, INSTANCE::GetBlockStmtList)
            }
        }

        class CaseClause(p: Pointer? = Pointer.NULL) : Stmt(p) {
            val list: List<Expr> by lazy {
                this.list(INSTANCE::GetNumCaseClauseList, INSTANCE::GetCaseClauseList)
            }

            val body: List<Stmt> by lazy {
                this.list(INSTANCE::GetNumCaseClauseBody, INSTANCE::GetCaseClauseBody)
            }
        }

        class DeclStmt(p: Pointer? = Pointer.NULL) : Stmt(p) {
            val decl: Decl by lazy { INSTANCE.GetDeclStmtDecl(this) }
        }

        class DeferStmt(p: Pointer? = Pointer.NULL) : Stmt(p) {
            val call: Expr by lazy { INSTANCE.GetDeferStmtCall(this) }
        }

        class ExprStmt(p: Pointer? = Pointer.NULL) : Stmt(p) {
            val x: Expr by lazy { INSTANCE.GetExprStmtX(this) }
        }

        class IfStmt(p: Pointer? = Pointer.NULL) : Stmt(p) {
            val init: Stmt? by lazy { INSTANCE.GetIfStmtInit(this) }

            val cond: Expr by lazy { INSTANCE.GetIfStmtCond(this) }

            val body: BlockStmt by lazy { INSTANCE.GetIfStmtBody(this) }

            val `else`: Stmt? by lazy { INSTANCE.GetIfStmtElse(this) }
        }

        class ForStmt(p: Pointer? = Pointer.NULL) : Stmt(p) {
            val init: Stmt? by lazy { INSTANCE.GetForStmtInit(this) }

            val cond: Expr? by lazy { INSTANCE.GetForStmtCond(this) }

            val post: Stmt? by lazy { INSTANCE.GetForStmtPost(this) }

            val body: BlockStmt? by lazy { INSTANCE.GetForStmtBody(this) }
        }

        class GoStmt(p: Pointer? = Pointer.NULL) : Stmt(p) {
            val call: Expr by lazy { INSTANCE.GetGoStmtCall(this) }
        }

        class IncDecStmt(p: Pointer? = Pointer.NULL) : Stmt(p) {
            val tokString: String by lazy { INSTANCE.GetIncDecStmtTokString(this) }

            val x: Expr by lazy { INSTANCE.GetIncDecStmtX(this) }
        }

        class LabeledStmt(p: Pointer? = Pointer.NULL) : Stmt(p) {

            val label: Ident by lazy { INSTANCE.GetLabeledStmtLabel(this) }

            val stmt: Stmt by lazy { INSTANCE.GetLabeledStmtStmt(this) }
        }

        class RangeStmt(p: Pointer? = Pointer.NULL) : Stmt(p) {
            val tokString: String by lazy { INSTANCE.GetRangeStmtTokString(this) }

            val key: Expr? by lazy { INSTANCE.GetRangeStmtKey(this) }

            val value: Expr? by lazy { INSTANCE.GetRangeStmtValue(this) }

            val x: Expr by lazy { INSTANCE.GetRangeStmtX(this) }

            val body: BlockStmt by lazy { INSTANCE.GetRangeStmtBody(this) }
        }

        class ReturnStmt(p: Pointer? = Pointer.NULL) : Stmt(p) {
            val results: List<Expr> by lazy {
                list(INSTANCE::GetNumReturnStmtResults, INSTANCE::GetReturnStmtResult)
            }
        }

        class SendStmt(p: Pointer? = Pointer.NULL) : Stmt(p) {
            val chan: Expr by lazy { INSTANCE.GetSendStmtChan(this) }

            val value: Expr by lazy { INSTANCE.GetSendStmtValue(this) }
        }

        class SwitchStmt(p: Pointer? = Pointer.NULL) : Stmt(p) {
            val init: Stmt? by lazy { INSTANCE.GetSwitchStmtInit(this) }

            val tag: Expr? by lazy { INSTANCE.GetSwitchStmtTag(this) }

            val body: BlockStmt by lazy { INSTANCE.GetSwitchStmtBody(this) }
        }

        class TypeSwitchStmt(p: Pointer? = Pointer.NULL) : Stmt(p) {
            val init: Stmt? by lazy { INSTANCE.GetTypeSwitchStmtInit(this) }

            val assign: Stmt by lazy { INSTANCE.GetTypeSwitchStmtAssign(this) }

            val body: BlockStmt by lazy { INSTANCE.GetTypeSwitchStmtBody(this) }
        }

        class Position(p: Pointer? = Pointer.NULL) : GoObject(p) {
            val line: Int by lazy { INSTANCE.GetPositionLine(this) }

            val column: Int by lazy { INSTANCE.GetPositionColumn(this) }
        }

        class FileSet(p: Pointer? = Pointer.NULL) : GoObject(p) {
            fun position(pos: Int): Position {
                return INSTANCE.GetFileSetPosition(this, pos)
            }

            fun fileName(pos: Int): String? {
                return INSTANCE.GetFileSetFileName(this, pos)
            }

            fun code(astNode: Node): String? {
                return INSTANCE.GetFileSetNodeCode(this, astNode)
            }
        }

        class CommentMap(p: Pointer? = Pointer.NULL) : GoObject(p) {
            fun comment(node: Node): String? {
                return INSTANCE.GetCommentMapNodeComment(this, node)
            }
        }

        class File(p: Pointer? = Pointer.NULL) : Node(p) {
            val comments: Pointer by lazy { INSTANCE.GetFileComments(this) }

            val imports: List<ImportSpec> by lazy {
                list(INSTANCE::GetNumFileImports, INSTANCE::GetFileImport)
            }

            val decls: List<Decl> by lazy {
                this.list(INSTANCE::GetNumFileDecls, INSTANCE::GetFileDecl)
            }

            val name: Ident by lazy { INSTANCE.GetFileName(this) }
        }
    }

    // go/parser package

    fun goParserParseFile(fileSet: Ast.FileSet, path: String): Ast.File

    fun GetType(obj: Pointer): String

    fun GetNodePos(node: Ast.Node): Int

    fun GetNodeEnd(node: Ast.Node): Int

    fun NewFileSet(): Ast.FileSet

    fun NewCommentMap(fset: Ast.FileSet, file: Ast.File, comments: Any): Ast.CommentMap

    fun GetCommentMapNodeComment(commentMap: Ast.CommentMap, node: Ast.Node): String?

    fun GetFileName(file: Ast.File): Ast.Ident

    fun GetFieldType(field: Ast.Field): Ast.Expr

    fun GetNumFieldListList(fieldList: Ast.FieldList): Int

    fun GetFieldListList(fieldList: Ast.FieldList, i: Int): Ast.Field

    fun GetNumFieldNames(field: Ast.Field): Int

    fun GetFieldName(field: Ast.Field, i: Int): Ast.Ident

    fun GetNumFileImports(file: Ast.File): Int

    fun GetPositionLine(position: Ast.Position): Int

    fun GetPositionColumn(position: Ast.Position): Int

    fun GetFileSetPosition(fileSet: Ast.FileSet, pos: Int): Ast.Position

    fun GetFileSetFileName(fileSet: Ast.FileSet, pos: Int): String?

    fun GetFileSetNodeCode(fileSet: Ast.FileSet, node: Ast.Node): String?

    fun GetFileComments(file: Ast.File): Pointer

    fun GetFileImport(file: Ast.File, i: Int): Ast.ImportSpec

    fun GetNumFileDecls(file: Ast.File): Int

    fun GetFileDecl(file: Ast.File, i: Int): Ast.Decl

    fun GetFuncDeclRecv(funcDecl: Ast.FuncDecl): Ast.FieldList?

    fun GetFuncDeclType(funcDecl: Ast.FuncDecl): Ast.FuncType

    fun GetFuncDeclName(funcDecl: Ast.FuncDecl): Ast.Ident

    fun GetFuncDeclBody(funcDecl: Ast.FuncDecl): Ast.BlockStmt?

    fun GetCompositeLitType(compositeLit: Ast.CompositeLit): Ast.Expr?

    fun GetNumCompositeLitElts(compositeLit: Ast.CompositeLit): Int

    fun GetCompositeLitElt(compositeLit: Ast.CompositeLit, i: Int): Ast.Expr

    fun MakeFuncDeclFromFuncLit(funcLit: Ast.FuncLit): Ast.FuncDecl

    fun GetEllipsisElt(ellipsis: Ast.Ellipsis): Ast.Expr

    fun GetIdentName(ident: Ast.Ident): String

    fun GetKeyValueExprKey(keyValueExpr: Ast.KeyValueExpr): Ast.Expr

    fun GetKeyValueExprValue(keyValueExpr: Ast.KeyValueExpr): Ast.Expr

    fun GetParenExprX(parenExpr: Ast.ParenExpr): Ast.Expr

    fun GetBasicLitValue(basicLit: Ast.BasicLit): String

    fun GetBasicLitKind(basicLit: Ast.BasicLit): Int

    fun GetBinaryExprOpString(binaryExpr: Ast.BinaryExpr): String

    fun GetBinaryExprX(binaryExpr: Ast.BinaryExpr): Ast.Expr

    fun GetBinaryExprY(binaryExpr: Ast.BinaryExpr): Ast.Expr

    fun GetCallExprFun(callExpr: Ast.CallExpr): Ast.Expr

    fun GetNumCallExprArgs(callExpr: Ast.CallExpr): Int

    fun GetCallExprArg(callExpr: Ast.CallExpr, i: Int): Ast.Expr

    fun GetSelectorExprSel(selectorExpr: Ast.SelectorExpr): Ast.Ident

    fun GetSelectorExprX(selectorExpr: Ast.SelectorExpr): Ast.Expr

    fun GetStarExprX(starExpr: Ast.StarExpr): Ast.Expr

    fun GetSliceExprX(sliceExpr: Ast.SliceExpr): Ast.Expr

    fun GetSliceExprLow(sliceExpr: Ast.SliceExpr): Ast.Expr?

    fun GetSliceExprHigh(sliceExpr: Ast.SliceExpr): Ast.Expr?

    fun GetSliceExprMax(sliceExpr: Ast.SliceExpr): Ast.Expr?

    fun GetTypeAssertExprX(typeAssertExpr: Ast.TypeAssertExpr): Ast.Expr

    fun GetTypeAssertExprType(typeAssertExpr: Ast.TypeAssertExpr): Ast.Expr?

    fun GetUnaryExprOpString(unaryExpr: Ast.UnaryExpr): String

    fun GetUnaryExprX(unaryExpr: Ast.UnaryExpr): Ast.Expr

    fun GetArrayTypeElt(arrayType: Ast.ArrayType): Ast.Expr

    fun GetChanTypeValue(chanType: Ast.ChanType): Ast.Expr

    fun GetInterfaceTypeMethods(interfaceType: Ast.InterfaceType): Ast.FieldList

    fun GetInterfaceTypeIncomplete(interfaceType: Ast.InterfaceType): Boolean

    fun GetFuncTypeTypeParams(funcType: Ast.FuncType): Ast.FieldList?

    fun GetFuncTypeParams(funcType: Ast.FuncType): Ast.FieldList

    fun GetFuncTypeResults(funcType: Ast.FuncType): Ast.FieldList?

    fun GetMapTypeKey(mapType: Ast.MapType): Ast.Expr

    fun GetMapTypeValue(mapType: Ast.MapType): Ast.Expr

    fun GetStructTypeFields(structType: Ast.StructType): Ast.FieldList

    fun GetStructTypeIncomplete(structType: Ast.StructType): Boolean

    fun GetAssignStmtTok(assignStmt: Ast.AssignStmt): Int

    fun GetNumAssignStmtLhs(assignStmt: Ast.AssignStmt): Int

    fun GetAssignStmtLhs(assignStmt: Ast.AssignStmt, i: Int): Ast.Expr

    fun GetNumAssignStmtRhs(assignStmt: Ast.AssignStmt): Int

    fun GetAssignStmtRhs(assignStmt: Ast.AssignStmt, i: Int): Ast.Expr

    fun GetNumBlockStmtList(blockStmt: Ast.BlockStmt): Int

    fun GetBlockStmtList(blockStmt: Ast.BlockStmt, i: Int): Ast.Stmt

    fun GetBranchStmtTokString(branchStmt: Ast.BranchStmt): String

    fun GetBranchStmtLabel(branchStmt: Ast.BranchStmt): Ast.Ident?

    fun GetNumCaseClauseList(caseClause: Ast.CaseClause): Int

    fun GetCaseClauseList(caseClause: Ast.CaseClause, i: Int): Ast.Expr

    fun GetNumCaseClauseBody(caseClause: Ast.CaseClause): Int

    fun GetCaseClauseBody(caseClause: Ast.CaseClause, i: Int): Ast.Stmt

    fun GetDeclStmtDecl(declStmt: Ast.DeclStmt): Ast.Decl

    fun GetExprStmtX(exprStmt: Ast.ExprStmt): Ast.Expr

    fun GetDeferStmtCall(deferStmt: Ast.DeferStmt): Ast.Expr

    fun GetForStmtInit(forStmt: Ast.ForStmt): Ast.Stmt?

    fun GetForStmtCond(forStmt: Ast.ForStmt): Ast.Expr?

    fun GetForStmtPost(forStmt: Ast.ForStmt): Ast.Stmt?

    fun GetForStmtBody(forStmt: Ast.ForStmt): Ast.BlockStmt?

    fun GetGoStmtCall(goStmt: Ast.GoStmt): Ast.Expr

    fun GetIncDecStmtTokString(incDecStmt: Ast.IncDecStmt): String

    fun GetIncDecStmtX(incDecStmt: Ast.IncDecStmt): Ast.Expr

    fun GetLabeledStmtLabel(labeledStmt: Ast.LabeledStmt): Ast.Ident

    fun GetLabeledStmtStmt(labeledStmt: Ast.LabeledStmt): Ast.Stmt

    fun GetIndexExprX(IndexExpr: Ast.IndexExpr): Ast.Expr

    fun GetIndexExprIndex(IndexExpr: Ast.IndexExpr): Ast.Expr

    fun GetIndexListExprX(indexListExpr: Ast.IndexListExpr): Ast.Expr

    fun GetNumIndexListExprIndices(indexListExpr: Ast.IndexListExpr): Int

    fun GetIndexListExprIndex(indexListExpr: Ast.IndexListExpr, i: Int): Ast.Expr

    fun GetIfStmtInit(ifStmt: Ast.IfStmt): Ast.Stmt?

    fun GetIfStmtCond(ifStmt: Ast.IfStmt): Ast.Expr

    fun GetIfStmtBody(ifStmt: Ast.IfStmt): Ast.BlockStmt

    fun GetIfStmtElse(ifStmt: Ast.IfStmt): Ast.Stmt?

    fun GetRangeStmtTokString(rangeStmt: Ast.RangeStmt): String

    fun GetRangeStmtKey(rangeStmt: Ast.RangeStmt): Ast.Expr?

    fun GetRangeStmtValue(rangeStmt: Ast.RangeStmt): Ast.Expr?

    fun GetRangeStmtX(rangeStmt: Ast.RangeStmt): Ast.Expr

    fun GetRangeStmtBody(rangeStmt: Ast.RangeStmt): Ast.BlockStmt

    fun GetNumReturnStmtResults(returnStmt: Ast.ReturnStmt): Int

    fun GetReturnStmtResult(returnStmt: Ast.ReturnStmt, i: Int): Ast.Expr

    fun GetSendStmtChan(sendStmt: Ast.SendStmt): Ast.Expr

    fun GetSendStmtValue(sendStmt: Ast.SendStmt): Ast.Expr

    fun GetSwitchStmtInit(switchStmt: Ast.SwitchStmt): Ast.Stmt?

    fun GetSwitchStmtTag(switchStmt: Ast.SwitchStmt): Ast.Expr?

    fun GetSwitchStmtBody(stmt: Ast.SwitchStmt): Ast.BlockStmt

    fun GetTypeSwitchStmtInit(typeSwitchStmt: Ast.TypeSwitchStmt): Ast.Stmt?

    fun GetTypeSwitchStmtAssign(typeSwitchStmt: Ast.TypeSwitchStmt): Ast.Stmt

    fun GetTypeSwitchStmtBody(typeSwitchStmt: Ast.TypeSwitchStmt): Ast.BlockStmt

    fun GetNumGenDeclSpecs(genDecl: Ast.GenDecl): Int

    fun GetGenDeclSpec(genDecl: Ast.GenDecl, i: Int): Ast.Spec

    fun GetGenDeclTok(genDecl: Ast.GenDecl): Int

    fun GetImportSpecName(importSpec: Ast.ImportSpec): Ast.Ident?

    fun GetImportSpecPath(importSpec: Ast.ImportSpec): Ast.BasicLit

    fun GetNumValueSpecNames(valueSpec: Ast.ValueSpec): Int

    fun GetValueSpecName(valueSpec: Ast.ValueSpec, i: Int): Ast.Ident

    fun GetValueSpecType(valueSpec: Ast.ValueSpec): Ast.Expr?

    fun GetNumValueSpecValues(valueSpec: Ast.ValueSpec): Int

    fun GetValueSpecValue(valueSpec: Ast.ValueSpec, i: Int): Ast.Expr

    fun GetTypeSpecName(typeSpec: Ast.TypeSpec): Ast.Ident

    fun GetTypeSpecAssign(typeSpec: Ast.TypeSpec): Int

    fun GetTypeSpecType(typeSpec: Ast.TypeSpec): Ast.Expr

    companion object {
        val INSTANCE: GoStandardLibrary by lazy {
            try {
                val arch =
                    System.getProperty("os.arch")
                        .replace("aarch64", "arm64")
                        .replace("x86_64", "amd64")
                val ext: String =
                    if (System.getProperty("os.name").startsWith("Mac")) {
                        ".dylib"
                    } else {
                        ".so"
                    }

                val stream =
                    GoLanguageFrontend::class.java.getResourceAsStream("/libgoast-$arch$ext")

                val tmp = java.io.File.createTempFile("libgoast", ext)
                tmp.deleteOnExit()
                val fos = FileOutputStream(tmp)
                stream?.copyTo(FileOutputStream(tmp))

                fos.close()
                stream?.close()

                LanguageFrontend.log.info("Loading libgoast library from ${tmp.absoluteFile}")

                Native.load(tmp.absolutePath, GoStandardLibrary::class.java)
            } catch (ex: Exception) {
                throw TranslationException(
                    "Error while loading cpgo library. Go frontend will not work correctly: $ex"
                )
            }
        }
    }
}

// TODO: optimize to use iterators instead
fun <T : PointerType, S : PointerType> T.list(
    numFunc: (T) -> Int,
    itemFunc: (T, Int) -> S,
): MutableList<S> {
    val list = mutableListOf<S>()
    for (i in 0 until numFunc(this)) {
        list += itemFunc(this, i)
    }

    return list
}
