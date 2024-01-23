//
// Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
//                    $$$$$$\  $$$$$$$\   $$$$$$\
//                   $$  __$$\ $$  __$$\ $$  __$$\
//                   $$ /  \__|$$ |  $$ |$$ /  \__|
//                   $$ |      $$$$$$$  |$$ |$$$$\
//                   $$ |      $$  ____/ $$ |\_$$ |
//                   $$ |  $$\ $$ |      $$ |  $$ |
//                   \$$$$$   |$$ |      \$$$$$   |
//                    \______/ \__|       \______/
//
//

package main

import (
	"C"
	"bytes"
	"fmt"
	"go/ast"
	"go/parser"
	"go/printer"
	"go/token"
	"reflect"
	"strings"
	"unsafe"

	"golang.org/x/mod/modfile"
	"golang.org/x/mod/module"

	pointer "github.com/mattn/go-pointer"
)

func main() {

}

//export GetType
func GetType(ptr unsafe.Pointer) *C.char {
	v := pointer.Restore(ptr)
	return C.CString(fmt.Sprintf("%T", v))
}

//export NewFileSet
func NewFileSet() unsafe.Pointer {
	return save(token.NewFileSet())
}

//export goParserParseFile
func goParserParseFile(fset unsafe.Pointer, path *C.char) unsafe.Pointer {
	f, err := parser.ParseFile(
		pointer.Restore(fset).(*token.FileSet),
		C.GoString(path), nil, parser.ParseComments|parser.SkipObjectResolution)
	if err != nil {
		panic(err)
	}

	return save(f)
}

//export modfileParse
func modfileParse(path *C.char, bytes *C.char) unsafe.Pointer {
	f, err := modfile.Parse(C.GoString(path), []byte(C.GoString(bytes)), nil)
	if err != nil {
		panic(err)
	}

	return save(f)
}

//export modfileGetFileModule
func modfileGetFileModule(ptr unsafe.Pointer) unsafe.Pointer {
	f := restore[*modfile.File](ptr)
	return save(f.Module)
}

//export modfileGetModuleMod
func modfileGetModuleMod(ptr unsafe.Pointer) unsafe.Pointer {
	m := restore[*modfile.Module](ptr)
	return save(m.Mod)
}

//export moduleGetVersionPath
func moduleGetVersionPath(ptr unsafe.Pointer) *C.char {
	v := restore[module.Version](ptr)
	return C.CString(v.Path)
}

//export NewCommentMap
func NewCommentMap(ptr1 unsafe.Pointer, ptr2 unsafe.Pointer, ptr3 unsafe.Pointer) unsafe.Pointer {
	fset := restore[*token.FileSet](ptr1)
	node := restore[ast.Node](ptr2)
	comments := restore[*[]*ast.CommentGroup](ptr3)
	m := ast.NewCommentMap(fset, node, *comments)
	return save(m)
}

//export GetFileSetPosition
func GetFileSetPosition(ptr unsafe.Pointer, pos int) unsafe.Pointer {
	fset := restore[*token.FileSet](ptr)
	position := fset.Position(token.Pos(pos))
	return save(&position)
}

//export GetFileSetFileName
func GetFileSetFileName(ptr unsafe.Pointer, pos int) *C.char {
	fset := restore[*token.FileSet](ptr)
	file := fset.File(token.Pos(pos))
	if file != nil {
		return C.CString(file.Name())
	} else {
		return nil
	}
}

//export GetFileSetNodeCode
func GetFileSetNodeCode(ptr1 unsafe.Pointer, ptr2 unsafe.Pointer) *C.char {
	fset := restore[*token.FileSet](ptr1)
	node := restore[ast.Node](ptr2)
	var codeBuf bytes.Buffer
	_ = printer.Fprint(&codeBuf, fset, node)

	return C.CString(codeBuf.String())
}

//export GetPositionLine
func GetPositionLine(ptr unsafe.Pointer) int {
	pos := restore[*token.Position](ptr)
	return pos.Line
}

//export GetPositionColumn
func GetPositionColumn(ptr unsafe.Pointer) int {
	pos := restore[*token.Position](ptr)
	return pos.Column
}

