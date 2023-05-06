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
	"path/filepath"
	"strconv"
	"strings"

	"golang.org/x/mod/modfile"
	"tekao.net/jnigi"
)

const MetadataProviderClass = cpg.GraphPackage + "/MetadataProvider"
const LanguageProviderClass = cpg.GraphPackage + "/LanguageProvider"

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
	tu = this.NewTranslationUnitDeclaration(fset, file, path)

	scope := this.GetScopeManager()

	// reset scope
	scope.ResetToGlobal((*cpg.Node)(tu))

	this.CurrentTU = tu

	for _, imprt := range file.Imports {
		i := this.handleImportSpec(fset, imprt)

		err = scope.AddDeclaration((*cpg.Declaration)(i))
		if err != nil {
			log.Fatal(err)
		}
	}

	// Create a new namespace declaration, representing the package
	p := this.NewNamespaceDeclaration(fset, nil, file.Name.Name)

	// we need to construct the package "path" (e.g. "encoding/json") out of the
	// module path as well as the current directory in relation to the topLevel
	packagePath := filepath.Dir(path)

	// Construct a relative path starting from the top level
	packagePath, err = filepath.Rel(this.TopLevel, packagePath)
	if err == nil {
		// If we are in a module, we need to prepend the module path to it
		if this.Module != nil {
			packagePath = filepath.Join(this.Module.Module.Mod.Path, packagePath)
		}

		p.SetPath(packagePath)
	} else {
		this.LogError("Could not relativize package path to top level. Cannot set package path: %v", err)
	}

	// enter scope
	scope.EnterScope((*cpg.Node)(p))

	for _, decl := range file.Decls {
		// Retrieve all top level declarations. One "Decl" could potentially
		// contain multiple CPG declarations.
		decls := this.handleDecl(fset, decl)

		for _, d := range decls {
			if d != nil {
				// Add declaration to current scope. This will also add it to the
				// respective AST scope holder
				err = scope.AddDeclaration((*cpg.Declaration)(d))
				if err != nil {
					log.Fatal(err)

				}
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
	this.LogTrace("Handling comments for %+v", astNode)

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

		this.LogTrace("Comments: %+v", comment)
	}
}

// handleDecl parses an [ast.Decl]. Note, that in a "Decl", one or more actual
// declarations can be found. Therefore, this function returns a slice of
// [cpg.Declaration].
func (this *GoLanguageFrontend) handleDecl(fset *token.FileSet, decl ast.Decl) (decls []*cpg.Declaration) {
	this.LogTrace("Handling declaration (%T): %+v", decl, decl)

	decls = []*cpg.Declaration{}

	switch v := decl.(type) {
	case *ast.FuncDecl:
		// There can be only a single function declaration
		decls = append(decls, (*cpg.Declaration)(this.handleFuncDecl(fset, v)))
	case *ast.GenDecl:
		// GenDecl can hold multiple declarations
		decls = this.handleGenDecl(fset, v)
	default:
		this.LogError("Not parsing declaration of type %T yet: %+v", v, v)
		// TODO: Return a ProblemDeclaration
	}

	// Handle comments for all declarations
	for _, d := range decls {
		// TODO: This is problematic because we are assigning it the wrong node
		if d != nil {
			this.handleComments((*cpg.Node)(d), decl)
		}
	}

	return
}

func (this *GoLanguageFrontend) handleFuncDecl(fset *token.FileSet, funcDecl *ast.FuncDecl) *cpg.FunctionDeclaration {
	this.LogTrace("Handling func Decl: %+v", *funcDecl)

	var scope = this.GetScopeManager()
	var receiver *cpg.VariableDeclaration

	var f *cpg.FunctionDeclaration
	if funcDecl.Recv != nil {
		m := this.NewMethodDeclaration(fset, funcDecl, funcDecl.Name.Name)

		// TODO: why is this a list?
		var recv = funcDecl.Recv.List[0]

		var recordType = this.handleType(fset, recv.Type)

		// The name of the Go receiver is optional. In fact, if the name is not
		// specified we probably do not need any receiver variable at all,
		// because the syntax is only there to ensure that this method is part
		// of the struct, but it is not modifying the receiver.
		if len(recv.Names) > 0 {
			receiver = this.NewVariableDeclaration(fset, nil, recv.Names[0].Name)

			// TODO: should we use the FQN here? FQNs are a mess in the CPG...
			receiver.SetType(recordType)

			err := m.SetReceiver(receiver)
			if err != nil {
				log.Fatal(err)
			}
		}

		if recordType != nil {
			var recordName = recordType.GetName()

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
				this.LogTrace("Record: %+v", record)

				err = record.AddMethod(m)
				if err != nil {
					log.Fatal(err)

				}
			}
		}

		f = (*cpg.FunctionDeclaration)(m)
	} else {
		// We do not want to prefix the package for an empty (lambda) function name
		var localNameOnly bool = false
		if funcDecl.Name.Name == "" {
			localNameOnly = true
		}

		f = this.NewFunctionDeclaration(fset, funcDecl, funcDecl.Name.Name, "", localNameOnly)
	}

	// enter scope for function
	scope.EnterScope((*cpg.Node)(f))

	if receiver != nil {
		this.LogTrace("Adding receiver %s", (*cpg.Node)(receiver).GetName())

		// add the receiver do the scope manager, so we can resolve the receiver value
		this.GetScopeManager().AddDeclaration((*cpg.Declaration)(receiver))
	}

	var t *cpg.Type = this.handleType(fset, funcDecl.Type)
	var returnTypes []*cpg.Type = []*cpg.Type{}

	if funcDecl.Type.Results != nil {
		for _, returnVariable := range funcDecl.Type.Results.List {
			returnTypes = append(returnTypes, this.handleType(fset, returnVariable.Type))

			// if the function has named return variables, be sure to declare them as well
			if returnVariable.Names != nil {
				p := this.NewVariableDeclaration(fset, returnVariable, returnVariable.Names[0].Name)

				p.SetType(this.handleType(fset, returnVariable.Type))

				// add parameter to scope
				this.GetScopeManager().AddDeclaration((*cpg.Declaration)(p))
			}
		}
	}

	this.LogTrace("Function has type %s", t.GetName())

	f.SetType(t)
	f.SetReturnTypes(returnTypes)

	// TODO: for other languages, we would enter the record declaration, if
	// this is a method; however I am not quite sure if this makes sense for
	// go, since we do not have a 'this', but rather a named receiver

	for _, param := range funcDecl.Type.Params.List {
		this.LogTrace("Parsing param: %+v", param)

		var name string
		// Somehow parameters end up having no name sometimes, have not fully understood why.
		if len(param.Names) > 0 {
			// TODO: more than one name?
			name = param.Names[0].Name

			// If the name is an underscore, it means that the parameter is
			// unnamed. In order to avoid confusing and some compatibility with
			// other languages, we are just setting the name to an empty string
			// in this case.
			if name == "_" {
				name = ""
			}
		} else {
			this.LogError("Some param has no name, which is a bit weird: %+v", param)
		}

		p := this.NewParamVariableDeclaration(fset, param, name)

		// Check for varargs. In this case we want to parse the element type
		// (and make it an array afterwards)
		if ell, ok := param.Type.(*ast.Ellipsis); ok {
			p.SetVariadic(true)
			var t = this.handleType(fset, ell.Elt)

			var i = jnigi.NewObjectRef(cpg.PointerOriginClass)
			err := env.GetStaticField(cpg.PointerOriginClass, "ARRAY", i)
			if err != nil {
				log.Fatal(err)
			}

			p.SetType(t.Reference(i))
		} else {
			p.SetType(this.handleType(fset, param.Type))
		}

		// add parameter to scope
		this.GetScopeManager().AddDeclaration((*cpg.Declaration)(p))

		this.handleComments((*cpg.Node)(p), param)
	}

	this.LogTrace("Parsing function body of %s", (*cpg.Node)(f).GetName())

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

	return f
}

