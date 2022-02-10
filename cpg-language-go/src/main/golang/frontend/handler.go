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
package frontend

import (
	"cpg"
	"fmt"
	"go/ast"
	"go/token"
	"io/ioutil"
	"log"
	"os"
	"path"
	"strconv"
	"strings"

	"golang.org/x/mod/modfile"
	"tekao.net/jnigi"
)

func getImportName(spec *ast.ImportSpec) string {
	if spec.Name != nil {
		return spec.Name.Name
	}

	var path = spec.Path.Value[1 : len(spec.Path.Value)-1]
	var paths = strings.Split(path, "/")

	return paths[len(paths)-1]
}

func (frontend *GoLanguageFrontend) ParseModule(topLevel string) (exists bool, err error) {
	frontend.LogInfo("Looking for a go.mod file in %s", topLevel)

	mod := path.Join(topLevel, "go.mod")

	if _, err := os.Stat(mod); err != nil {
		if os.IsNotExist(err) {
			frontend.LogInfo("%s does not exist", mod)

			return false, nil
		}
	}

	b, err := ioutil.ReadFile(mod)
	if err != nil {
		return true, fmt.Errorf("could not read go.mod: %w", err)
	}

	module, err := modfile.Parse(mod, b, nil)
	if err != nil {
		return true, fmt.Errorf("could not parse mod file: %w", err)
	}

	frontend.Module = module

	frontend.LogInfo("Go application has module support with path %s", module.Module.Mod.Path)

	return true, nil
}

func (this *GoLanguageFrontend) HandleFile(fset *token.FileSet, file *ast.File, path string) (tu *cpg.TranslationUnitDeclaration, err error) {
	tu = cpg.NewTranslationUnitDeclaration(fset, file, path, this.GetCodeFromRawNode(fset, file))

	scope := this.GetScopeManager()

	// reset scope
	scope.ResetToGlobal((*cpg.Node)(tu))

	// set current TU
	this.SetCurrentTU(tu)

	for _, imprt := range file.Imports {
		i := this.handleImportSpec(fset, imprt)

		err = scope.AddDeclaration((*cpg.Declaration)(i))
		if err != nil {
			log.Fatal(err)
		}
	}

	// create a new namespace declaration, representing the package
	p := cpg.NewNamespaceDeclaration(fset, nil, file.Name.Name, fmt.Sprintf("package %s", file.Name.Name))

	// enter scope
	scope.EnterScope((*cpg.Node)(p))

	for _, decl := range file.Decls {
		var d *cpg.Declaration

		d = this.handleDecl(fset, decl)

		if d != nil {
			err = scope.AddDeclaration((*cpg.Declaration)(d))
			if err != nil {
				log.Fatal(err)

			}
		}
	}

	// leave scope
	scope.LeaveScope((*cpg.Node)(p))

	// add it
	scope.AddDeclaration((*cpg.Declaration)(p))

	return
}

// handleComments maps comments from ast.Node to a cpg.Node by using ast.CommentMap.
func (this *GoLanguageFrontend) handleComments(node *cpg.Node, astNode ast.Node) {
	this.LogDebug("Handling comments for %+v", astNode)

	var comment = ""

	// Lookup ast node in comment map. One cannot use Filter() because this would actually filter all the comments
	// that are "below" this AST node as well, e.g. in its children. We only want the comments on the node itself.
	// Therefore we must convert the CommentMap back into an actual map to access the stored entry for the node.
	comments, ok := (map[ast.Node][]*ast.CommentGroup)(this.CommentMap)[astNode]
	if !ok {
		return
	}

	for _, c := range comments {
		text := strings.TrimRight(c.Text(), "\n")
		comment += text
	}

	if comment != "" {
		node.SetComment(comment)

		this.LogDebug("Comments: %+v", comment)
	}
}

func (this *GoLanguageFrontend) handleDecl(fset *token.FileSet, decl ast.Decl) (d *cpg.Declaration) {
	this.LogDebug("Handling declaration (%T): %+v", decl, decl)

	switch v := decl.(type) {
	case *ast.FuncDecl:
		d = (*cpg.Declaration)(this.handleFuncDecl(fset, v))
	case *ast.GenDecl:
		d = (*cpg.Declaration)(this.handleGenDecl(fset, v))
	default:
		this.LogError("Not parsing declaration of type %T yet: %+v", v, v)
		// no match
		d = nil
	}

	if d != nil {
		this.handleComments((*cpg.Node)(d), decl)
	}

	return
}