//export GetNodePos
func GetNodePos(ptr unsafe.Pointer) int {
	node := pointer.Restore(ptr).(ast.Node)
	return int(node.Pos())
}

//export GetNodeEnd
func GetNodeEnd(ptr unsafe.Pointer) C.int {
	node := pointer.Restore(ptr).(ast.Node)
	return C.int(node.End())
}

//export GetCommentMapNodeComment
func GetCommentMapNodeComment(ptr1 unsafe.Pointer, ptr2 unsafe.Pointer) *C.char {
	cm := restore[ast.CommentMap](ptr1)
	node := restore[ast.Node](ptr2)
	var comment = ""

	// Lookup ast node in comment map. One cannot use Filter() because this would actually filter all the comments
	// that are "below" this AST node as well, e.g. in its children. We only want the comments on the node itself.
	// Therefore we must convert the CommentMap back into an actual map to access the stored entry for the node.
	comments, ok := (map[ast.Node][]*ast.CommentGroup)(cm)[node]
	if !ok {
		return nil
	}

	for _, c := range comments {
		comment += c.Text()
	}
	// Remove last \n
	comment = strings.TrimRight(comment, "\n")

	return C.CString(comment)
}

//export GetFileName
func GetFileName(ptr unsafe.Pointer) unsafe.Pointer {
	file := pointer.Restore(ptr).(*ast.File)
	return save(file.Name)
}

//export GetNumFileDecls
func GetNumFileDecls(ptr unsafe.Pointer) C.int {
	return num[*ast.File](ptr, func(t *ast.File) []ast.Decl {
		return t.Decls
	})
}

//export GetFileDecl
func GetFileDecl(ptr unsafe.Pointer, i int) unsafe.Pointer {
	return item[*ast.File](ptr, i, func(t *ast.File) []ast.Decl {
		return t.Decls
	})
}

//export GetNumFileImports
func GetNumFileImports(ptr unsafe.Pointer) C.int {
	return num[*ast.File](ptr, func(t *ast.File) []*ast.ImportSpec {
		return t.Imports
	})
}

//export GetFileImport
func GetFileImport(ptr unsafe.Pointer, i int) unsafe.Pointer {
	return item[*ast.File](ptr, i, func(t *ast.File) []*ast.ImportSpec {
		return t.Imports
	})
}

//export GetFileComments
func GetFileComments(ptr unsafe.Pointer) unsafe.Pointer {
	file := restore[*ast.File](ptr)
	return save(&file.Comments)
}

//export GetNumFieldListList
func GetNumFieldListList(ptr unsafe.Pointer) C.int {
	return num[*ast.FieldList](ptr, func(t *ast.FieldList) []*ast.Field {
		return t.List
	})
}

//export GetFieldListList
func GetFieldListList(ptr unsafe.Pointer, i int) unsafe.Pointer {
	return item[*ast.FieldList](ptr, i, func(t *ast.FieldList) []*ast.Field {
		return t.List
	})
}

//export GetBinaryExprX
func GetBinaryExprX(ptr unsafe.Pointer) unsafe.Pointer {
	bin := restore[*ast.BinaryExpr](ptr)
	return save(bin.X)
}

//export GetBinaryExprOpString
func GetBinaryExprOpString(ptr unsafe.Pointer) *C.char {
	bin := restore[*ast.BinaryExpr](ptr)
	return C.CString(bin.Op.String())
}

//export GetBinaryExprY
func GetBinaryExprY(ptr unsafe.Pointer) unsafe.Pointer {
	bin := restore[*ast.BinaryExpr](ptr)
	return save(bin.Y)
}

//export GetDeclStmtDecl
func GetDeclStmtDecl(ptr unsafe.Pointer) unsafe.Pointer {
	stmt := restore[*ast.DeclStmt](ptr)
	return save(stmt.Decl)
}

//export GetExprStmtX
func GetExprStmtX(ptr unsafe.Pointer) unsafe.Pointer {
	stmt := restore[*ast.ExprStmt](ptr)
	return save(stmt.X)
}