func (this *GoLanguageFrontend) handleGenDecl(fset *token.FileSet, genDecl *ast.GenDecl) (decls []*cpg.Declaration) {
	decls = []*cpg.Declaration{}

	for _, spec := range genDecl.Specs {
		switch v := spec.(type) {
		case *ast.ValueSpec:
			decls = append(decls, this.handleValueSpec(fset, v)...)
		case *ast.TypeSpec:
			decls = append(decls, this.handleTypeSpec(fset, v))
		case *ast.ImportSpec:
			// Somehow these end up duplicate in the AST, so do not handle them here
		default:
			this.LogError("Not parsing specification of type %T yet: %+v", v, v)
		}
	}

	return
}

// handleValueSpec handles parsing of an [ast.ValueSpec], which is a variable
// declaration. Since this can potentially declare multiple variables with one
// "spec", this returns a slice of [cpg.Declaration].
func (this *GoLanguageFrontend) handleValueSpec(fset *token.FileSet, valueDecl *ast.ValueSpec) (decls []*cpg.Declaration) {
	decls = []*cpg.Declaration{}

	// We need to declare one variable for each name
	for idx, ident := range valueDecl.Names {
		d := this.NewVariableDeclaration(fset, valueDecl, ident.Name)

		// Handle the type (if its there)
		if valueDecl.Type != nil {
			t := this.handleType(fset, valueDecl.Type)

			d.SetType(t)
		}

		// There could either be no initializers, otherwise the amount of values
		// must match the names
		lenValues := len(valueDecl.Values)
		if lenValues != 0 && lenValues != len(valueDecl.Names) {
			this.LogError("Number of initializers does not match number of names. Initializers might be incomplete")
		}

		// The initializer is in the "Values" slice with the respective index
		if len(valueDecl.Values) > idx {
			var expr = this.handleExpr(fset, valueDecl.Values[idx])

			err := d.SetInitializer(expr)
			if err != nil {
				log.Fatal(err)
			}
		}

		decls = append(decls, d.Declaration())
	}

	return decls
}

// handleTypeSpec handles an [ast.TypeSec], which defines either a struct or an
// interface. It returns a single [cpg.Declaration].
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
	this.LogTrace("Import specifier with: %+v)", *importSpec)

	i := this.NewIncludeDeclaration(fset, importSpec, getImportName(importSpec))

	var scope = this.GetScopeManager()

	i.SetFilename(importSpec.Path.Value[1 : len(importSpec.Path.Value)-1])

	err := scope.AddDeclaration((*cpg.Declaration)(i))
	if err != nil {
		log.Fatal(err)
	}

	return (*cpg.Declaration)(i)
}

func (this *GoLanguageFrontend) handleIdentAsName(ident *ast.Ident) string {
	return ident.Name
}