func (this *GoLanguageFrontend) handleFuncDecl(fset *token.FileSet, funcDecl *ast.FuncDecl) *jnigi.ObjectRef {
	this.LogDebug("Handling func Decl: %+v", *funcDecl)

	var scope = this.GetScopeManager()
	var receiver *cpg.VariableDeclaration

	var f *cpg.FunctionDeclaration
	if funcDecl.Recv != nil {
		m := cpg.NewMethodDeclaration(fset, funcDecl)

		// TODO: why is this a list?
		var recv = funcDecl.Recv.List[0]

		var recordType = this.handleType(recv.Type)

		receiver = cpg.NewVariableDeclaration(fset, nil)

		// TODO: should we use the FQN here? FQNs are a mess in the CPG...
		receiver.SetName(recv.Names[0].Name)
		receiver.SetType(recordType)

		err := m.SetReceiver(receiver)
		if err != nil {
			log.Fatal(err)
		}

		if recordType != nil {
			var recordName = (*cpg.Node)(recordType).GetName()

			// TODO: this will only find methods within the current translation unit
			// this is a limitation that we have for C++ as well
			record, err := this.GetScopeManager().GetRecordForName(
				this.GetScopeManager().GetCurrentScope(),
				recordName)

			if err != nil {
				log.Fatal(err)

			}

			if record != nil && !record.IsNil() {
				// now this gets a little bit hacky, we will add it to the record declaration
				// this is strictly speaking not 100 % true, since the method property edge is
				// marked as AST and in Go a method is not part of the struct's AST but is declared
				// outside. In the future, we need to differentiate between just the associated members
				// of the class and the pure AST nodes declared in the struct itself
				this.LogDebug("Record: %+v", record)

				err = record.AddMethod(m)
				if err != nil {
					log.Fatal(err)

				}
			}
		}

		f = (*cpg.FunctionDeclaration)(m)
	} else {
		f = cpg.NewFunctionDeclaration(fset, funcDecl)
	}

	// note, the name must be set BEFORE entering the scope
	f.SetName(funcDecl.Name.Name)

	// enter scope for function
	scope.EnterScope((*cpg.Node)(f))

	if receiver != nil {
		this.LogDebug("Adding receiver %s", (*cpg.Node)(receiver).GetName())

		// add the receiver do the scope manager, so we can resolve the receiver value
		this.GetScopeManager().AddDeclaration((*cpg.Declaration)(receiver))
	}

	var t *cpg.Type

	// TODO: for now, just the first result type. Maybe later combine it into a pair?
	if funcDecl.Type.Results == nil {
		// its proably void
		t = cpg.TypeParser_createFrom("void", false)
	} else {
		t = this.handleType(funcDecl.Type.Results.List[0].Type)

		// if the function has named return variables, be sure to declare them as well
		for _, returnVariable := range funcDecl.Type.Results.List {
			if returnVariable.Names != nil {
				p := cpg.NewVariableDeclaration(fset, returnVariable)

				p.SetName(returnVariable.Names[0].Name)
				p.SetType(this.handleType(returnVariable.Type))

				// add parameter to scope
				this.GetScopeManager().AddDeclaration((*cpg.Declaration)(p))
			}
		}
	}

	this.LogDebug("Function has return type %s", (*cpg.Node)(t).GetName())

	f.SetType(t)

	// TODO: for other languages, we would enter the record declaration, if
	// this is a method; however I am not quite sure if this makes sense for
	// go, since we do not have a 'this', but rather a named receiver

	this.LogDebug("Parsing function body")

	for _, param := range funcDecl.Type.Params.List {
		p := cpg.NewParamVariableDeclaration(fset, param)

		// TODO: more than one name?
		p.SetName(param.Names[0].Name)
		p.SetType(this.handleType(param.Type))

		// add parameter to scope
		this.GetScopeManager().AddDeclaration((*cpg.Declaration)(p))

		this.handleComments((*cpg.Node)(p), param)
	}

	// parse body
	s := this.handleBlockStmt(fset, funcDecl.Body)

	err := f.SetBody((*cpg.Statement)(s))
	if err != nil {
		log.Fatal(err)

	}

	// leave scope
	err = scope.LeaveScope((*cpg.Node)(f))
	if err != nil {
		log.Fatal(err)

	}

	return (*jnigi.ObjectRef)(f)
}

func (this *GoLanguageFrontend) handleGenDecl(fset *token.FileSet, genDecl *ast.GenDecl) *jnigi.ObjectRef {
	// TODO: Handle multiple declarations
	for _, spec := range genDecl.Specs {
		switch v := spec.(type) {
		case *ast.ValueSpec:
			return (*jnigi.ObjectRef)(this.handleValueSpec(fset, v))
		case *ast.TypeSpec:
			return (*jnigi.ObjectRef)(this.handleTypeSpec(fset, v))
		case *ast.ImportSpec:
			// somehow these end up duplicate in the AST, so do not handle them here
			return nil
			/*return (*jnigi.ObjectRef)(this.handleImportSpec(fset, v))*/
		default:
			this.LogError("Not parsing specication of type %T yet: %+v", v, v)
		}
	}

	return nil
}

func (this *GoLanguageFrontend) handleValueSpec(fset *token.FileSet, valueDecl *ast.ValueSpec) *cpg.Declaration {
	// TODO: more names
	var ident = valueDecl.Names[0]

	d := (cpg.NewVariableDeclaration(fset, valueDecl))

	d.SetName(ident.Name)

	if valueDecl.Type != nil {
		t := this.handleType(valueDecl.Type)

		d.SetType(t)
	}

	// add an initializer
	if len(valueDecl.Values) > 0 {
		// TODO: How to deal with multiple values
		var expr = this.handleExpr(fset, valueDecl.Values[0])

		err := d.SetInitializer(expr)
		if err != nil {
			log.Fatal(err)
		}
	}

	return (*cpg.Declaration)(d)
}