//export GetCallExprFun
func GetCallExprFun(ptr unsafe.Pointer) unsafe.Pointer {
	c := restore[*ast.CallExpr](ptr)
	return save(c.Fun)
}

//export GetNumCallExprArgs
func GetNumCallExprArgs(ptr unsafe.Pointer) C.int {
	return num[*ast.CallExpr](ptr, func(t *ast.CallExpr) []ast.Expr {
		return t.Args
	})
}

//export GetCallExprArg
func GetCallExprArg(ptr unsafe.Pointer, i int) unsafe.Pointer {
	return item[*ast.CallExpr](ptr, i, func(t *ast.CallExpr) []ast.Expr {
		return t.Args
	})
}

//export GetEllipsisElt
func GetEllipsisElt(ptr unsafe.Pointer) unsafe.Pointer {
	expr := restore[*ast.Ellipsis](ptr)
	return save(expr.Elt)
}

//export GetForStmtInit
func GetForStmtInit(ptr unsafe.Pointer) unsafe.Pointer {
	stmt := restore[*ast.ForStmt](ptr)
	return save(stmt.Init)
}

//export GetForStmtCond
func GetForStmtCond(ptr unsafe.Pointer) unsafe.Pointer {
	stmt := restore[*ast.ForStmt](ptr)
	return save(stmt.Cond)
}

//export GetForStmtPost
func GetForStmtPost(ptr unsafe.Pointer) unsafe.Pointer {
	stmt := restore[*ast.ForStmt](ptr)
	return save(stmt.Post)
}

//export GetForStmtBody
func GetForStmtBody(ptr unsafe.Pointer) unsafe.Pointer {
	stmt := restore[*ast.ForStmt](ptr)
	return save(stmt.Body)
}

//export GetGoStmtCall
func GetGoStmtCall(ptr unsafe.Pointer) unsafe.Pointer {
	stmt := restore[*ast.GoStmt](ptr)
	return save(stmt.Call)
}

//export GetIncDecStmtTokString
func GetIncDecStmtTokString(ptr unsafe.Pointer) *C.char {
	stmt := restore[*ast.IncDecStmt](ptr)
	return C.CString(stmt.Tok.String())
}

//export GetIncDecStmtX
func GetIncDecStmtX(ptr unsafe.Pointer) unsafe.Pointer {
	stmt := restore[*ast.IncDecStmt](ptr)
	return save(stmt.X)
}

//export GetIfStmtInit
func GetIfStmtInit(ptr unsafe.Pointer) unsafe.Pointer {
	stmt := restore[*ast.IfStmt](ptr)
	return save(stmt.Init)
}

//export GetIfStmtCond
func GetIfStmtCond(ptr unsafe.Pointer) unsafe.Pointer {
	stmt := restore[*ast.IfStmt](ptr)
	return save(stmt.Cond)
}

//export GetIfStmtBody
func GetIfStmtBody(ptr unsafe.Pointer) unsafe.Pointer {
	stmt := restore[*ast.IfStmt](ptr)
	return save(stmt.Body)
}

//export GetIfStmtElse
func GetIfStmtElse(ptr unsafe.Pointer) unsafe.Pointer {
	stmt := restore[*ast.IfStmt](ptr)
	return save(stmt.Else)
}

//export GetLabeledStmtLabel
func GetLabeledStmtLabel(ptr unsafe.Pointer) unsafe.Pointer {
	stmt := restore[*ast.LabeledStmt](ptr)
	return save(stmt.Label)
}

//export GetLabeledStmtStmt
func GetLabeledStmtStmt(ptr unsafe.Pointer) unsafe.Pointer {
	stmt := restore[*ast.LabeledStmt](ptr)
	return save(stmt.Stmt)
}

//export GetRangeStmtTokString
func GetRangeStmtTokString(ptr unsafe.Pointer) *C.char {
	stmt := restore[*ast.RangeStmt](ptr)
	return C.CString(stmt.Tok.String())
}

//export GetRangeStmtKey
func GetRangeStmtKey(ptr unsafe.Pointer) unsafe.Pointer {
	stmt := restore[*ast.RangeStmt](ptr)
	return save(stmt.Key)
}