func (this *GoLanguageFrontend) handleStructTypeSpec(fset *token.FileSet, typeDecl *ast.TypeSpec, structType *ast.StructType) *cpg.RecordDeclaration {
	r := this.NewRecordDeclaration(fset, typeDecl, this.handleIdentAsName(typeDecl.Name), "struct")

	var scope = this.GetScopeManager()

	scope.EnterScope((*cpg.Node)(r))

	if !structType.Incomplete {
		for _, field := range structType.Fields.List {

			// a field can also have no name, which means that it is embedded, not quite
			// sure yet how to handle this, but since the embedded field can be accessed
			// by its type, it could make sense to name the field according to the type

			var name string
			t := this.handleType(fset, field.Type)

			if field.Names == nil {
				// retrieve the root type name
				var typeName = t.GetRoot().GetName().ToString()

				this.LogTrace("Handling embedded field of type %s", typeName)

				name = typeName
			} else {
				this.LogTrace("Handling field %s", field.Names[0].Name)

				// TODO: Multiple names?
				name = field.Names[0].Name
			}

			f := this.NewFieldDeclaration(fset, field, name)

			f.SetType(t)

			scope.AddDeclaration((*cpg.Declaration)(f))
		}
	}

	scope.LeaveScope((*cpg.Node)(r))

	return r
}

func (this *GoLanguageFrontend) handleInterfaceTypeSpec(fset *token.FileSet, typeDecl *ast.TypeSpec, interfaceType *ast.InterfaceType) *cpg.RecordDeclaration {
	r := this.NewRecordDeclaration(fset, typeDecl, this.handleIdentAsName(typeDecl.Name), "interface")

	var scope = this.GetScopeManager()

	scope.EnterScope((*cpg.Node)(r))

	if !interfaceType.Incomplete {
		for _, method := range interfaceType.Methods.List {
			t := this.handleType(fset, method.Type)

			// Even though this list is called "Methods", it contains all kinds
			// of things, so we need to proceed with caution. Only if the
			// "method" actually has a name, we declare a new method
			// declaration.
			if len(method.Names) > 0 {
				m := this.NewMethodDeclaration(fset, method, method.Names[0].Name)
				m.SetType(t)

				scope.AddDeclaration((*cpg.Declaration)(m))
			} else {
				this.LogTrace("Adding %s as super class of interface %s", t.GetName(), (*cpg.Node)(r).GetName())
				// Otherwise, it contains either types or interfaces. For now we
				// hope that it only has interfaces. We consider embedded
				// interfaces as sort of super types for this interface.
				r.AddSuperClass(t)
			}
		}
	}

	scope.LeaveScope((*cpg.Node)(r))

	return r
}