func (this *GoLanguageFrontend) handleTypeSpec(fset *token.FileSet, typeDecl *ast.TypeSpec) *cpg.Declaration {
	err := this.LogInfo("Type specifier with name %s and type (%T, %+v)", typeDecl.Name.Name, typeDecl.Type, typeDecl.Type)
	if err != nil {
		log.Fatal(err)
	}

	switch v := typeDecl.Type.(type) {
	case *ast.StructType:
		return (*cpg.Declaration)(this.handleStructTypeSpec(fset, typeDecl, v))
	case *ast.InterfaceType:
		return (*cpg.Declaration)(this.handleInterfaceTypeSpec(fset, typeDecl, v))
	}

	return nil
}

func (this *GoLanguageFrontend) handleImportSpec(fset *token.FileSet, importSpec *ast.ImportSpec) *cpg.Declaration {
	this.LogInfo("Import specifier with: %+v)", *importSpec)

	i := cpg.NewIncludeDeclaration(fset, importSpec)

	var scope = this.GetScopeManager()

	i.SetName(getImportName(importSpec))
	i.SetFilename(importSpec.Path.Value[1 : len(importSpec.Path.Value)-1])

	err := scope.AddDeclaration((*cpg.Declaration)(i))
	if err != nil {
		log.Fatal(err)
	}

	return (*cpg.Declaration)(i)
}

func (this *GoLanguageFrontend) handleIdentAsName(ident *ast.Ident) string {
	if this.isBuiltinType(ident.Name) {
		return ident.Name
	} else {
		return fmt.Sprintf("%s.%s", this.File.Name.Name, ident.Name)
	}
}

func (this *GoLanguageFrontend) handleStructTypeSpec(fset *token.FileSet, typeDecl *ast.TypeSpec, structType *ast.StructType) *cpg.RecordDeclaration {
	r := cpg.NewRecordDeclaration(fset, typeDecl)

	r.SetKind("struct")
	r.SetName(this.handleIdentAsName(typeDecl.Name))

	var scope = this.GetScopeManager()

	scope.EnterScope((*cpg.Node)(r))

	if !structType.Incomplete {
		for _, field := range structType.Fields.List {

			f := cpg.NewFieldDeclaration(fset, field)

			// a field can also have no name, which means that it is embedded, not quite
			// sure yet how to handle this, but since the embedded field can be accessed
			// by its type, it could make sense to name the field according to the type

			t := this.handleType(field.Type)

			if field.Names == nil {
				// retrieve the root type name
				var typeName = (*cpg.Node)(t.GetRoot()).GetName()

				this.LogDebug("Handling embedded field of type %s", typeName)

				f.SetName(typeName)
			} else {
				this.LogDebug("Handling field %s", field.Names[0].Name)

				// TODO: Multiple names?
				f.SetName(field.Names[0].Name)
			}

			f.SetType(t)

			scope.AddDeclaration((*cpg.Declaration)(f))
		}
	}

	scope.LeaveScope((*cpg.Node)(r))

	return r
}

func (this *GoLanguageFrontend) handleInterfaceTypeSpec(fset *token.FileSet, typeDecl *ast.TypeSpec, interfaceType *ast.InterfaceType) *cpg.RecordDeclaration {
	r := cpg.NewRecordDeclaration(fset, typeDecl)

	r.SetKind("interface")
	r.SetName(this.handleIdentAsName(typeDecl.Name))

	var scope = this.GetScopeManager()

	scope.EnterScope((*cpg.Node)(r))

	if !interfaceType.Incomplete {
		for _, method := range interfaceType.Methods.List {
			m := cpg.NewMethodDeclaration(fset, method)

			t := this.handleType(method.Type)

			m.SetType(t)
			m.SetName(method.Names[0].Name)

			scope.AddDeclaration((*cpg.Declaration)(m))
		}
	}

	scope.LeaveScope((*cpg.Node)(r))

	return r
}

func (this *GoLanguageFrontend) handleBlockStmt(fset *token.FileSet, blockStmt *ast.BlockStmt) *cpg.CompoundStatement {
	this.LogDebug("Handling block statement: %+v", *blockStmt)

	c := cpg.NewCompoundStatement(fset, blockStmt)

	// enter scope
	this.GetScopeManager().EnterScope((*cpg.Node)(c))

	for _, stmt := range blockStmt.List {
		var s *cpg.Statement

		s = this.handleStmt(fset, stmt)

		if s != nil {
			// add statement
			c.AddStatement(s)
		}
	}

	// leave scope
	this.GetScopeManager().LeaveScope((*cpg.Node)(c))

	return c
}