//export GetRangeStmtValue
func GetRangeStmtValue(ptr unsafe.Pointer) unsafe.Pointer {
	stmt := restore[*ast.RangeStmt](ptr)
	return save(stmt.Value)
}

//export GetRangeStmtX
func GetRangeStmtX(ptr unsafe.Pointer) unsafe.Pointer {
	stmt := restore[*ast.RangeStmt](ptr)
	return save(stmt.X)
}

//export GetRangeStmtBody
func GetRangeStmtBody(ptr unsafe.Pointer) unsafe.Pointer {
	stmt := restore[*ast.RangeStmt](ptr)
	return save(stmt.Body)
}

//export GetBasicLitKind
func GetBasicLitKind(ptr unsafe.Pointer) C.int {
	lit := restore[*ast.BasicLit](ptr)
	return C.int(lit.Kind)
}

//export GetBasicLitValue
func GetBasicLitValue(ptr unsafe.Pointer) *C.char {
	lit := restore[*ast.BasicLit](ptr)
	return C.CString(lit.Value)
}

//export GetCompositeLitType
func GetCompositeLitType(ptr unsafe.Pointer) unsafe.Pointer {
	lit := restore[*ast.CompositeLit](ptr)
	return save(lit.Type)
}

//export GetNumCompositeLitElts
func GetNumCompositeLitElts(ptr unsafe.Pointer) C.int {
	return num[*ast.CompositeLit](ptr, func(t *ast.CompositeLit) []ast.Expr {
		return t.Elts
	})
}

//export GetCompositeLitElt
func GetCompositeLitElt(ptr unsafe.Pointer, i int) unsafe.Pointer {
	return item[*ast.CompositeLit](ptr, i, func(t *ast.CompositeLit) []ast.Expr {
		return t.Elts
	})
}

//export MakeFuncDeclFromFuncLit
func MakeFuncDeclFromFuncLit(ptr unsafe.Pointer) unsafe.Pointer {
	lit := restore[*ast.FuncLit](ptr)
	if lit == nil {
		return nil
	}
	decl := &ast.FuncDecl{
		Doc:  nil,
		Recv: nil,
		Name: ast.NewIdent(""),
		Type: lit.Type,
		Body: lit.Body,
	}
	return save(decl)
}

//export GetIdentNamePos
func GetIdentNamePos(ptr unsafe.Pointer) C.int {
	ident := pointer.Restore(ptr).(*ast.Ident)
	return C.int(ident.NamePos)
}

//export GetIdentName
func GetIdentName(ptr unsafe.Pointer) *C.char {
	ident := pointer.Restore(ptr).(*ast.Ident)
	return C.CString(ident.Name)
}

//export GetIndexExprX
func GetIndexExprX(ptr unsafe.Pointer) unsafe.Pointer {
	expr := restore[*ast.IndexExpr](ptr)
	return save(expr.X)
}

//export GetIndexExprIndex
func GetIndexExprIndex(ptr unsafe.Pointer) unsafe.Pointer {
	expr := restore[*ast.IndexExpr](ptr)
	return save(expr.Index)
}

//export GetIndexListExprX
func GetIndexListExprX(ptr unsafe.Pointer) unsafe.Pointer {
	expr := restore[*ast.IndexListExpr](ptr)
	return save(expr.X)
}

//export GetNumIndexListExprIndices
func GetNumIndexListExprIndices(ptr unsafe.Pointer) C.int {
	return num[*ast.IndexListExpr](ptr, func(t *ast.IndexListExpr) []ast.Expr {
		return t.Indices
	})
}

//export GetIndexListExprIndex
func GetIndexListExprIndex(ptr unsafe.Pointer, i int) unsafe.Pointer {
	return item[*ast.IndexListExpr](ptr, i, func(t *ast.IndexListExpr) []ast.Expr {
		return t.Indices
	})
}

//export GetKeyValueExprKey
func GetKeyValueExprKey(ptr unsafe.Pointer) unsafe.Pointer {
	kv := restore[*ast.KeyValueExpr](ptr)
	return save(kv.Key)
}