func (this *GoLanguageFrontend) handleBlockStmt(fset *token.FileSet, blockStmt *ast.BlockStmt) *cpg.CompoundStatement {
	this.LogTrace("Handling block statement: %+v", *blockStmt)

	c := this.NewCompoundStatement(fset, blockStmt)

	// enter scope
	this.GetScopeManager().EnterScope((*cpg.Node)(c))

	for _, stmt := range blockStmt.List {
		var s *cpg.Statement

		s = this.handleStmt(fset, stmt, blockStmt)

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
	this.LogTrace("Handling for statement: %+v", *forStmt)

	f := this.NewForStatement(fset, forStmt)

	var scope = this.GetScopeManager()

	scope.EnterScope((*cpg.Node)(f))

	if forStmt.Init != nil {
		initStatement := this.handleStmt(fset, forStmt.Init, forStmt)
		f.SetInitializerStatement(initStatement)
	}

	if forStmt.Cond != nil {
		condition := this.handleExpr(fset, forStmt.Cond)
		f.SetCondition(condition)
	}

	if forStmt.Post != nil {
		iter := this.handleStmt(fset, forStmt.Post, forStmt)
		f.SetIterationStatement(iter)
	}

	if body := this.handleStmt(fset, forStmt.Body, forStmt); body != nil {
		f.SetStatement(body)
	}

	scope.LeaveScope((*cpg.Node)(f))

	return f
}

func (this *GoLanguageFrontend) handleRangeStmt(fset *token.FileSet, rangeStmt *ast.RangeStmt) *cpg.ForEachStatement {
	this.LogTrace("Handling range statement: %+v", *rangeStmt)

	f := this.NewForEachStatement(fset, rangeStmt)

	var scope = this.GetScopeManager()

	scope.EnterScope((*cpg.Node)(f))

	// TODO: Support other use cases that do not use DEFINE
	if rangeStmt.Tok == token.DEFINE {
		stmt := this.NewDeclarationStatement(fset, rangeStmt)

		// TODO: not really the best way to deal with this
		// TODO: key type is always int. we could set this
		var keyName = rangeStmt.Key.(*ast.Ident).Name

		key := this.NewVariableDeclaration(fset, rangeStmt.Key, keyName)
		this.GetScopeManager().AddDeclaration((*cpg.Declaration)(key))
		stmt.AddToPropertyEdgeDeclaration((*cpg.Declaration)(key))

		if rangeStmt.Value != nil {
			// TODO: not really the best way to deal with this
			// TODO: key type is always int. we could set this
			var valueName = rangeStmt.Value.(*ast.Ident).Name

			value := this.NewVariableDeclaration(fset, rangeStmt.Key, valueName)
			this.GetScopeManager().AddDeclaration((*cpg.Declaration)(value))
			stmt.AddToPropertyEdgeDeclaration((*cpg.Declaration)(value))
		}

		f.SetVariable((*cpg.Statement)(stmt))
	}

	iterable := (*cpg.Statement)(this.handleExpr(fset, rangeStmt.X))
	f.SetIterable(iterable)

	body := this.handleStmt(fset, rangeStmt.Body, rangeStmt)
	f.SetStatement(body)

	scope.LeaveScope((*cpg.Node)(f))

	return f
}

func (this *GoLanguageFrontend) handleReturnStmt(fset *token.FileSet, returnStmt *ast.ReturnStmt) *cpg.ReturnStatement {
	this.LogTrace("Handling return statement: %+v", *returnStmt)

	r := this.NewReturnStatement(fset, returnStmt)

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
	this.LogTrace("Handling decimal increment statement: %+v", *incDecStmt)

	var opCode string
	if incDecStmt.Tok == token.INC {
		opCode = "++"
	}

	if incDecStmt.Tok == token.DEC {
		opCode = "--"
	}

	u := this.NewUnaryOperator(fset, incDecStmt, opCode, true, false)

	if input := this.handleExpr(fset, incDecStmt.X); input != nil {
		u.SetInput(input)
	}

	return u
}

func (this *GoLanguageFrontend) handleStmt(fset *token.FileSet, stmt ast.Stmt, parent ast.Stmt) (s *cpg.Statement) {
	this.LogTrace("Handling statement (%T): %+v", stmt, stmt)

	switch v := stmt.(type) {
	case *ast.ExprStmt:
		// in our cpg, each expression is also a statement,
		// so we do not need an expression statement wrapper
		s = (*cpg.Statement)(this.handleExpr(fset, v.X))
	case *ast.AssignStmt:
		s = (*cpg.Statement)(this.handleAssignStmt(fset, v, parent))
	case *ast.DeclStmt:
		s = (*cpg.Statement)(this.handleDeclStmt(fset, v))
	case *ast.GoStmt:
		s = (*cpg.Statement)(this.handleGoStmt(fset, v))
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
	case *ast.RangeStmt:
		s = (*cpg.Statement)(this.handleRangeStmt(fset, v))
	case *ast.ReturnStmt:
		s = (*cpg.Statement)(this.handleReturnStmt(fset, v))
	case *ast.IncDecStmt:
		s = (*cpg.Statement)(this.handleIncDecStmt(fset, v))
	default:
		msg := fmt.Sprintf("Not parsing statement of type %T yet: %s", v, code(fset, v))
		this.LogError(msg)
		s = (*cpg.Statement)(this.NewProblemExpression(fset, v, msg))
	}

	if s != nil {
		this.handleComments((*cpg.Node)(s), stmt)
	}

	return
}

func (this *GoLanguageFrontend) handleExpr(fset *token.FileSet, expr ast.Expr) (e *cpg.Expression) {
	this.LogTrace("Handling expression (%T): %+v", expr, expr)

	switch v := expr.(type) {
	case *ast.CallExpr:
		e = (*cpg.Expression)(this.handleCallExpr(fset, v))
	case *ast.IndexExpr:
		e = (*cpg.Expression)(this.handleIndexExpr(fset, v))
	case *ast.BinaryExpr:
		e = (*cpg.Expression)(this.handleBinaryExpr(fset, v))
	case *ast.UnaryExpr:
		e = (*cpg.Expression)(this.handleUnaryExpr(fset, v))
	case *ast.StarExpr:
		e = (*cpg.Expression)(this.handleStarExpr(fset, v))
	case *ast.SelectorExpr:
		e = (*cpg.Expression)(this.handleSelectorExpr(fset, v))
	case *ast.SliceExpr:
		e = (*cpg.Expression)(this.handleSliceExpr(fset, v))
	case *ast.KeyValueExpr:
		e = (*cpg.Expression)(this.handleKeyValueExpr(fset, v))
	case *ast.BasicLit:
		e = (*cpg.Expression)(this.handleBasicLit(fset, v))
	case *ast.CompositeLit:
		e = (*cpg.Expression)(this.handleCompositeLit(fset, v))
	case *ast.FuncLit:
		e = (*cpg.Expression)(this.handleFuncLit(fset, v))
	case *ast.Ident:
		e = (*cpg.Expression)(this.handleIdent(fset, v))
	case *ast.TypeAssertExpr:
		e = (*cpg.Expression)(this.handleTypeAssertExpr(fset, v))
	case *ast.ParenExpr:
		e = this.handleExpr(fset, v.X)
	default:
		msg := fmt.Sprintf("Not parsing expression of type %T yet: %s", v, code(fset, v))
		this.LogError(msg)
		e = (*cpg.Expression)(this.NewProblemExpression(fset, v, msg))
	}

	if e != nil {
		this.handleComments((*cpg.Node)(e), expr)
	}

	return
}

func (this *GoLanguageFrontend) handleAssignStmt(fset *token.FileSet, assignStmt *ast.AssignStmt, parent ast.Stmt) (expr *cpg.Expression) {
	this.LogTrace("Handling assignment statement: %+v", assignStmt)

	this.LogDebug("Parent: %#v", parent)

	var rhs = []*cpg.Expression{}
	var lhs = []*cpg.Expression{}
	for _, expr := range assignStmt.Lhs {
		lhs = append(lhs, this.handleExpr(fset, expr))
	}

	for _, expr := range assignStmt.Rhs {
		rhs = append(rhs, this.handleExpr(fset, expr))
	}

	a := this.NewAssignExpression(fset, assignStmt, "=")

	a.SetLHS(lhs)
	a.SetRHS(rhs)

	// We need to explicitly set the operator code on this assignment as
	// something which potentially declares a variable, so we can resolve this
	// in our extra pass.
	if assignStmt.Tok == token.DEFINE {
		a.SetOperatorCode(":=")
	}

	expr = (*cpg.Expression)(a)

	return
}

func (this *GoLanguageFrontend) handleDeclStmt(fset *token.FileSet, declStmt *ast.DeclStmt) (expr *cpg.Expression) {
	this.LogTrace("Handling declaration statement: %+v", *declStmt)

	// Lets create a variable declaration (wrapped with a declaration stmt) with
	// this, because we define the variable here
	stmt := this.NewDeclarationStatement(fset, declStmt)

	decls := this.handleDecl(fset, declStmt.Decl)

	// Loop over the declarations and add them to the scope as well as the statement.
	for _, d := range decls {
		stmt.AddToPropertyEdgeDeclaration(d)
		this.GetScopeManager().AddDeclaration(d)
	}

	return (*cpg.Expression)(stmt)
}

// handleGoStmt handles the `go` statement, which is a special keyword in go
// that starts the supplied call expression in a separate Go routine. We cannot
// model this 1:1, so we basically we create a call expression to a built-in call.
func (this *GoLanguageFrontend) handleGoStmt(fset *token.FileSet, goStmt *ast.GoStmt) (expr *cpg.Expression) {
	this.LogTrace("Handling go statement: %+v", *goStmt)

	ref := (*cpg.Expression)(this.NewDeclaredReferenceExpression(fset, nil, "go"))

	call := this.NewCallExpression(fset, goStmt, ref, "go")
	call.AddArgument(this.handleCallExpr(fset, goStmt.Call))

	return (*cpg.Expression)(call)
}

func (this *GoLanguageFrontend) handleIfStmt(fset *token.FileSet, ifStmt *ast.IfStmt) (expr *cpg.Expression) {
	this.LogTrace("Handling if statement: %+v", *ifStmt)

	stmt := this.NewIfStatement(fset, ifStmt)

	var scope = this.GetScopeManager()

	scope.EnterScope((*cpg.Node)(stmt))

	if ifStmt.Init != nil {
		init := this.handleStmt(fset, ifStmt.Init, ifStmt)
		stmt.SetInitializerStatement(init)
	}

	cond := this.handleExpr(fset, ifStmt.Cond)
	stmt.SetCondition(cond)

	then := this.handleBlockStmt(fset, ifStmt.Body)
	// Somehow this can be nil-ish?
	if !then.IsNil() {
		stmt.SetThenStatement((*cpg.Statement)(then))
	}

	if ifStmt.Else != nil {
		els := this.handleStmt(fset, ifStmt.Else, ifStmt)
		stmt.SetElseStatement((*cpg.Statement)(els))
	}

	scope.LeaveScope((*cpg.Node)(stmt))

	return (*cpg.Expression)(stmt)
}

func (this *GoLanguageFrontend) handleSwitchStmt(fset *token.FileSet, switchStmt *ast.SwitchStmt) (expr *cpg.Expression) {
	this.LogTrace("Handling switch statement: %+v", *switchStmt)

	s := this.NewSwitchStatement(fset, switchStmt)

	if switchStmt.Init != nil {
		s.SetInitializerStatement(this.handleStmt(fset, switchStmt.Init, switchStmt))
	}

	if switchStmt.Tag != nil {
		s.SetCondition(this.handleExpr(fset, switchStmt.Tag))
	}

	s.SetStatement((*cpg.Statement)(this.handleBlockStmt(fset, switchStmt.Body))) // should only contain case clauses

	return (*cpg.Expression)(s)
}

func (this *GoLanguageFrontend) handleCaseClause(fset *token.FileSet, caseClause *ast.CaseClause) (expr *cpg.Expression) {
	this.LogTrace("Handling case clause: %+v", *caseClause)

	var s *cpg.Statement

	if caseClause.List == nil {
		s = (*cpg.Statement)(this.NewDefaultStatement(fset, nil))
	} else {
		c := this.NewCaseStatement(fset, caseClause)
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
		s = this.handleStmt(fset, stmt, caseClause)

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

	// In Go, regular cast expressions (not type asserts are modelled as calls).
	// In this case, the Fun contains a type expression.
	switch v := callExpr.Fun.(type) {
	case *ast.ArrayType,
		*ast.StructType,
		*ast.FuncType,
		*ast.InterfaceType,
		*ast.MapType,
		*ast.ChanType:
		this.LogDebug("Handling cast expression: %#v", callExpr)

		cast := this.NewCastExpression(fset, callExpr)
		cast.SetCastType(this.handleType(fset, v))

		if len(callExpr.Args) > 1 {
			cast.SetExpression(this.handleExpr(fset, callExpr.Args[0]))
		}

		return (*cpg.Expression)(cast)
	}

	// parse the Fun field, to see which kind of expression it is
	var reference = this.handleExpr(fset, callExpr.Fun)

	if reference == nil {
		return nil
	}

	name := reference.GetName().GetLocalName()

	if name == "new" {
		return this.handleNewExpr(fset, callExpr)
	} else if name == "make" {
		return this.handleMakeExpr(fset, callExpr)
	}

	isMemberExpression, err := (*jnigi.ObjectRef)(reference).IsInstanceOf(env, cpg.MemberExpressionClass)
	if err != nil {
		log.Fatal(err)

	}

	if isMemberExpression {
		this.LogTrace("Fun is a member call to %s", name)

		m := this.NewMemberCallExpression(fset, callExpr, reference)

		c = (*cpg.CallExpression)(m)
	} else {
		this.LogTrace("Handling regular call expression to %s", name)

		c = this.NewCallExpression(fset, callExpr, reference, name)
	}

	for _, arg := range callExpr.Args {
		e := this.handleExpr(fset, arg)

		if e != nil {
			c.AddArgument(e)
		}
	}

	return (*cpg.Expression)(c)
}

func (this *GoLanguageFrontend) handleIndexExpr(fset *token.FileSet, indexExpr *ast.IndexExpr) *cpg.ArraySubscriptionExpression {
	a := this.NewArraySubscriptionExpression(fset, indexExpr)

	a.SetArrayExpression(this.handleExpr(fset, indexExpr.X))
	a.SetSubscriptExpression(this.handleExpr(fset, indexExpr.Index))

	return a
}

// handleSliceExpr handles a [ast.SliceExpr], which is an extended version of
// [ast.IndexExpr]. We are modelling this as a combination of a
// [cpg.ArraySubscriptionExpression] that contains a [cpg.RangeExpression] as
// its subscriptExpression to share some code between this and an index
// expression.
func (this *GoLanguageFrontend) handleSliceExpr(fset *token.FileSet, sliceExpr *ast.SliceExpr) *cpg.ArraySubscriptionExpression {
	a := this.NewArraySubscriptionExpression(fset, sliceExpr)

	a.SetArrayExpression(this.handleExpr(fset, sliceExpr.X))

	// Build the slice expression
	s := this.NewRangeExpression(fset, sliceExpr)
	if sliceExpr.Low != nil {
		s.SetFloor(this.handleExpr(fset, sliceExpr.Low))
	}
	if sliceExpr.High != nil {
		s.SetCeiling(this.handleExpr(fset, sliceExpr.High))
	}
	if sliceExpr.Max != nil {
		s.SetThird(this.handleExpr(fset, sliceExpr.Max))
	}

	a.SetSubscriptExpression((*cpg.Expression)(s))

	return a
}

func (this *GoLanguageFrontend) handleNewExpr(fset *token.FileSet, callExpr *ast.CallExpr) *cpg.Expression {
	n := this.NewNewExpression(fset, callExpr)

	// first argument is type
	t := this.handleType(fset, callExpr.Args[0])

	// new is a pointer, so need to reference the type with a pointer
	var pointer = jnigi.NewObjectRef(cpg.PointerOriginClass)
	err := env.GetStaticField(cpg.PointerOriginClass, "POINTER", pointer)
	if err != nil {
		log.Fatal(err)
	}

	(*cpg.HasType)(n).SetType(t.Reference(pointer))

	// a new expression also needs an initializer, which is usually a constructexpression
	c := this.NewConstructExpression(fset, callExpr)
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
	t := this.handleType(fset, callExpr.Args[0])

	// actually make() can make more than just arrays, i.e. channels and maps
	if _, isArray := callExpr.Args[0].(*ast.ArrayType); isArray {
		r := this.NewArrayCreationExpression(fset, callExpr)

		// second argument is a dimension (if this is an array), usually a literal
		if len(callExpr.Args) > 1 {
			d := this.handleExpr(fset, callExpr.Args[1])

			r.AddDimension(d)
		}

		n = (*cpg.Expression)(r)
	} else {
		// create at least a generic construct expression for the given map or channel type
		// and provide the remaining arguments

		c := this.NewConstructExpression(fset, callExpr)

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
	b := this.NewBinaryOperator(fset, binaryExpr, binaryExpr.Op.String())

	lhs := this.handleExpr(fset, binaryExpr.X)
	rhs := this.handleExpr(fset, binaryExpr.Y)

	b.SetLHS(lhs)
	b.SetRHS(rhs)

	return b
}

func (this *GoLanguageFrontend) handleUnaryExpr(fset *token.FileSet, unaryExpr *ast.UnaryExpr) *cpg.UnaryOperator {
	u := this.NewUnaryOperator(fset, unaryExpr, unaryExpr.Op.String(), false, false)

	input := this.handleExpr(fset, unaryExpr.X)
	if input != nil {
		u.SetInput(input)
	}

	return u
}

func (this *GoLanguageFrontend) handleStarExpr(fset *token.FileSet, unaryExpr *ast.StarExpr) *cpg.UnaryOperator {
	u := this.NewUnaryOperator(fset, unaryExpr, "*", false, true)

	input := this.handleExpr(fset, unaryExpr.X)
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
		if base.GetName().GetLocalName() == getImportName(imp) {
			// found a package name, so this is NOT a member expression
			isMemberExpression = false
		}
	}

	var decl *cpg.DeclaredReferenceExpression
	if isMemberExpression {
		m := this.NewMemberExpression(fset, selectorExpr, selectorExpr.Sel.Name, base)

		decl = (*cpg.DeclaredReferenceExpression)(m)
	} else {
		// we need to set the name to a FQN-style, including the package scope. the call resolver will then resolve this
		fqn := fmt.Sprintf("%s.%s", base.GetName(), selectorExpr.Sel.Name)

		this.LogTrace("Trying to parse the fqn '%s'", fqn)

		name := this.ParseName(fqn)

		decl = this.NewDeclaredReferenceExpression(fset, selectorExpr, fqn)
		decl.Node().SetName(name)
	}

	// For now we just let the VariableUsageResolver handle this. Therefore,
	// we can not differentiate between field access to a receiver, an object
	// or a const field within a package at this point.

	// check, if the base relates to a receiver
	/*var method = (*cpg.MethodDeclaration)((*jnigi.ObjectRef)(this.GetScopeManager().GetCurrentFunction()).Cast(MethodDeclarationClass))

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
	this.LogTrace("Handling key value expression %+v", *expr)

	k := this.NewKeyValueExpression(fset, expr)

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
	this.LogTrace("Handling literal %+v", *lit)

	var value cpg.Castable
	var t *cpg.Type

	lang, err := this.GetLanguage()
	if err != nil {
		panic(err)
	}

	switch lit.Kind {
	case token.STRING:
		// strip the "
		value = cpg.NewString(lit.Value[1 : len(lit.Value)-1])
		t = cpg.TypeParser_createFrom("string", lang)
	case token.INT:
		i, _ := strconv.ParseInt(lit.Value, 10, 64)
		value = cpg.NewInteger(int(i))
		t = cpg.TypeParser_createFrom("int", lang)
	case token.FLOAT:
		// default seems to be float64
		f, _ := strconv.ParseFloat(lit.Value, 64)
		value = cpg.NewDouble(f)
		t = cpg.TypeParser_createFrom("float64", lang)
	case token.IMAG:
		// TODO
		t = &cpg.UnknownType_getUnknown(lang).Type
	case token.CHAR:
		value = cpg.NewString(lit.Value)
		t = cpg.TypeParser_createFrom("rune", lang)
		break
	}

	l := this.NewLiteral(fset, lit, value, t)

	return l
}

// handleCompositeLit handles a composite literal, which we need to translate into a combination of a
// ConstructExpression and a list of KeyValueExpressions. The problem is that we need to add the list
// as a first argument of the construct expression.
func (this *GoLanguageFrontend) handleCompositeLit(fset *token.FileSet, lit *ast.CompositeLit) *cpg.ConstructExpression {
	this.LogTrace("Handling composite literal %+v", *lit)

	c := this.NewConstructExpression(fset, lit)

	// parse the type field, to see which kind of expression it is
	var typ = this.handleType(fset, lit.Type)

	if typ != nil {
		(*cpg.Node)(c).SetName(typ.GetName())
		(*cpg.Expression)(c).SetType(typ)
	}

	l := this.NewInitializerListExpression(fset, lit)

	c.AddArgument((*cpg.Expression)(l))

	// Normally, the construct expression would not have DFG edge, but in this case we are mis-using it
	// to simulate an object literal, so we need to add a DFG here, otherwise a declaration is disconnected
	// from its initialization.
	c.AddPrevDFG((*cpg.Node)(l))

	var exprs = []*cpg.Expression{}
	for _, elem := range lit.Elts {
		expr := this.handleExpr(fset, elem)

		if expr != nil {
			exprs = append(exprs, expr)
		}
	}

	l.SetInitializers(exprs)

	return c
}

// handleFuncLit handles a function literal, which we need to translate into a combination of a
// LambdaExpression and a function declaration.
func (this *GoLanguageFrontend) handleFuncLit(fset *token.FileSet, lit *ast.FuncLit) *cpg.LambdaExpression {
	this.LogTrace("Handling function literal %#v", *lit)

	l := this.NewLambdaExpression(fset, lit)

	// Parse the expression as a function declaration with a little trick
	funcDecl := this.handleFuncDecl(fset, &ast.FuncDecl{Type: lit.Type, Body: lit.Body, Name: ast.NewIdent("")})

	this.LogTrace("Function of literal is: %#v", funcDecl)

	l.SetFunction(funcDecl)

	return l
}

func (this *GoLanguageFrontend) handleIdent(fset *token.FileSet, ident *ast.Ident) *cpg.Expression {
	lang, err := this.GetLanguage()
	if err != nil {
		panic(err)
	}

	// Check, if this is 'nil', because then we handle it as a literal in the graph
	if ident.Name == "nil" {
		lit := this.NewLiteral(fset, ident, nil, &cpg.UnknownType_getUnknown(lang).Type)

		(*cpg.Node)(lit).SetName(this.ParseName(ident.Name))

		return (*cpg.Expression)(lit)
	}

	ref := this.NewDeclaredReferenceExpression(fset, ident, ident.Name)

	tu := this.CurrentTU

	// check, if this refers to a package import
	i := tu.GetIncludeByName(ident.Name)

	// then set the refersTo, because our regular CPG passes will not resolve them
	if i != nil && !(*jnigi.ObjectRef)(i).IsNil() {
		ref.SetRefersTo((*cpg.Declaration)(i))
	}

	return (*cpg.Expression)(ref)
}

func (this *GoLanguageFrontend) handleTypeAssertExpr(fset *token.FileSet, assert *ast.TypeAssertExpr) *cpg.CastExpression {
	cast := this.NewCastExpression(fset, assert)

	// Parse the inner expression
	expr := this.handleExpr(fset, assert.X)

	// Parse the type
	typ := this.handleType(fset, assert.Type)

	cast.SetExpression(expr)

	if typ != nil {
		cast.SetCastType(typ)
	}

	return cast
}

func (this *GoLanguageFrontend) handleType(fset *token.FileSet, typeExpr ast.Expr) *cpg.Type {
	var err error

	this.LogTrace("Parsing type %T: %s", typeExpr, code(fset, typeExpr))

	lang, err := this.GetLanguage()
	if err != nil {
		panic(err)
	}

	switch v := typeExpr.(type) {
	case *ast.Ident:
		var name string
		if this.isBuiltinType(v.Name) {
			name = v.Name
			this.LogTrace("non-fqn type: %s", name)
		} else {
			name = fmt.Sprintf("%s.%s", this.File.Name.Name, v.Name)
			this.LogTrace("fqn type: %s", name)
		}

		return cpg.TypeParser_createFrom(name, lang)
	case *ast.SelectorExpr:
		// small shortcut
		fqn := fmt.Sprintf("%s.%s", v.X.(*ast.Ident).Name, v.Sel.Name)
		this.LogTrace("FQN type: %s", fqn)
		return cpg.TypeParser_createFrom(fqn, lang)
	case *ast.StarExpr:
		t := this.handleType(fset, v.X)

		var i = jnigi.NewObjectRef(cpg.PointerOriginClass)
		err = env.GetStaticField(cpg.PointerOriginClass, "POINTER", i)
		if err != nil {
			log.Fatal(err)
		}

		this.LogTrace("Pointer to %s", t.GetName())

		return t.Reference(i)
	case *ast.ArrayType:
		t := this.handleType(fset, v.Elt)

		var i = jnigi.NewObjectRef(cpg.PointerOriginClass)
		err = env.GetStaticField(cpg.PointerOriginClass, "ARRAY", i)
		if err != nil {
			log.Fatal(err)
		}

		this.LogTrace("Array of %s", t.GetName())

		return t.Reference(i)
	case *ast.MapType:
		// we cannot properly represent Golangs built-in map types, yet so we have
		// to make a shortcut here and represent it as a Java-like map<K, V> type.
		t := cpg.TypeParser_createFrom("map", lang)
		keyType := this.handleType(fset, v.Key)
		valueType := this.handleType(fset, v.Value)

		// TODO(oxisto): Find a better way to represent casts
		(*cpg.ObjectType)(t).AddGeneric(keyType)
		(*cpg.ObjectType)(t).AddGeneric(valueType)

		return t
	case *ast.ChanType:
		// handle them similar to maps
		t := cpg.TypeParser_createFrom("chan", lang)
		chanType := this.handleType(fset, v.Value)

		(*cpg.ObjectType)(t).AddGeneric(chanType)

		return t
	case *ast.FuncType:
		var parametersTypesList, returnTypesList, name *jnigi.ObjectRef
		var parameterTypes = []*cpg.Type{}
		var returnTypes = []*cpg.Type{}

		for _, param := range v.Params.List {
			parameterTypes = append(parameterTypes, this.handleType(fset, param.Type))
		}

		parametersTypesList, err = cpg.ListOf(parameterTypes)
		if err != nil {
			log.Fatal(err)
		}

		if v.Results != nil {
			for _, ret := range v.Results.List {
				returnTypes = append(returnTypes, this.handleType(fset, ret.Type))
			}
		}

		returnTypesList, err = cpg.ListOf(returnTypes)
		if err != nil {
			log.Fatal(err)
		}

		name, err = cpg.StringOf(funcTypeName(parameterTypes, returnTypes))
		if err != nil {
			log.Fatal(err)
		}

		var t, err = env.NewObject(cpg.FunctionTypeClass,
			name,
			parametersTypesList.Cast("java/util/List"),
			returnTypesList.Cast("java/util/List"),
			lang)
		if err != nil {
			log.Fatal(err)
		}

		return &cpg.Type{ObjectRef: t}
	case *ast.InterfaceType:
		var name = "interface{"
		// We do not really support dedicated interfaces types, so all we can for now
		// is parse it as an object type with a pseudo-name
		for _, method := range v.Methods.List {
			name += this.handleType(fset, method.Type).GetName().ToString()
		}

		name += "}"

		return cpg.TypeParser_createFrom(name, lang)
	case *ast.IndexExpr:
		// This is a type with one type parameter. First we need to parse the "X" expression as a type
		var t = this.handleType(fset, v.X)

		// Then we parse the "Index" as a type parameter
		var genericType = this.handleType(fset, v.Index)

		(*cpg.ObjectType)(t).AddGeneric(genericType)

		return t
	case *ast.IndexListExpr:
		// This is a type with two type parameters. First we need to parse the "X" expression as a type
		var t = this.handleType(fset, v.X)

		// Then we parse the "Indices" as a type parameter
		for _, index := range v.Indices {
			var genericType = this.handleType(fset, index)

			(*cpg.ObjectType)(t).AddGeneric(genericType)
		}

		return t
	default:
		this.LogError("Not parsing type of type %T yet. Defaulting to unknown type", v)
	}

	return &cpg.UnknownType_getUnknown(lang).Type
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

func (this *GoLanguageFrontend) ParseName(fqn string) *cpg.Name {
	var n *cpg.Name = (*cpg.Name)(jnigi.NewObjectRef(cpg.NameClass))
	err := env.CallStaticMethod(cpg.NameKtClass, "parseName", n, this.Cast(LanguageProviderClass), cpg.NewCharSequence(fqn))
	if err != nil {
		log.Fatal(err)
	}

	return n
}

// funcTypeName produces a Go-style function type name such as `func(int, string) string` or `func(int) (error, string)`
func funcTypeName(paramTypes []*cpg.Type, returnTypes []*cpg.Type) string {
	var rn []string
	var pn []string

	for _, t := range paramTypes {
		pn = append(pn, t.GetName().ToString())
	}

	for _, t := range returnTypes {
		rn = append(rn, t.GetName().ToString())
	}

	var rs string

	if len(returnTypes) > 1 {
		rs = fmt.Sprintf(" (%s)", strings.Join(rn, ", "))
	} else if len(returnTypes) > 0 {
		rs = fmt.Sprintf(" %s", strings.Join(rn, ", "))
	}

	return fmt.Sprintf("func(%s)%s", strings.Join(pn, ", "), rs)
}