func (this *GoLanguageFrontend) handleForStmt(fset *token.FileSet, forStmt *ast.ForStmt) *cpg.ForStatement {
	this.LogDebug("Handling for statement: %+v", *forStmt)

	f := cpg.NewForStatement(fset, forStmt)

	var scope = this.GetScopeManager()

	scope.EnterScope((*cpg.Node)(f))

	if initStatement := this.handleStmt(fset, forStmt.Init); initStatement != nil {
		f.SetInitializerStatement(initStatement)
	}

	if condition := this.handleExpr(fset, forStmt.Cond); condition != nil {
		f.SetCondition(condition)
	}

	if iter := this.handleStmt(fset, forStmt.Post); iter != nil {
		f.SetIterationStatement(iter)
	}

	if body := this.handleStmt(fset, forStmt.Body); body != nil {
		f.SetStatement(body)
	}

	scope.LeaveScope((*cpg.Node)(f))

	return f
}

func (this *GoLanguageFrontend) handleReturnStmt(fset *token.FileSet, returnStmt *ast.ReturnStmt) *cpg.ReturnStatement {
	this.LogDebug("Handling return statement: %+v", *returnStmt)

	r := cpg.NewReturnStatement(fset, returnStmt)

	if returnStmt.Results != nil && len(returnStmt.Results) > 0 {
		e := this.handleExpr(fset, returnStmt.Results[0])

		// TODO: parse more than one result expression

		if e != nil {
			r.SetReturnValue(e)
		}
	} else {
		// TODO: connect result statement to result variables
	}

	return r
}

func (this *GoLanguageFrontend) handleIncDecStmt(fset *token.FileSet, incDecStmt *ast.IncDecStmt) *cpg.UnaryOperator {
	this.LogDebug("Handling decimal increment statement: %+v", *incDecStmt)

	u := cpg.NewUnaryOperator(fset, incDecStmt)

	if incDecStmt.Tok == token.INC {
		u.SetOperatorCode("++")
	}

	if incDecStmt.Tok == token.DEC {
		u.SetOperatorCode("--")
	}

	if input := this.handleExpr(fset, incDecStmt.X); input != nil {
		u.SetInput(input)
	}

	return u
}

func (this *GoLanguageFrontend) handleStmt(fset *token.FileSet, stmt ast.Stmt) (s *cpg.Statement) {
	this.LogDebug("Handling statement (%T): %+v", stmt, stmt)

	switch v := stmt.(type) {
	case *ast.ExprStmt:
		// in our cpg, each expression is also a statement,
		// so we do not need an expression statement wrapper
		s = (*cpg.Statement)(this.handleExpr(fset, v.X))
	case *ast.AssignStmt:
		s = (*cpg.Statement)(this.handleAssignStmt(fset, v))
	case *ast.DeclStmt:
		s = (*cpg.Statement)(this.handleDeclStmt(fset, v))
	case *ast.IfStmt:
		s = (*cpg.Statement)(this.handleIfStmt(fset, v))
	case *ast.SwitchStmt:
		s = (*cpg.Statement)(this.handleSwitchStmt(fset, v))
	case *ast.CaseClause:
		s = (*cpg.Statement)(this.handleCaseClause(fset, v))
	case *ast.BlockStmt:
		s = (*cpg.Statement)(this.handleBlockStmt(fset, v))
	case *ast.ForStmt:
		s = (*cpg.Statement)(this.handleForStmt(fset, v))
	case *ast.ReturnStmt:
		s = (*cpg.Statement)(this.handleReturnStmt(fset, v))
	case *ast.IncDecStmt:
		s = (*cpg.Statement)(this.handleIncDecStmt(fset, v))
	default:
		this.LogError("Not parsing statement of type %T yet: %+v", v, v)
		s = nil
	}

	if s != nil {
		this.handleComments((*cpg.Node)(s), stmt)
	}

	return
}

func (this *GoLanguageFrontend) handleExpr(fset *token.FileSet, expr ast.Expr) (e *cpg.Expression) {
	this.LogDebug("Handling expression (%T): %+v", expr, expr)

	switch v := expr.(type) {
	case *ast.CallExpr:
		e = (*cpg.Expression)(this.handleCallExpr(fset, v))
	case *ast.IndexExpr:
		e = (*cpg.Expression)(this.handleIndexExpr(fset, v))
	case *ast.BinaryExpr:
		e = (*cpg.Expression)(this.handleBinaryExpr(fset, v))
	case *ast.UnaryExpr:
		e = (*cpg.Expression)(this.handleUnaryExpr(fset, v))
	case *ast.SelectorExpr:
		e = (*cpg.Expression)(this.handleSelectorExpr(fset, v))
	case *ast.KeyValueExpr:
		e = (*cpg.Expression)(this.handleKeyValueExpr(fset, v))
	case *ast.BasicLit:
		e = (*cpg.Expression)(this.handleBasicLit(fset, v))
	case *ast.CompositeLit:
		e = (*cpg.Expression)(this.handleCompositeLit(fset, v))
	case *ast.Ident:
		e = (*cpg.Expression)(this.handleIdent(fset, v))
	default:
		this.LogError("Could not parse expression of type %T: %+v", v, v)
		// TODO: return an error instead?
		e = nil
	}

	if e != nil {
		this.handleComments((*cpg.Node)(e), expr)
	}

	return
}