//export GetKeyValueExprValue
func GetKeyValueExprValue(ptr unsafe.Pointer) unsafe.Pointer {
	kv := restore[*ast.KeyValueExpr](ptr)
	return save(kv.Value)
}

//export GetParenExprX
func GetParenExprX(ptr unsafe.Pointer) unsafe.Pointer {
	p := restore[*ast.ParenExpr](ptr)
	return save(p.X)
}

//export GetSelectorExprX
func GetSelectorExprX(ptr unsafe.Pointer) unsafe.Pointer {
	sel := restore[*ast.SelectorExpr](ptr)
	return save(sel.X)
}

//export GetSelectorExprSel
func GetSelectorExprSel(ptr unsafe.Pointer) unsafe.Pointer {
	sel := restore[*ast.SelectorExpr](ptr)
	return save(sel.Sel)
}

//export GetSliceExprX
func GetSliceExprX(ptr unsafe.Pointer) unsafe.Pointer {
	expr := restore[*ast.SliceExpr](ptr)
	return save(expr.X)
}

//export GetSliceExprLow
func GetSliceExprLow(ptr unsafe.Pointer) unsafe.Pointer {
	expr := restore[*ast.SliceExpr](ptr)
	return save(expr.Low)
}

//export GetSliceExprHigh
func GetSliceExprHigh(ptr unsafe.Pointer) unsafe.Pointer {
	expr := restore[*ast.SliceExpr](ptr)
	return save(expr.High)
}

//export GetSliceExprMax
func GetSliceExprMax(ptr unsafe.Pointer) unsafe.Pointer {
	expr := restore[*ast.SliceExpr](ptr)
	return save(expr.Max)
}

//export GetStarExprX
func GetStarExprX(ptr unsafe.Pointer) unsafe.Pointer {
	star := restore[*ast.StarExpr](ptr)
	return save(star.X)
}

//export GetTypeAssertExprX
func GetTypeAssertExprX(ptr unsafe.Pointer) unsafe.Pointer {
	expr := restore[*ast.TypeAssertExpr](ptr)
	return save(expr.X)
}

//export GetTypeAssertExprType
func GetTypeAssertExprType(ptr unsafe.Pointer) unsafe.Pointer {
	expr := restore[*ast.TypeAssertExpr](ptr)
	return save(expr.Type)
}

//export GetUnaryExprOpString
func GetUnaryExprOpString(ptr unsafe.Pointer) *C.char {
	unary := restore[*ast.UnaryExpr](ptr)
	return C.CString(unary.Op.String())
}

//export GetUnaryExprX
func GetUnaryExprX(ptr unsafe.Pointer) unsafe.Pointer {
	unary := restore[*ast.UnaryExpr](ptr)
	return save(unary.X)
}

//export GetArrayTypeElt
func GetArrayTypeElt(ptr unsafe.Pointer) unsafe.Pointer {
	typ := restore[*ast.ArrayType](ptr)
	return save(typ.Elt)
}

//export GetChanTypeValue
func GetChanTypeValue(ptr unsafe.Pointer) unsafe.Pointer {
	typ := restore[*ast.ChanType](ptr)
	return save(typ.Value)
}

//export GetFuncTypeParams
func GetFuncTypeParams(ptr unsafe.Pointer) unsafe.Pointer {
	typ := restore[*ast.FuncType](ptr)
	return save(typ.Params)
}

//export GetFuncTypeTypeParams
func GetFuncTypeTypeParams(ptr unsafe.Pointer) unsafe.Pointer {
	typ := restore[*ast.FuncType](ptr)
	return save(typ.TypeParams)
}

//export GetFuncParams
func GetFuncParams(ptr unsafe.Pointer) unsafe.Pointer {
	typ := restore[*ast.FuncType](ptr)
	return save(typ.Params)
}

//export GetFuncTypeResults
func GetFuncTypeResults(ptr unsafe.Pointer) unsafe.Pointer {
	typ := restore[*ast.FuncType](ptr)
	res := save(typ.Results)
	return res
}

//export GetMapTypeKey
func GetMapTypeKey(ptr unsafe.Pointer) unsafe.Pointer {
	typ := restore[*ast.MapType](ptr)
	return save(typ.Key)
}

//export GetMapTypeValue
func GetMapTypeValue(ptr unsafe.Pointer) unsafe.Pointer {
	typ := restore[*ast.MapType](ptr)
	return save(typ.Value)
}

//export GetStructTypeFields
func GetStructTypeFields(ptr unsafe.Pointer) unsafe.Pointer {
	typ := restore[*ast.StructType](ptr)
	return save(typ.Fields)
}

//export GetStructTypeIncomplete
func GetStructTypeIncomplete(ptr unsafe.Pointer) C.char {
	typ := restore[*ast.StructType](ptr)
	if typ.Incomplete {
		return C.char(1)
	} else {
		return C.char(0)
	}
}

//export GetNumFieldNames
func GetNumFieldNames(ptr unsafe.Pointer) C.int {
	return num[*ast.Field](ptr, func(t *ast.Field) []*ast.Ident {
		return t.Names
	})
}

//export GetFieldName
func GetFieldName(ptr unsafe.Pointer, i int) unsafe.Pointer {
	return item[*ast.Field](ptr, i, func(t *ast.Field) []*ast.Ident {
		return t.Names
	})
}

//export GetFieldType
func GetFieldType(ptr unsafe.Pointer) (typ unsafe.Pointer) {
	field := pointer.Restore(ptr).(*ast.Field)
	return save(field.Type)
}

//export GetNumGenDeclSpecs
func GetNumGenDeclSpecs(ptr unsafe.Pointer) C.int {
	return num[*ast.GenDecl](ptr, func(t *ast.GenDecl) []ast.Spec {
		return t.Specs
	})
}

//export GetGenDeclSpec
func GetGenDeclSpec(ptr unsafe.Pointer, i int) unsafe.Pointer {
	return item[*ast.GenDecl](ptr, i, func(t *ast.GenDecl) []ast.Spec {
		return t.Specs
	})
}

//export GetGenDeclTok
func GetGenDeclTok(ptr unsafe.Pointer) int {
	decl := restore[*ast.GenDecl](ptr)
	return int(decl.Tok)
}

//export GetInterfaceTypeMethods
func GetInterfaceTypeMethods(ptr unsafe.Pointer) unsafe.Pointer {
	i := restore[*ast.InterfaceType](ptr)
	return save(i.Methods)
}

//export GetInterfaceTypeIncomplete
func GetInterfaceTypeIncomplete(ptr unsafe.Pointer) C.char {
	i := restore[*ast.InterfaceType](ptr)
	if i.Incomplete {
		return C.char(1)
	} else {
		return C.char(0)
	}
}

//export GetFuncDeclRecv
func GetFuncDeclRecv(ptr unsafe.Pointer) unsafe.Pointer {
	f := restore[*ast.FuncDecl](ptr)
	return save(f.Recv)
}

// GetFuncDeclName returns the name property of a [*ast.FuncDecl] as [*ast.Ident].
//
//export GetFuncDeclName
func GetFuncDeclName(ptr unsafe.Pointer) unsafe.Pointer {
	f := restore[*ast.FuncDecl](ptr)
	return save(f.Name)
}

//export GetFuncDeclType
func GetFuncDeclType(ptr unsafe.Pointer) unsafe.Pointer {
	f := restore[*ast.FuncDecl](ptr)
	return save(f.Type)
}

// GetFuncDeclBody returns the body property of a [*ast.FuncDecl] as [*ast.BlockStmt].
//
//export GetFuncDeclBody
func GetFuncDeclBody(ptr unsafe.Pointer) unsafe.Pointer {
	f := restore[*ast.FuncDecl](ptr)
	return save(f.Body)
}

//export GetImportSpecName
func GetImportSpecName(ptr unsafe.Pointer) unsafe.Pointer {
	spec := restore[*ast.ImportSpec](ptr)
	return save(spec.Name)
}

//export GetImportSpecPath
func GetImportSpecPath(ptr unsafe.Pointer) unsafe.Pointer {
	spec := restore[*ast.ImportSpec](ptr)
	return save(spec.Path)
}