func (this *GoLanguageFrontend) handleAssignStmt(fset *token.FileSet, assignStmt *ast.AssignStmt) (expr *cpg.Expression) {
	this.LogDebug("Handling assignment statement: %+v", assignStmt)

	// TODO: more than one Rhs?!
	rhs := this.handleExpr(fset, assignStmt.Rhs[0])

	if assignStmt.Tok == token.DEFINE {
		// lets create a variable declaration (wrapped with a declaration stmt) with this, because we define the variable here
		stmt := cpg.NewDeclarationStatement(fset, assignStmt)

		// TODO: assignment of multiple values
		d := cpg.NewVariableDeclaration(fset, assignStmt)

		var name = assignStmt.Lhs[0].(*ast.Ident).Name
		d.SetName(name)

		if rhs != nil {
			d.SetInitializer(rhs)
		}

		this.GetScopeManager().AddDeclaration((*cpg.Declaration)(d))

		stmt.SetSingleDeclaration((*cpg.Declaration)(d))

		expr = (*cpg.Expression)(stmt)
	} else {
		lhs := this.handleExpr(fset, assignStmt.Lhs[0])

		b := cpg.NewBinaryOperator(fset, assignStmt)

		b.SetOperatorCode("=")

		if lhs != nil {
			b.SetLHS(lhs)
		}

		if rhs != nil {
			b.SetRHS(rhs)
		}

		expr = (*cpg.Expression)(b)
	}

	return
}

func (this *GoLanguageFrontend) handleDeclStmt(fset *token.FileSet, declStmt *ast.DeclStmt) (expr *cpg.Expression) {
	this.LogDebug("Handling declaration statement: %+v", *declStmt)

	// lets create a variable declaration (wrapped with a declaration stmt) with this,
	// because we define the variable here
	stmt := cpg.NewDeclarationStatement(fset, declStmt)

	d := this.handleDecl(fset, declStmt.Decl)

	stmt.SetSingleDeclaration((*cpg.Declaration)(d))

	this.GetScopeManager().AddDeclaration(d)

	return (*cpg.Expression)(stmt)
}

func (this *GoLanguageFrontend) handleIfStmt(fset *token.FileSet, ifStmt *ast.IfStmt) (expr *cpg.Expression) {
	this.LogDebug("Handling if statement: %+v", *ifStmt)

	stmt := cpg.NewIfStatement(fset, ifStmt)

	var scope = this.GetScopeManager()

	scope.EnterScope((*cpg.Node)(stmt))

	init := this.handleStmt(fset, ifStmt.Init)
	if init != nil {
		stmt.SetInitializerStatement(init)
	}

	cond := this.handleExpr(fset, ifStmt.Cond)
	if cond != nil {
		stmt.SetCondition(cond)
	} else {
		this.LogError("If statement should really have a condition. It is either missing or could not be parsed.")
	}

	then := this.handleBlockStmt(fset, ifStmt.Body)
	stmt.SetThenStatement((*cpg.Statement)(then))

	els := this.handleStmt(fset, ifStmt.Else)
	if els != nil {
		stmt.SetElseStatement((*cpg.Statement)(els))
	}

	scope.LeaveScope((*cpg.Node)(stmt))

	return (*cpg.Expression)(stmt)
}

func (this *GoLanguageFrontend) handleSwitchStmt(fset *token.FileSet, switchStmt *ast.SwitchStmt) (expr *cpg.Expression) {
	this.LogDebug("Handling switch statement: %+v", *switchStmt)

	s := cpg.NewSwitchStatement(fset, switchStmt)

	if switchStmt.Init != nil {
		s.SetInitializerStatement(this.handleStmt(fset, switchStmt.Init))
	}

	if switchStmt.Tag != nil {
		s.SetCondition(this.handleExpr(fset, switchStmt.Tag))
	}

	s.SetStatement((*cpg.Statement)(this.handleBlockStmt(fset, switchStmt.Body))) // should only contain case clauses

	return (*cpg.Expression)(s)
}

func (this *GoLanguageFrontend) handleCaseClause(fset *token.FileSet, caseClause *ast.CaseClause) (expr *cpg.Expression) {
	this.LogDebug("Handling case clause: %+v", *caseClause)

	var s *cpg.Statement

	if caseClause.List == nil {
		s = (*cpg.Statement)(cpg.NewDefaultStatement(fset, nil))
	} else {
		c := cpg.NewCaseStatement(fset, caseClause)
		c.SetCaseExpression(this.handleExpr(fset, caseClause.List[0]))

		s = (*cpg.Statement)(c)
	}

	// need to find the current block / scope and add the statements to it
	block := this.GetScopeManager().GetCurrentBlock()

	// add the case statement
	if s != nil && block != nil && !block.IsNil() {
		block.AddStatement((*cpg.Statement)(s))
	}

	for _, stmt := range caseClause.Body {
		s = this.handleStmt(fset, stmt)

		if s != nil && block != nil && !block.IsNil() {
			// add statement
			block.AddStatement(s)
		}
	}

	// this is a little trick, to not add the case statement in handleStmt because we added it already.
	// otherwise, the order is screwed up.
	return nil
}

func (this *GoLanguageFrontend) handleCallExpr(fset *token.FileSet, callExpr *ast.CallExpr) *cpg.Expression {
	var c *cpg.CallExpression
	// parse the Fun field, to see which kind of expression it is
	var reference = this.handleExpr(fset, callExpr.Fun)

	if reference == nil {
		return nil
	}

	name := reference.GetName()

	if name == "new" {
		return this.handleNewExpr(fset, callExpr)
	} else if name == "make" {
		return this.handleMakeExpr(fset, callExpr)
	}

	isMemberExpression, err := (*jnigi.ObjectRef)(reference).IsInstanceOf(env, "de/fraunhofer/aisec/cpg/graph/statements/expressions/MemberExpression")
	if err != nil {
		log.Fatal(err)

	}

	if isMemberExpression {
		baseName := (*cpg.Node)((*cpg.MemberExpression)(reference).GetBase()).GetName()
		// this is not 100% accurate since it should be rather the type not the base name
		// but FQNs are really broken in the CPG so this is ok for now
		fqn := fmt.Sprintf("%s.%s", baseName, name)

		this.LogDebug("Fun is a member call to %s", name)

		m := cpg.NewMemberCallExpression(fset, callExpr)
		m.SetName(name)
		m.SetFqn(fqn)

		member := cpg.NewDeclaredReferenceExpression(fset, nil)
		member.SetName(name)

		m.SetBase((*cpg.MemberExpression)(reference).GetBase())
		m.SetMember(member.Node())

		c = (*cpg.CallExpression)(m)
	} else {
		this.LogDebug("Handling regular call expression to %s", name)

		c = cpg.NewCallExpression(fset, callExpr)

		// the name is already a FQN if it contains a dot
		pos := strings.LastIndex(name, ".")
		if pos != -1 {
			fqn := name

			c.SetFqn(fqn)

			// need to have the short name
			c.SetName(name[pos+1:])
		} else {
			c.SetName(name)
		}

	}

	for _, arg := range callExpr.Args {
		e := this.handleExpr(fset, arg)

		if e != nil {
			c.AddArgument(e)
		}
	}

	// reference.disconnectFromGraph()

	return (*cpg.Expression)(c)
}

func (this *GoLanguageFrontend) handleIndexExpr(fset *token.FileSet, indexExpr *ast.IndexExpr) *cpg.Expression {
	a := cpg.NewArraySubscriptionExpression(fset, indexExpr)

	a.SetArrayExpression(this.handleExpr(fset, indexExpr.X))
	a.SetSubscriptExpression(this.handleExpr(fset, indexExpr.Index))

	return (*cpg.Expression)(a)
}

func (this *GoLanguageFrontend) handleNewExpr(fset *token.FileSet, callExpr *ast.CallExpr) *cpg.Expression {
	n := cpg.NewNewExpression(fset, callExpr)

	// first argument is type
	t := this.handleType(callExpr.Args[0])

	// new is a pointer, so need to reference the type with a pointer
	pointer, err := env.GetStaticField("de/fraunhofer/aisec/cpg/graph/types/PointerType$PointerOrigin", "POINTER", jnigi.ObjectType("de/fraunhofer/aisec/cpg/graph/types/PointerType$PointerOrigin"))
	if err != nil {
		log.Fatal(err)
	}

	(*cpg.HasType)(n).SetType(t.Reference(pointer.(*jnigi.ObjectRef)))

	// a new expression also needs an initializer, which is usually a constructexpression
	c := cpg.NewConstructExpression(fset, callExpr)
	(*cpg.HasType)(c).SetType(t)

	n.SetInitializer((*cpg.Expression)(c))

	return (*cpg.Expression)(n)
}

func (this *GoLanguageFrontend) handleMakeExpr(fset *token.FileSet, callExpr *ast.CallExpr) *cpg.Expression {
	var n *cpg.Expression

	if callExpr.Args == nil || len(callExpr.Args) < 1 {
		return nil
	}

	// first argument is always the type, handle it
	t := this.handleType(callExpr.Args[0])

	// actually make() can make more than just arrays, i.e. channels and maps
	if _, isArray := callExpr.Args[0].(*ast.ArrayType); isArray {
		r := cpg.NewArrayCreationExpression(fset, callExpr)

		// second argument is a dimension (if this is an array), usually a literal
		if len(callExpr.Args) > 1 {
			d := this.handleExpr(fset, callExpr.Args[1])

			r.AddDimension(d)
		}

		n = (*cpg.Expression)(r)
	} else {
		// create at least a generic construct expression for the given map or channel type
		// and provide the remaining arguments

		c := cpg.NewConstructExpression(fset, callExpr)

		// pass the remaining arguments
		for _, arg := range callExpr.Args[1:] {
			a := this.handleExpr(fset, arg)

			c.AddArgument(a)
		}

		n = (*cpg.Expression)(c)
	}

	// set the type, we have parsed earlier
	(*cpg.HasType)(n).SetType(t)

	return n
}