//export GetNumValueSpecNames
func GetNumValueSpecNames(ptr unsafe.Pointer) C.int {
	return num[*ast.ValueSpec](ptr, func(t *ast.ValueSpec) []*ast.Ident {
		return t.Names
	})
}

//export GetValueSpecName
func GetValueSpecName(ptr unsafe.Pointer, i int) unsafe.Pointer {
	return item[*ast.ValueSpec](ptr, i, func(t *ast.ValueSpec) []*ast.Ident {
		return t.Names
	})
}

//export GetValueSpecType
func GetValueSpecType(ptr unsafe.Pointer) unsafe.Pointer {
	spec := restore[*ast.ValueSpec](ptr)
	return save(spec.Type)
}

//export GetNumValueSpecValues
func GetNumValueSpecValues(ptr unsafe.Pointer) C.int {
	return num[*ast.ValueSpec](ptr, func(t *ast.ValueSpec) []ast.Expr {
		return t.Values
	})
}

//export GetValueSpecValue
func GetValueSpecValue(ptr unsafe.Pointer, i int) unsafe.Pointer {
	return item[*ast.ValueSpec](ptr, i, func(t *ast.ValueSpec) []ast.Expr {
		return t.Values
	})
}

//export GetTypeSpecName
func GetTypeSpecName(ptr unsafe.Pointer) unsafe.Pointer {
	spec := restore[*ast.TypeSpec](ptr)
	return save(spec.Name)
}

//export GetTypeSpecAssign
func GetTypeSpecAssign(ptr unsafe.Pointer) int {
	spec := restore[*ast.TypeSpec](ptr)
	return int(spec.Assign)
}

//export GetTypeSpecType
func GetTypeSpecType(ptr unsafe.Pointer) unsafe.Pointer {
	spec := restore[*ast.TypeSpec](ptr)
	return save(spec.Type)
}

//export GetNumBlockStmtList
func GetNumBlockStmtList(ptr unsafe.Pointer) C.int {
	stmt := restore[*ast.BlockStmt](ptr)
	return C.int(len(stmt.List))
}

//export GetBlockStmtList
func GetBlockStmtList(ptr unsafe.Pointer, i int) unsafe.Pointer {
	stmt := restore[*ast.BlockStmt](ptr)
	return save(stmt.List[i])
}

//export GetBranchStmtTokString
func GetBranchStmtTokString(ptr unsafe.Pointer) *C.char {
	stmt := restore[*ast.BranchStmt](ptr)
	return C.CString(stmt.Tok.String())
}

//export GetBranchStmtLabel
func GetBranchStmtLabel(ptr unsafe.Pointer) unsafe.Pointer {
	stmt := restore[*ast.BranchStmt](ptr)
	return save(stmt.Label)
}

//export GetNumCaseClauseList
func GetNumCaseClauseList(ptr unsafe.Pointer) C.int {
	stmt := restore[*ast.CaseClause](ptr)
	return C.int(len(stmt.List))
}

//export GetCaseClauseList
func GetCaseClauseList(ptr unsafe.Pointer, i int) unsafe.Pointer {
	stmt := restore[*ast.CaseClause](ptr)
	return save(stmt.List[i])
}

//export GetNumCaseClauseBody
func GetNumCaseClauseBody(ptr unsafe.Pointer) C.int {
	stmt := restore[*ast.CaseClause](ptr)
	return C.int(len(stmt.Body))
}

//export GetCaseClauseBody
func GetCaseClauseBody(ptr unsafe.Pointer, i int) unsafe.Pointer {
	stmt := restore[*ast.CaseClause](ptr)
	return save(stmt.Body[i])
}

//export GetNumAssignStmtLhs
func GetNumAssignStmtLhs(ptr unsafe.Pointer) C.int {
	return num[*ast.AssignStmt](ptr, func(t *ast.AssignStmt) []ast.Expr {
		return t.Lhs
	})
}

//export GetAssignStmtLhs
func GetAssignStmtLhs(ptr unsafe.Pointer, i int) unsafe.Pointer {
	return item[*ast.AssignStmt](ptr, i, func(t *ast.AssignStmt) []ast.Expr {
		return t.Lhs
	})
}