func (this *GoLanguageFrontend) handleBinaryExpr(fset *token.FileSet, binaryExpr *ast.BinaryExpr) *cpg.BinaryOperator {
	b := cpg.NewBinaryOperator(fset, binaryExpr)

	lhs := this.handleExpr(fset, binaryExpr.X)
	rhs := this.handleExpr(fset, binaryExpr.Y)

	err := b.SetOperatorCode(binaryExpr.Op.String())
	if err != nil {
		log.Fatal(err)

	}

	if lhs != nil {
		b.SetLHS(lhs)
	}

	if rhs != nil {
		b.SetRHS(rhs)
	}

	return b
}

func (this *GoLanguageFrontend) handleUnaryExpr(fset *token.FileSet, unaryExpr *ast.UnaryExpr) *cpg.UnaryOperator {
	u := cpg.NewUnaryOperator(fset, unaryExpr)

	input := this.handleExpr(fset, unaryExpr.X)

	err := u.SetOperatorCode(unaryExpr.Op.String())
	if err != nil {
		log.Fatal(err)
	}

	if input != nil {
		u.SetInput(input)
	}

	return u
}

func (this *GoLanguageFrontend) handleSelectorExpr(fset *token.FileSet, selectorExpr *ast.SelectorExpr) *cpg.DeclaredReferenceExpression {
	base := this.handleExpr(fset, selectorExpr.X)

	// check, if this just a regular reference to a variable with a package scope and not a member expression
	var isMemberExpression bool = true
	for _, imp := range this.File.Imports {
		if base.GetName() == getImportName(imp) {
			// found a package name, so this is NOT a member expression
			isMemberExpression = false
		}
	}

	var decl *cpg.DeclaredReferenceExpression
	if isMemberExpression {
		m := cpg.NewMemberExpression(fset, selectorExpr)
		m.SetBase(base)
		(*cpg.Node)(m).SetName(selectorExpr.Sel.Name)

		decl = (*cpg.DeclaredReferenceExpression)(m)
	} else {
		decl = cpg.NewDeclaredReferenceExpression(fset, selectorExpr)

		// we need to set the name to a FQN-style, including the package scope. the call resolver will then resolve this
		fqn := fmt.Sprintf("%s.%s", base.GetName(), selectorExpr.Sel.Name)
		decl.SetName(fqn)
	}

	// For now we just let the VariableUsageResolver handle this. Therefore,
	// we can not differentiate between field access to a receiver, an object
	// or a const field within a package at this point.

	// check, if the base relates to a receiver
	/*var method = (*cpg.MethodDeclaration)((*jnigi.ObjectRef)(this.GetScopeManager().GetCurrentFunction()).Cast("de/fraunhofer/aisec/cpg/graph/declarations/MethodDeclaration"))

	if method != nil && !method.IsNil() {
		//recv := method.GetReceiver()

		// this refers to our receiver
		if (*cpg.Node)(recv).GetName() == (*cpg.Node)(base).GetName() {

			(*cpg.DeclaredReferenceExpression)(base).SetRefersTo(recv.Declaration())
		}
	}*/

	return decl
}

func (this *GoLanguageFrontend) handleKeyValueExpr(fset *token.FileSet, expr *ast.KeyValueExpr) *cpg.KeyValueExpression {
	this.LogDebug("Handling key value expression %+v", *expr)

	k := cpg.NewKeyValueExpression(fset, expr)

	keyExpr := this.handleExpr(fset, expr.Key)
	if keyExpr != nil {
		k.SetKey(keyExpr)
	}

	valueExpr := this.handleExpr(fset, expr.Value)
	if valueExpr != nil {
		k.SetValue(valueExpr)
	}

	return k
}

func (this *GoLanguageFrontend) handleBasicLit(fset *token.FileSet, lit *ast.BasicLit) *cpg.Literal {
	this.LogDebug("Handling literal %+v", *lit)

	l := cpg.NewLiteral(fset, lit)

	var value interface{}
	var t *cpg.Type

	switch lit.Kind {
	case token.STRING:
		// strip the "
		value = cpg.NewString(lit.Value[1 : len(lit.Value)-1])
		t = cpg.TypeParser_createFrom("string", false)
	case token.INT:
		i, _ := strconv.ParseInt(lit.Value, 10, 64)
		value = cpg.NewInteger(int(i))
		t = cpg.TypeParser_createFrom("int", false)
	case token.FLOAT:
		// default seems to be float64
		f, _ := strconv.ParseFloat(lit.Value, 64)
		value = cpg.NewDouble(f)
		t = cpg.TypeParser_createFrom("float64", false)
	case token.IMAG:
	case token.CHAR:
		value = cpg.NewString(lit.Value)
		break
	}

	l.SetType(t)
	l.SetValue(value)

	return l
}

// handleCompositeLit handles a composite literal, which we need to translate into a combination of a
// ConstructExpression and a list of KeyValueExpressions. The problem is that we need to add the list
// as a first argument of the construct expression.
func (this *GoLanguageFrontend) handleCompositeLit(fset *token.FileSet, lit *ast.CompositeLit) *cpg.ConstructExpression {
	this.LogDebug("Handling composite literal %+v", *lit)

	c := cpg.NewConstructExpression(fset, lit)

	// parse the type field, to see which kind of expression it is
	var reference = this.handleExpr(fset, lit.Type)

	if reference == nil {
		return nil
	}

	(*cpg.Node)(c).SetName(reference.GetName())

	l := cpg.NewInitializerListExpression(fset, lit)

	c.AddArgument((*cpg.Expression)(l))

	for _, elem := range lit.Elts {
		expr := this.handleExpr(fset, elem)

		if expr != nil {
			l.AddInitializer(expr)
		}
	}

	return c
}

func (this *GoLanguageFrontend) handleIdent(fset *token.FileSet, ident *ast.Ident) *cpg.DeclaredReferenceExpression {
	ref := cpg.NewDeclaredReferenceExpression(fset, ident)

	tu := this.GetCurrentTU()

	// check, if this refers to a package import
	i := tu.GetIncludeByName(ident.Name)

	// then set the refersTo, because our regular CPG passes will not resolve them
	if i != nil && !(*jnigi.ObjectRef)(i).IsNil() {
		ref.SetRefersTo((*cpg.Declaration)(i))
	}

	ref.SetName(ident.Name)

	return ref
}

func (this *GoLanguageFrontend) handleType(typeExpr ast.Expr) *cpg.Type {
	this.LogDebug("Parsing type %T: %+v", typeExpr, typeExpr)

	switch v := typeExpr.(type) {
	case *ast.Ident:
		// make it a fqn according to the current package to make things easier
		fqn := this.handleIdentAsName(v)

		this.LogDebug("FQN type: %s", fqn)
		return cpg.TypeParser_createFrom(fqn, false)
	case *ast.SelectorExpr:
		// small shortcut
		fqn := fmt.Sprintf("%s.%s", v.X.(*ast.Ident).Name, v.Sel.Name)
		this.LogDebug("FQN type: %s", fqn)
		return cpg.TypeParser_createFrom(fqn, false)
	case *ast.StarExpr:
		t := this.handleType(v.X)

		i, err := env.GetStaticField("de/fraunhofer/aisec/cpg/graph/types/PointerType$PointerOrigin", "POINTER", jnigi.ObjectType("de/fraunhofer/aisec/cpg/graph/types/PointerType$PointerOrigin"))
		if err != nil {
			log.Fatal(err)
		}

		this.LogDebug("Pointer to %s", (*cpg.Node)(t).GetName())

		return t.Reference(i.(*jnigi.ObjectRef))
	case *ast.ArrayType:
		t := this.handleType(v.Elt)

		i, err := env.GetStaticField("de/fraunhofer/aisec/cpg/graph/types/PointerType$PointerOrigin", "ARRAY", jnigi.ObjectType("de/fraunhofer/aisec/cpg/graph/types/PointerType$PointerOrigin"))
		if err != nil {
			log.Fatal(err)
		}

		this.LogDebug("Array of %s", (*cpg.Node)(t).GetName())

		return t.Reference(i.(*jnigi.ObjectRef))
	case *ast.MapType:
		// we cannot properly represent Golangs built-in map types, yet so we have
		// to make a shortcut here and represent it as a Java-like map<K, V> type.
		t := cpg.TypeParser_createFrom("map", false)
		keyType := this.handleType(v.Key)
		valueType := this.handleType(v.Value)

		(*cpg.ObjectType)(t).AddGeneric(keyType)
		(*cpg.ObjectType)(t).AddGeneric(valueType)

		return t
	case *ast.ChanType:
		// handle them similar to maps
		t := cpg.TypeParser_createFrom("chan", false)
		chanType := this.handleType(v.Value)

		(*cpg.ObjectType)(t).AddGeneric(chanType)

		return t
	case *ast.FuncType:
		if v.Results == nil {
			return cpg.TypeParser_createFrom("void", false)
		} else {
			// for now, we are only interested in the return type
			return this.handleType(v.Results.List[0].Type)
		}
	}

	return cpg.UnknownType_getUnknown()
}

func (this *GoLanguageFrontend) isBuiltinType(s string) bool {
	switch s {
	case "bool":
		fallthrough
	case "byte":
		fallthrough
	case "complex128":
		fallthrough
	case "complex64":
		fallthrough
	case "error":
		fallthrough
	case "float32":
		fallthrough
	case "float64":
		fallthrough
	case "int":
		fallthrough
	case "int16":
		fallthrough
	case "int32":
		fallthrough
	case "int64":
		fallthrough
	case "int8":
		fallthrough
	case "rune":
		fallthrough
	case "string":
		fallthrough
	case "uint":
		fallthrough
	case "uint16":
		fallthrough
	case "uint32":
		fallthrough
	case "uint64":
		fallthrough
	case "uint8":
		fallthrough
	case "uintptr":
		return true
	default:
		return false
	}
}