//export GetAssignStmtTok
func GetAssignStmtTok(ptr unsafe.Pointer) C.int {
	stmt := restore[*ast.AssignStmt](ptr)
	return C.int(stmt.Tok)
}

//export GetNumAssignStmtRhs
func GetNumAssignStmtRhs(ptr unsafe.Pointer) C.int {
	return num[*ast.AssignStmt](ptr, func(t *ast.AssignStmt) []ast.Expr {
		return t.Rhs
	})
}

//export GetAssignStmtRhs
func GetAssignStmtRhs(ptr unsafe.Pointer, i int) unsafe.Pointer {
	return item[*ast.AssignStmt](ptr, i, func(t *ast.AssignStmt) []ast.Expr {
		return t.Rhs
	})
}

//export GetDeferStmtCall
func GetDeferStmtCall(ptr unsafe.Pointer) unsafe.Pointer {
	stmt := restore[*ast.DeferStmt](ptr)
	return save(stmt.Call)
}

//export GetNumReturnStmtResults
func GetNumReturnStmtResults(ptr unsafe.Pointer) C.int {
	return num[*ast.ReturnStmt](ptr, func(t *ast.ReturnStmt) []ast.Expr {
		return t.Results
	})
}

//export GetReturnStmtResult
func GetReturnStmtResult(ptr unsafe.Pointer, i int) unsafe.Pointer {
	return item[*ast.ReturnStmt](ptr, i, func(t *ast.ReturnStmt) []ast.Expr {
		return t.Results
	})
}

//export GetSendStmtChan
func GetSendStmtChan(ptr unsafe.Pointer) unsafe.Pointer {
	stmt := restore[*ast.SendStmt](ptr)
	return save(stmt.Chan)
}

//export GetSendStmtValue
func GetSendStmtValue(ptr unsafe.Pointer) unsafe.Pointer {
	stmt := restore[*ast.SendStmt](ptr)
	return save(stmt.Value)
}

//export GetSwitchStmtInit
func GetSwitchStmtInit(ptr unsafe.Pointer) unsafe.Pointer {
	stmt := restore[*ast.SwitchStmt](ptr)
	return save(stmt.Init)
}

//export GetSwitchStmtTag
func GetSwitchStmtTag(ptr unsafe.Pointer) unsafe.Pointer {
	stmt := restore[*ast.SwitchStmt](ptr)
	return save(stmt.Tag)
}

//export GetSwitchStmtBody
func GetSwitchStmtBody(ptr unsafe.Pointer) unsafe.Pointer {
	stmt := restore[*ast.SwitchStmt](ptr)
	return save(stmt.Body)
}

//export GetTypeSwitchStmtInit
func GetTypeSwitchStmtInit(ptr unsafe.Pointer) unsafe.Pointer {
	stmt := restore[*ast.TypeSwitchStmt](ptr)
	return save(stmt.Init)
}

//export GetTypeSwitchStmtAssign
func GetTypeSwitchStmtAssign(ptr unsafe.Pointer) unsafe.Pointer {
	stmt := restore[*ast.TypeSwitchStmt](ptr)
	return save(stmt.Assign)
}

//export GetTypeSwitchStmtBody
func GetTypeSwitchStmtBody(ptr unsafe.Pointer) unsafe.Pointer {
	stmt := restore[*ast.TypeSwitchStmt](ptr)
	return save(stmt.Body)
}

func restore[T any](ptr unsafe.Pointer) T {
	return pointer.Restore(ptr).(T)
}

func save(val any) unsafe.Pointer {
	rv := reflect.ValueOf(val)
	if val == nil || (rv.Kind() == reflect.Pointer && rv.IsNil()) {
		return nil
	} else {
		return pointer.Save(val)
	}
}

func num[T any, S any](ptr unsafe.Pointer, fieldFunc func(t T) []S) C.int {
	t := restore[T](ptr)

	return C.int(len(fieldFunc(t)))
}

func item[T any, S any](ptr unsafe.Pointer, i int, fieldFunc func(t T) []S) unsafe.Pointer {
	t := restore[T](ptr)

	return save(fieldFunc(t)[i])
}
