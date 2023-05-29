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

func (g *GoLanguageFrontend) ParseModule(topLevel string) (exists bool, err error) {
	g.LogInfo("Looking for a go.mod file in %s", topLevel)

	mod := path.Join(topLevel, "go.mod")

	if _, err := os.Stat(mod); err != nil {
		if os.IsNotExist(err) {
			g.LogInfo("%s does not exist", mod)

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

	g.Module = module

	g.LogInfo("Go application has module support with path %s", module.Module.Mod.Path)

	return true, nil
}

func (g *GoLanguageFrontend) HandleFile(fset *token.FileSet, file *ast.File, path string) (tu *cpg.TranslationUnitDeclaration, err error) {
	tu = g.NewTranslationUnitDeclaration(fset, file, path)

	scope := g.GetScopeManager()

	// reset scope
	scope.ResetToGlobal((*cpg.Node)(tu))

	g.CurrentTU = tu

	for _, imprt := range file.Imports {
		i := g.handleImportSpec(fset, imprt)

		err = scope.AddDeclaration((*cpg.Declaration)(i))
		if err != nil {
			log.Fatal(err)
		}
	}

	// Create a new namespace declaration, representing the package
	p := g.NewNamespaceDeclaration(fset, nil, file.Name.Name)

	// we need to construct the package "path" (e.g. "encoding/json") out of the
	// module path as well as the current directory in relation to the topLevel
	packagePath := filepath.Dir(path)

	// Construct a relative path starting from the top level
	packagePath, err = filepath.Rel(g.TopLevel, packagePath)
	if err == nil {
		// If we are in a module, we need to prepend the module path to it
		if g.Module != nil {
			packagePath = filepath.Join(g.Module.Module.Mod.Path, packagePath)
		}

		p.SetPath(packagePath)
	} else {
		g.LogError("Could not relativize package path to top level. Cannot set package path: %v", err)
	}

	// enter scope
	scope.EnterScope((*cpg.Node)(p))

	for _, decl := range file.Decls {
		// Retrieve all top level declarations. One "Decl" could potentially
		// contain multiple CPG declarations.
		decls := g.handleDecl(fset, decl)

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
func (g *GoLanguageFrontend) handleComments(node *cpg.Node, astNode ast.Node) {
	g.LogTrace("Handling comments for %+v", astNode)

	var comment = ""

	// Lookup ast node in comment map. One cannot use Filter() because this would actually filter all the comments
	// that are "below" this AST node as well, e.g. in its children. We only want the comments on the node itself.
	// Therefore we must convert the CommentMap back into an actual map to access the stored entry for the node.
	comments, ok := (map[ast.Node][]*ast.CommentGroup)(g.CommentMap)[astNode]
	if !ok {
		return
	}

	for _, c := range comments {
		text := strings.TrimRight(c.Text(), "\n")
		comment += text
	}

	if comment != "" {
		node.SetComment(comment)

		g.LogTrace("Comments: %+v", comment)
	}
}

// handleDecl parses an [ast.Decl]. Note, that in a "Decl", one or more actual
// declarations can be found. Therefore, this function returns a slice of
// [cpg.Declaration].
func (g *GoLanguageFrontend) handleDecl(fset *token.FileSet, decl ast.Decl) (decls []*cpg.Declaration) {
	g.LogTrace("Handling declaration (%T): %+v", decl, decl)

	decls = []*cpg.Declaration{}

	switch v := decl.(type) {
	case *ast.FuncDecl:
		// There can be only a single function declaration
		decls = append(decls, (*cpg.Declaration)(g.handleFuncDecl(fset, v)))
	case *ast.GenDecl:
		// GenDecl can hold multiple declarations
		decls = g.handleGenDecl(fset, v)
	default:
		g.LogError("Not parsing declaration of type %T yet: %+v", v, v)
		// TODO: Return a ProblemDeclaration
	}

	// Handle comments for all declarations
	for _, d := range decls {
		// TODO: This is problematic because we are assigning it the wrong node
		if d != nil {
			g.handleComments((*cpg.Node)(d), decl)
		}
	}

	return
}

func (g *GoLanguageFrontend) handleFuncDecl(fset *token.FileSet, funcDecl *ast.FuncDecl) *cpg.FunctionDeclaration {
	g.LogTrace("Handling func Decl: %+v", *funcDecl)

	var scope = g.GetScopeManager()
	var receiver *cpg.VariableDeclaration

	var f *cpg.FunctionDeclaration
	if funcDecl.Recv != nil {
		m := g.NewMethodDeclaration(fset, funcDecl, funcDecl.Name.Name)

		// TODO: why is this a list?
		var recv = funcDecl.Recv.List[0]

		var recordType = g.handleType(fset, recv.Type)

		// The name of the Go receiver is optional. In fact, if the name is not
		// specified we probably do not need any receiver variable at all,
		// because the syntax is only there to ensure that this method is part
		// of the struct, but it is not modifying the receiver.
		if len(recv.Names) > 0 {
			receiver = g.NewVariableDeclaration(fset, nil, recv.Names[0].Name)

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
			//  this is a limitation that we have for C++ as well
			record, err := g.GetScopeManager().GetRecordForName(
				g.GetScopeManager().GetCurrentScope(),
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
				g.LogTrace("Record: %+v", record)

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

		f = g.NewFunctionDeclaration(fset, funcDecl, funcDecl.Name.Name, "", localNameOnly)
	}

	// enter scope for function
	scope.EnterScope((*cpg.Node)(f))

	if receiver != nil {
		g.LogTrace("Adding receiver %s", (*cpg.Node)(receiver).GetName())

		// add the receiver do the scope manager, so we can resolve the receiver value
		g.GetScopeManager().AddDeclaration((*cpg.Declaration)(receiver))
	}

	var t = g.handleType(fset, funcDecl.Type)
	var returnTypes = []*cpg.Type{}

	if funcDecl.Type.Results != nil {
		for _, returnVariable := range funcDecl.Type.Results.List {
			returnTypes = append(returnTypes, g.handleType(fset, returnVariable.Type))

			// if the function has named return variables, be sure to declare them as well
			if returnVariable.Names != nil {
				p := g.NewVariableDeclaration(fset, returnVariable, returnVariable.Names[0].Name)

				p.SetType(g.handleType(fset, returnVariable.Type))

				// add parameter to scope
				g.GetScopeManager().AddDeclaration((*cpg.Declaration)(p))
			}
		}
	}

	g.LogTrace("Function has type %s", t.GetName())

	f.SetType(t)
	f.SetReturnTypes(returnTypes)

	// TODO: for other languages, we would enter the record declaration, if
	// this is a method; however I am not quite sure if this makes sense for
	// go, since we do not have a 'this', but rather a named receiver

	for _, param := range funcDecl.Type.Params.List {
		g.LogTrace("Parsing param: %+v", param)

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
			g.LogError("Some param has no name, which is a bit weird: %+v", param)
		}

		p := g.NewParamVariableDeclaration(fset, param, name)

		// Check for varargs. In this case we want to parse the element type
		// (and make it an array afterwards)
		if ell, ok := param.Type.(*ast.Ellipsis); ok {
			p.SetVariadic(true)
			var t = g.handleType(fset, ell.Elt)

			var i = jnigi.NewObjectRef(cpg.PointerOriginClass)
			err := env.GetStaticField(cpg.PointerOriginClass, "ARRAY", i)
			if err != nil {
				log.Fatal(err)
			}

			p.SetType(t.Reference(i))
		} else {
			p.SetType(g.handleType(fset, param.Type))
		}

		// add parameter to scope
		g.GetScopeManager().AddDeclaration((*cpg.Declaration)(p))

		g.handleComments((*cpg.Node)(p), param)
	}

	g.LogTrace("Parsing function body of %s", (*cpg.Node)(f).GetName())

	// parse body
	s := g.handleBlockStmt(fset, funcDecl.Body)

	// Check, if the last statement is a return statement, otherwise we insert an implicit one
	last := s.LastOrNull()
	ok, err := (*jnigi.ObjectRef)(last).IsInstanceOf(env, cpg.RecordDeclarationClass)
	if err != nil {
		log.Fatal(err)
	}

	if !ok {
		r := g.NewReturnStatement(fset, nil)
		s.AddStatement((*cpg.Statement)(r))
	}

	err = f.SetBody((*cpg.Statement)(s))
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

func (g *GoLanguageFrontend) handleGenDecl(fset *token.FileSet, genDecl *ast.GenDecl) (decls []*cpg.Declaration) {
	decls = []*cpg.Declaration{}

	for _, spec := range genDecl.Specs {
		switch v := spec.(type) {
		case *ast.ValueSpec:
			decls = append(decls, g.handleValueSpec(fset, v)...)
		case *ast.TypeSpec:
			decls = append(decls, g.handleTypeSpec(fset, v))
		case *ast.ImportSpec:
			// Somehow these end up duplicate in the AST, so do not handle them here
		default:
			g.LogError("Not parsing specification of type %T yet: %+v", v, v)
		}
	}

	return
}

// handleValueSpec handles parsing of an [ast.ValueSpec], which is a variable
// declaration. Since this can potentially declare multiple variables with one
// "spec", this returns a slice of [cpg.Declaration].
func (g *GoLanguageFrontend) handleValueSpec(fset *token.FileSet, valueDecl *ast.ValueSpec) (decls []*cpg.Declaration) {
	decls = []*cpg.Declaration{}

	// We need to declare one variable for each name
	for idx, ident := range valueDecl.Names {
		d := g.NewVariableDeclaration(fset, valueDecl, ident.Name)

		// Handle the type (if its there)
		if valueDecl.Type != nil {
			t := g.handleType(fset, valueDecl.Type)

			d.SetType(t)
		}

		lenValues := len(valueDecl.Values)
		if lenValues != 0 && lenValues != len(valueDecl.Names) {
			var names []string
			for _, n := range valueDecl.Names {
				names = append(names, n.String())
			}

			g.LogError("Number of initializers (%d) does not match number of names (%s). Initializers might be incomplete", lenValues, names)
		}

		// The initializer is in the "Values" slice with the respective index
		if len(valueDecl.Values) > idx {
			var expr = g.handleExpr(fset, valueDecl.Values[idx])

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
func (g *GoLanguageFrontend) handleTypeSpec(fset *token.FileSet, typeDecl *ast.TypeSpec) *cpg.Declaration {
	err := g.LogInfo("Type specifier with name %s and type (%T, %+v)", typeDecl.Name.Name, typeDecl.Type, typeDecl.Type)
	if err != nil {
		log.Fatal(err)
	}

	switch v := typeDecl.Type.(type) {
	case *ast.StructType:
		return (*cpg.Declaration)(g.handleStructTypeSpec(fset, typeDecl, v))
	case *ast.InterfaceType:
		return (*cpg.Declaration)(g.handleInterfaceTypeSpec(fset, typeDecl, v))
	}

	return nil
}

func (g *GoLanguageFrontend) handleImportSpec(fset *token.FileSet, importSpec *ast.ImportSpec) *cpg.Declaration {
	g.LogTrace("Import specifier with: %+v)", *importSpec)

	i := g.NewIncludeDeclaration(fset, importSpec, getImportName(importSpec))

	var scope = g.GetScopeManager()

	i.SetFilename(importSpec.Path.Value[1 : len(importSpec.Path.Value)-1])

	err := scope.AddDeclaration((*cpg.Declaration)(i))
	if err != nil {
		log.Fatal(err)
	}

	return (*cpg.Declaration)(i)
}

func (g *GoLanguageFrontend) handleIdentAsName(ident *ast.Ident) string {
	return ident.Name
}

func (g *GoLanguageFrontend) handleStructTypeSpec(fset *token.FileSet, typeDecl *ast.TypeSpec, structType *ast.StructType) *cpg.RecordDeclaration {
	r := g.NewRecordDeclaration(fset, typeDecl, g.handleIdentAsName(typeDecl.Name), "struct")

	var scope = g.GetScopeManager()

	scope.EnterScope((*cpg.Node)(r))

	if !structType.Incomplete {
		for _, field := range structType.Fields.List {

			// a field can also have no name, which means that it is embedded, not quite
			// sure yet how to handle this, but since the embedded field can be accessed
			// by its type, it could make sense to name the field according to the type

			var name string
			t := g.handleType(fset, field.Type)

			if field.Names == nil {
				// retrieve the root type name
				var typeName = t.GetRoot().GetName().ToString()

				g.LogTrace("Handling embedded field of type %s", typeName)

				name = typeName
			} else {
				g.LogTrace("Handling field %s", field.Names[0].Name)

				// TODO: Multiple names?
				name = field.Names[0].Name
			}

			f := g.NewFieldDeclaration(fset, field, name)

			f.SetType(t)

			scope.AddDeclaration((*cpg.Declaration)(f))
		}
	}

	scope.LeaveScope((*cpg.Node)(r))

	return r
}

func (g *GoLanguageFrontend) handleInterfaceTypeSpec(fset *token.FileSet, typeDecl *ast.TypeSpec, interfaceType *ast.InterfaceType) *cpg.RecordDeclaration {
	r := g.NewRecordDeclaration(fset, typeDecl, g.handleIdentAsName(typeDecl.Name), "interface")

	var scope = g.GetScopeManager()

	scope.EnterScope((*cpg.Node)(r))

	if !interfaceType.Incomplete {
		for _, method := range interfaceType.Methods.List {
			t := g.handleType(fset, method.Type)

			// Even though this list is called "Methods", it contains all kinds
			// of things, so we need to proceed with caution. Only if the
			// "method" actually has a name, we declare a new method
			// declaration.
			if len(method.Names) > 0 {
				m := g.NewMethodDeclaration(fset, method, method.Names[0].Name)
				m.SetType(t)

				scope.AddDeclaration((*cpg.Declaration)(m))
			} else {
				g.LogTrace("Adding %s as super class of interface %s", t.GetName(), (*cpg.Node)(r).GetName())
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

func (g *GoLanguageFrontend) handleBlockStmt(fset *token.FileSet, blockStmt *ast.BlockStmt) *cpg.CompoundStatement {
	g.LogTrace("Handling block statement: %+v", *blockStmt)

	c := g.NewCompoundStatement(fset, blockStmt)

	// enter scope
	g.GetScopeManager().EnterScope((*cpg.Node)(c))

	for _, stmt := range blockStmt.List {
		var s *cpg.Statement

		s = g.handleStmt(fset, stmt, blockStmt)

		if s != nil {
			// add statement
			c.AddStatement(s)
		}
	}

	// leave scope
	g.GetScopeManager().LeaveScope((*cpg.Node)(c))

	return c
}

func (g *GoLanguageFrontend) handleForStmt(fset *token.FileSet, forStmt *ast.ForStmt) *cpg.ForStatement {
	g.LogTrace("Handling for statement: %+v", *forStmt)

	f := g.NewForStatement(fset, forStmt)

	var scope = g.GetScopeManager()

	scope.EnterScope((*cpg.Node)(f))

	if forStmt.Init != nil {
		initStatement := g.handleStmt(fset, forStmt.Init, forStmt)
		f.SetInitializerStatement(initStatement)
	}

	if forStmt.Cond != nil {
		condition := g.handleExpr(fset, forStmt.Cond)
		f.SetCondition(condition)
	}

	if forStmt.Post != nil {
		iter := g.handleStmt(fset, forStmt.Post, forStmt)
		f.SetIterationStatement(iter)
	}

	if body := g.handleStmt(fset, forStmt.Body, forStmt); body != nil {
		f.SetStatement(body)
	}

	scope.LeaveScope((*cpg.Node)(f))

	return f
}

func (g *GoLanguageFrontend) handleRangeStmt(fset *token.FileSet, rangeStmt *ast.RangeStmt) *cpg.ForEachStatement {
	g.LogTrace("Handling range statement: %+v", *rangeStmt)

	f := g.NewForEachStatement(fset, rangeStmt)

	var scope = g.GetScopeManager()

	scope.EnterScope((*cpg.Node)(f))

	// TODO: Support other use cases that do not use DEFINE
	if rangeStmt.Tok == token.DEFINE {
		stmt := g.NewDeclarationStatement(fset, rangeStmt)

		// TODO: not really the best way to deal with this
		// TODO: key type is always int. we could set this
		var keyName = rangeStmt.Key.(*ast.Ident).Name

		key := g.NewVariableDeclaration(fset, rangeStmt.Key, keyName)
		g.GetScopeManager().AddDeclaration((*cpg.Declaration)(key))
		stmt.AddToPropertyEdgeDeclaration((*cpg.Declaration)(key))

		if rangeStmt.Value != nil {
			// TODO: not really the best way to deal with this
			// TODO: key type is always int. we could set this
			var valueName = rangeStmt.Value.(*ast.Ident).Name

			value := g.NewVariableDeclaration(fset, rangeStmt.Key, valueName)
			g.GetScopeManager().AddDeclaration((*cpg.Declaration)(value))
			stmt.AddToPropertyEdgeDeclaration((*cpg.Declaration)(value))
		}

		f.SetVariable((*cpg.Statement)(stmt))
	}

	iterable := (*cpg.Statement)(g.handleExpr(fset, rangeStmt.X))
	f.SetIterable(iterable)

	body := g.handleStmt(fset, rangeStmt.Body, rangeStmt)
	f.SetStatement(body)

	scope.LeaveScope((*cpg.Node)(f))

	return f
}

func (g *GoLanguageFrontend) handleReturnStmt(fset *token.FileSet, returnStmt *ast.ReturnStmt) *cpg.ReturnStatement {
	g.LogTrace("Handling return statement: %+v", *returnStmt)

	r := g.NewReturnStatement(fset, returnStmt)

	if returnStmt.Results != nil && len(returnStmt.Results) > 0 {
		e := g.handleExpr(fset, returnStmt.Results[0])

		// TODO: parse more than one result expression

		if e != nil {
			r.SetReturnValue(e)
		}
	} else {
		// TODO: connect result statement to result variables
	}

	return r
}

func (g *GoLanguageFrontend) handleIncDecStmt(fset *token.FileSet, incDecStmt *ast.IncDecStmt) *cpg.UnaryOperator {
	g.LogTrace("Handling decimal increment statement: %+v", *incDecStmt)

	var opCode string
	if incDecStmt.Tok == token.INC {
		opCode = "++"
	}

	if incDecStmt.Tok == token.DEC {
		opCode = "--"
	}

	u := g.NewUnaryOperator(fset, incDecStmt, opCode, true, false)

	if input := g.handleExpr(fset, incDecStmt.X); input != nil {
		u.SetInput(input)
	}

	return u
}

func (g *GoLanguageFrontend) handleStmt(fset *token.FileSet, stmt ast.Stmt, parent ast.Stmt) (s *cpg.Statement) {
	g.LogTrace("Handling statement (%T): %+v", stmt, stmt)

	switch v := stmt.(type) {
	case *ast.ExprStmt:
		// in our cpg, each expression is also a statement,
		// so we do not need an expression statement wrapper
		s = (*cpg.Statement)(g.handleExpr(fset, v.X))
	case *ast.AssignStmt:
		s = (*cpg.Statement)(g.handleAssignStmt(fset, v, parent))
	case *ast.BranchStmt:
		s = (*cpg.Statement)(g.handleBranchStmt(fset, v, parent))
	case *ast.LabeledStmt:
		s = (*cpg.Statement)(g.handleLabeledStmt(fset, v, parent))
	case *ast.DeclStmt:
		s = (*cpg.Statement)(g.handleDeclStmt(fset, v))
	case *ast.GoStmt:
		s = (*cpg.Statement)(g.handleGoStmt(fset, v))
	case *ast.DeferStmt:
		s = (*cpg.Statement)(g.handleDeferStmt(fset, v))
	case *ast.IfStmt:
		s = (*cpg.Statement)(g.handleIfStmt(fset, v))
	case *ast.SwitchStmt:
		s = (*cpg.Statement)(g.handleSwitchStmt(fset, v))
	case *ast.CaseClause:
		s = (*cpg.Statement)(g.handleCaseClause(fset, v))
	case *ast.BlockStmt:
		s = (*cpg.Statement)(g.handleBlockStmt(fset, v))
	case *ast.ForStmt:
		s = (*cpg.Statement)(g.handleForStmt(fset, v))
	case *ast.RangeStmt:
		s = (*cpg.Statement)(g.handleRangeStmt(fset, v))
	case *ast.ReturnStmt:
		s = (*cpg.Statement)(g.handleReturnStmt(fset, v))
	case *ast.IncDecStmt:
		s = (*cpg.Statement)(g.handleIncDecStmt(fset, v))
	default:
		msg := fmt.Sprintf("Not parsing statement of type %T yet: %s", v, code(fset, v))
		g.LogError(msg)
		s = (*cpg.Statement)(g.NewProblemExpression(fset, v, msg))
	}

	if s != nil {
		g.handleComments((*cpg.Node)(s), stmt)
	}

	return
}

func (g *GoLanguageFrontend) handleExpr(fset *token.FileSet, expr ast.Expr) (e *cpg.Expression) {
	g.LogTrace("Handling expression (%T): %+v", expr, expr)

	switch v := expr.(type) {
	case *ast.CallExpr:
		e = (*cpg.Expression)(g.handleCallExpr(fset, v))
	case *ast.IndexExpr:
		e = (*cpg.Expression)(g.handleIndexExpr(fset, v))
	case *ast.BinaryExpr:
		e = (*cpg.Expression)(g.handleBinaryExpr(fset, v))
	case *ast.UnaryExpr:
		e = (*cpg.Expression)(g.handleUnaryExpr(fset, v))
	case *ast.StarExpr:
		e = (*cpg.Expression)(g.handleStarExpr(fset, v))
	case *ast.SelectorExpr:
		e = (*cpg.Expression)(g.handleSelectorExpr(fset, v))
	case *ast.SliceExpr:
		e = (*cpg.Expression)(g.handleSliceExpr(fset, v))
	case *ast.KeyValueExpr:
		e = (*cpg.Expression)(g.handleKeyValueExpr(fset, v))
	case *ast.BasicLit:
		e = (*cpg.Expression)(g.handleBasicLit(fset, v))
	case *ast.CompositeLit:
		e = (*cpg.Expression)(g.handleCompositeLit(fset, v))
	case *ast.FuncLit:
		e = (*cpg.Expression)(g.handleFuncLit(fset, v))
	case *ast.Ident:
		e = (*cpg.Expression)(g.handleIdent(fset, v))
	case *ast.TypeAssertExpr:
		e = (*cpg.Expression)(g.handleTypeAssertExpr(fset, v))
	case *ast.ParenExpr:
		e = g.handleExpr(fset, v.X)
	default:
		msg := fmt.Sprintf("Not parsing expression of type %T yet: %s", v, code(fset, v))
		g.LogError(msg)
		e = (*cpg.Expression)(g.NewProblemExpression(fset, v, msg))
	}

	if e != nil {
		g.handleComments((*cpg.Node)(e), expr)
	}

	return
}

func (g *GoLanguageFrontend) handleAssignStmt(fset *token.FileSet, assignStmt *ast.AssignStmt, parent ast.Stmt) (expr *cpg.Expression) {
	g.LogTrace("Handling assignment statement: %+v", assignStmt)

	var rhs []*cpg.Expression
	var lhs []*cpg.Expression
	for _, expr := range assignStmt.Lhs {
		lhs = append(lhs, g.handleExpr(fset, expr))
	}

	for _, expr := range assignStmt.Rhs {
		rhs = append(rhs, g.handleExpr(fset, expr))
	}

	a := g.NewAssignExpression(fset, assignStmt, "=")

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

func (g *GoLanguageFrontend) handleBranchStmt(fset *token.FileSet, branchStmt *ast.BranchStmt, parent ast.Stmt) (expr *cpg.Statement) {
	g.LogTrace("Handling branch statement: %+v", branchStmt)

	switch branchStmt.Tok.String() {
	case "break":
		stmt := g.NewBreakStatement(fset, branchStmt)
		if branchStmt.Label != nil {
			stmt.SetLabel(branchStmt.Label.Name)
		}

		return (*cpg.Statement)(stmt)
	case "continue":
		stmt := g.NewContinueStatement(fset, branchStmt)
		if branchStmt.Label != nil {
			stmt.SetLabel(branchStmt.Label.Name)
		}

		return (*cpg.Statement)(stmt)
	case "goto":
		stmt := g.NewGotoStatement(fset, branchStmt)
		stmt.SetLabelName(branchStmt.Label.Name)

		return (*cpg.Statement)(stmt)
	default:
		g.LogError("Unknown token in branch statement: %s", branchStmt.Tok)
	}

	return
}

func (g *GoLanguageFrontend) handleLabeledStmt(fset *token.FileSet, labeledStmt *ast.LabeledStmt, parent ast.Stmt) (expr *cpg.Statement) {
	g.LogTrace("Handling labeled statement: %+v", labeledStmt)

	stmt := g.NewLabelStatement(fset, labeledStmt)
	stmt.SetSubStatement(g.handleStmt(fset, labeledStmt.Stmt, labeledStmt))
	stmt.SetLabel(labeledStmt.Label.Name)

	return (*cpg.Statement)(stmt)
}

func (g *GoLanguageFrontend) handleDeclStmt(fset *token.FileSet, declStmt *ast.DeclStmt) (expr *cpg.Expression) {
	g.LogTrace("Handling declaration statement: %+v", *declStmt)

	// Lets create a variable declaration (wrapped with a declaration stmt) with
	// this, because we define the variable here
	stmt := g.NewDeclarationStatement(fset, declStmt)

	decls := g.handleDecl(fset, declStmt.Decl)

	// Loop over the declarations and add them to the scope as well as the statement.
	for _, d := range decls {
		stmt.AddToPropertyEdgeDeclaration(d)
		g.GetScopeManager().AddDeclaration(d)
	}

	return (*cpg.Expression)(stmt)
}

// handleGoStmt handles the `go` statement, which is a special keyword in go
// that starts the supplied call expression in a separate Go routine. We cannot
// model this 1:1, so we basically we create a call expression to a built-in call.
func (g *GoLanguageFrontend) handleGoStmt(fset *token.FileSet, goStmt *ast.GoStmt) (expr *cpg.Expression) {
	g.LogTrace("Handling go statement: %+v", *goStmt)

	call := g.NewUnaryOperator(fset, goStmt, "go", false, true)
	call.SetInput(g.handleCallExpr(fset, goStmt.Call))

	return (*cpg.Expression)(call)
}

// handleDeferStmt handles the `defer` statement, which is a special keyword in go
// that the supplied callee is executed once the function it is called in exists.
// We cannot model this 1:1, so we basically we create a call expression to a built-in call.
// We adjust the EOG of the call later in an extra pass.
func (g *GoLanguageFrontend) handleDeferStmt(fset *token.FileSet, deferStmt *ast.DeferStmt) (expr *cpg.Expression) {
	g.LogTrace("Handling defer statement: %+v", *deferStmt)

	call := g.NewUnaryOperator(fset, deferStmt, "defer", false, true)
	call.SetInput(g.handleCallExpr(fset, deferStmt.Call))

	return (*cpg.Expression)(call)
}

func (g *GoLanguageFrontend) handleIfStmt(fset *token.FileSet, ifStmt *ast.IfStmt) (expr *cpg.Expression) {
	g.LogTrace("Handling if statement: %+v", *ifStmt)

	stmt := g.NewIfStatement(fset, ifStmt)

	var scope = g.GetScopeManager()

	scope.EnterScope((*cpg.Node)(stmt))

	if ifStmt.Init != nil {
		init := g.handleStmt(fset, ifStmt.Init, ifStmt)
		stmt.SetInitializerStatement(init)
	}

	cond := g.handleExpr(fset, ifStmt.Cond)
	stmt.SetCondition(cond)

	then := g.handleBlockStmt(fset, ifStmt.Body)
	// Somehow this can be nil-ish?
	if !then.IsNil() {
		stmt.SetThenStatement((*cpg.Statement)(then))
	}

	if ifStmt.Else != nil {
		els := g.handleStmt(fset, ifStmt.Else, ifStmt)
		stmt.SetElseStatement((*cpg.Statement)(els))
	}

	scope.LeaveScope((*cpg.Node)(stmt))

	return (*cpg.Expression)(stmt)
}

func (g *GoLanguageFrontend) handleSwitchStmt(fset *token.FileSet, switchStmt *ast.SwitchStmt) (expr *cpg.Expression) {
	g.LogTrace("Handling switch statement: %+v", *switchStmt)

	s := g.NewSwitchStatement(fset, switchStmt)

	g.GetScopeManager().EnterScope((*cpg.Node)(s))

	if switchStmt.Init != nil {
		s.SetInitializerStatement(g.handleStmt(fset, switchStmt.Init, switchStmt))
	}

	if switchStmt.Tag != nil {
		s.SetCondition(g.handleExpr(fset, switchStmt.Tag))
	}

	s.SetStatement((*cpg.Statement)(g.handleBlockStmt(fset, switchStmt.Body))) // should only contain case clauses

	g.GetScopeManager().LeaveScope((*cpg.Node)(s))

	return (*cpg.Expression)(s)
}

func (g *GoLanguageFrontend) handleCaseClause(fset *token.FileSet, caseClause *ast.CaseClause) (expr *cpg.Expression) {
	g.LogTrace("Handling case clause: %+v", *caseClause)

	var s *cpg.Statement

	if caseClause.List == nil {
		s = (*cpg.Statement)(g.NewDefaultStatement(fset, nil))
	} else {
		c := g.NewCaseStatement(fset, caseClause)
		c.SetCaseExpression(g.handleExpr(fset, caseClause.List[0]))

		s = (*cpg.Statement)(c)
	}

	// need to find the current block / scope and add the statements to it
	block := g.GetScopeManager().GetCurrentBlock()

	// add the case statement
	if s != nil && block != nil && !block.IsNil() {
		block.AddStatement((*cpg.Statement)(s))
	}

	for _, stmt := range caseClause.Body {
		s = g.handleStmt(fset, stmt, caseClause)

		if s != nil && block != nil && !block.IsNil() {
			// add statement
			block.AddStatement(s)
		}
	}

	// this is a little trick, to not add the case statement in handleStmt because we added it already.
	// otherwise, the order is screwed up.
	return nil
}

func (g *GoLanguageFrontend) handleCallExpr(fset *token.FileSet, callExpr *ast.CallExpr) *cpg.Expression {
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
		g.LogDebug("Handling cast expression: %#v", callExpr)

		cast := g.NewCastExpression(fset, callExpr)
		cast.SetCastType(g.handleType(fset, v))

		if len(callExpr.Args) > 1 {
			cast.SetExpression(g.handleExpr(fset, callExpr.Args[0]))
		}

		return (*cpg.Expression)(cast)
	}

	// parse the Fun field, to see which kind of expression it is
	var reference = g.handleExpr(fset, callExpr.Fun)

	if reference == nil {
		return nil
	}

	name := reference.GetName().GetLocalName()

	if name == "new" {
		return g.handleNewExpr(fset, callExpr)
	} else if name == "make" {
		return g.handleMakeExpr(fset, callExpr)
	}

	isMemberExpression, err := (*jnigi.ObjectRef)(reference).IsInstanceOf(env, cpg.MemberExpressionClass)
	if err != nil {
		log.Fatal(err)

	}

	if isMemberExpression {
		g.LogTrace("Fun is a member call to %s", name)

		m := g.NewMemberCallExpression(fset, callExpr, reference)

		c = (*cpg.CallExpression)(m)
	} else {
		g.LogTrace("Handling regular call expression to %s", name)

		c = g.NewCallExpression(fset, callExpr, reference, name)
	}

	for _, arg := range callExpr.Args {
		e := g.handleExpr(fset, arg)

		if e != nil {
			c.AddArgument(e)
		}
	}

	return (*cpg.Expression)(c)
}

func (g *GoLanguageFrontend) handleIndexExpr(fset *token.FileSet, indexExpr *ast.IndexExpr) *cpg.ArraySubscriptionExpression {
	a := g.NewArraySubscriptionExpression(fset, indexExpr)

	a.SetArrayExpression(g.handleExpr(fset, indexExpr.X))
	a.SetSubscriptExpression(g.handleExpr(fset, indexExpr.Index))

	return a
}

// handleSliceExpr handles a [ast.SliceExpr], which is an extended version of
// [ast.IndexExpr]. We are modelling this as a combination of a
// [cpg.ArraySubscriptionExpression] that contains a [cpg.RangeExpression] as
// its subscriptExpression to share some code between this and an index
// expression.
func (g *GoLanguageFrontend) handleSliceExpr(fset *token.FileSet, sliceExpr *ast.SliceExpr) *cpg.ArraySubscriptionExpression {
	a := g.NewArraySubscriptionExpression(fset, sliceExpr)

	a.SetArrayExpression(g.handleExpr(fset, sliceExpr.X))

	// Build the slice expression
	s := g.NewRangeExpression(fset, sliceExpr)
	if sliceExpr.Low != nil {
		s.SetFloor(g.handleExpr(fset, sliceExpr.Low))
	}
	if sliceExpr.High != nil {
		s.SetCeiling(g.handleExpr(fset, sliceExpr.High))
	}
	if sliceExpr.Max != nil {
		s.SetThird(g.handleExpr(fset, sliceExpr.Max))
	}

	a.SetSubscriptExpression((*cpg.Expression)(s))

	return a
}

func (g *GoLanguageFrontend) handleNewExpr(fset *token.FileSet, callExpr *ast.CallExpr) *cpg.Expression {
	n := g.NewNewExpression(fset, callExpr)

	// first argument is type
	t := g.handleType(fset, callExpr.Args[0])

	// new is a pointer, so need to reference the type with a pointer
	var pointer = jnigi.NewObjectRef(cpg.PointerOriginClass)
	err := env.GetStaticField(cpg.PointerOriginClass, "POINTER", pointer)
	if err != nil {
		log.Fatal(err)
	}

	(*cpg.HasType)(n).SetType(t.Reference(pointer))

	// a new expression also needs an initializer, which is usually a constructexpression
	c := g.NewConstructExpression(fset, callExpr)
	(*cpg.HasType)(c).SetType(t)

	n.SetInitializer((*cpg.Expression)(c))

	return (*cpg.Expression)(n)
}

func (g *GoLanguageFrontend) handleMakeExpr(fset *token.FileSet, callExpr *ast.CallExpr) *cpg.Expression {
	var n *cpg.Expression

	if callExpr.Args == nil || len(callExpr.Args) < 1 {
		return nil
	}

	// first argument is always the type, handle it
	t := g.handleType(fset, callExpr.Args[0])

	// actually make() can make more than just arrays, i.e. channels and maps
	if _, isArray := callExpr.Args[0].(*ast.ArrayType); isArray {
		r := g.NewArrayCreationExpression(fset, callExpr)

		// second argument is a dimension (if this is an array), usually a literal
		if len(callExpr.Args) > 1 {
			d := g.handleExpr(fset, callExpr.Args[1])

			r.AddDimension(d)
		}

		n = (*cpg.Expression)(r)
	} else {
		// create at least a generic construct expression for the given map or channel type
		// and provide the remaining arguments

		c := g.NewConstructExpression(fset, callExpr)

		// pass the remaining arguments
		for _, arg := range callExpr.Args[1:] {
			a := g.handleExpr(fset, arg)

			c.AddArgument(a)
		}

		n = (*cpg.Expression)(c)
	}

	// set the type, we have parsed earlier
	(*cpg.HasType)(n).SetType(t)

	return n
}

func (g *GoLanguageFrontend) handleBinaryExpr(fset *token.FileSet, binaryExpr *ast.BinaryExpr) *cpg.BinaryOperator {
	b := g.NewBinaryOperator(fset, binaryExpr, binaryExpr.Op.String())

	lhs := g.handleExpr(fset, binaryExpr.X)
	rhs := g.handleExpr(fset, binaryExpr.Y)

	b.SetLHS(lhs)
	b.SetRHS(rhs)

	return b
}

func (g *GoLanguageFrontend) handleUnaryExpr(fset *token.FileSet, unaryExpr *ast.UnaryExpr) *cpg.UnaryOperator {
	u := g.NewUnaryOperator(fset, unaryExpr, unaryExpr.Op.String(), false, false)

	input := g.handleExpr(fset, unaryExpr.X)
	if input != nil {
		u.SetInput(input)
	}

	return u
}

func (g *GoLanguageFrontend) handleStarExpr(fset *token.FileSet, unaryExpr *ast.StarExpr) *cpg.UnaryOperator {
	u := g.NewUnaryOperator(fset, unaryExpr, "*", false, true)

	input := g.handleExpr(fset, unaryExpr.X)
	if input != nil {
		u.SetInput(input)
	}

	return u
}

func (g *GoLanguageFrontend) handleSelectorExpr(fset *token.FileSet, selectorExpr *ast.SelectorExpr) *cpg.DeclaredReferenceExpression {
	base := g.handleExpr(fset, selectorExpr.X)

	// check, if this just a regular reference to a variable with a package scope and not a member expression
	var isMemberExpression bool = true
	for _, imp := range g.File.Imports {
		if base.GetName().GetLocalName() == getImportName(imp) {
			// found a package name, so this is NOT a member expression
			isMemberExpression = false
		}
	}

	var decl *cpg.DeclaredReferenceExpression
	if isMemberExpression {
		m := g.NewMemberExpression(fset, selectorExpr, selectorExpr.Sel.Name, base)

		decl = (*cpg.DeclaredReferenceExpression)(m)
	} else {
		// we need to set the name to a FQN-style, including the package scope. the call resolver will then resolve this
		fqn := fmt.Sprintf("%s.%s", base.GetName(), selectorExpr.Sel.Name)

		g.LogTrace("Trying to parse the fqn '%s'", fqn)

		name := g.ParseName(fqn)

		decl = g.NewDeclaredReferenceExpression(fset, selectorExpr, fqn)
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

func (g *GoLanguageFrontend) handleKeyValueExpr(fset *token.FileSet, expr *ast.KeyValueExpr) *cpg.KeyValueExpression {
	g.LogTrace("Handling key value expression %+v", *expr)

	k := g.NewKeyValueExpression(fset, expr)

	keyExpr := g.handleExpr(fset, expr.Key)
	if keyExpr != nil {
		k.SetKey(keyExpr)
	}

	valueExpr := g.handleExpr(fset, expr.Value)
	if valueExpr != nil {
		k.SetValue(valueExpr)
	}

	return k
}

func (g *GoLanguageFrontend) handleBasicLit(fset *token.FileSet, lit *ast.BasicLit) *cpg.Literal {
	g.LogTrace("Handling literal %+v", *lit)

	var value cpg.Castable
	var t *cpg.Type

	lang, err := g.GetLanguage()
	if err != nil {
		panic(err)
	}

	switch lit.Kind {
	case token.STRING:
		// strip the "
		value = cpg.NewString(lit.Value[1 : len(lit.Value)-1])
		t = cpg.TypeParser_createFrom("string", lang, g.GetCtx())
	case token.INT:
		i, _ := strconv.ParseInt(lit.Value, 10, 64)
		value = cpg.NewInteger(int(i))
		t = cpg.TypeParser_createFrom("int", lang, g.GetCtx())
	case token.FLOAT:
		// default seems to be float64
		f, _ := strconv.ParseFloat(lit.Value, 64)
		value = cpg.NewDouble(f)
		t = cpg.TypeParser_createFrom("float64", lang, g.GetCtx())
	case token.IMAG:
		// TODO
		t = &cpg.UnknownType_getUnknown(lang).Type
	case token.CHAR:
		value = cpg.NewString(lit.Value)
		t = cpg.TypeParser_createFrom("rune", lang, g.GetCtx())
		break
	}

	l := g.NewLiteral(fset, lit, value, t)

	return l
}

// handleCompositeLit handles a composite literal, which we need to translate into a combination of a
// ConstructExpression and a list of KeyValueExpressions. The problem is that we need to add the list
// as a first argument of the construct expression.
func (g *GoLanguageFrontend) handleCompositeLit(fset *token.FileSet, lit *ast.CompositeLit) *cpg.ConstructExpression {
	g.LogTrace("Handling composite literal %+v", *lit)

	c := g.NewConstructExpression(fset, lit)

	// parse the type field, to see which kind of expression it is
	var typ = g.handleType(fset, lit.Type)

	if typ != nil {
		(*cpg.Node)(c).SetName(typ.GetName())
		(*cpg.Expression)(c).SetType(typ)
	}

	l := g.NewInitializerListExpression(fset, lit)

	c.AddArgument((*cpg.Expression)(l))

	// Normally, the construct expression would not have DFG edge, but in this case we are mis-using it
	// to simulate an object literal, so we need to add a DFG here, otherwise a declaration is disconnected
	// from its initialization.
	c.AddPrevDFG((*cpg.Node)(l))

	var exprs = []*cpg.Expression{}
	for _, elem := range lit.Elts {
		expr := g.handleExpr(fset, elem)

		if expr != nil {
			exprs = append(exprs, expr)
		}
	}

	l.SetInitializers(exprs)

	return c
}

// handleFuncLit handles a function literal, which we need to translate into a combination of a
// LambdaExpression and a function declaration.
func (g *GoLanguageFrontend) handleFuncLit(fset *token.FileSet, lit *ast.FuncLit) *cpg.LambdaExpression {
	g.LogTrace("Handling function literal %#v", *lit)

	l := g.NewLambdaExpression(fset, lit)

	// Parse the expression as a function declaration with a little trick
	funcDecl := g.handleFuncDecl(fset, &ast.FuncDecl{Type: lit.Type, Body: lit.Body, Name: ast.NewIdent("")})

	g.LogTrace("Function of literal is: %#v", funcDecl)

	l.SetFunction(funcDecl)

	return l
}

func (g *GoLanguageFrontend) handleIdent(fset *token.FileSet, ident *ast.Ident) *cpg.Expression {
	lang, err := g.GetLanguage()
	if err != nil {
		panic(err)
	}

	// Check, if this is 'nil', because then we handle it as a literal in the graph
	if ident.Name == "nil" {
		lit := g.NewLiteral(fset, ident, nil, &cpg.UnknownType_getUnknown(lang).Type)

		(*cpg.Node)(lit).SetName(g.ParseName(ident.Name))

		return (*cpg.Expression)(lit)
	}

	ref := g.NewDeclaredReferenceExpression(fset, ident, ident.Name)

	tu := g.CurrentTU

	// check, if this refers to a package import
	i := tu.GetIncludeByName(ident.Name)

	// then set the refersTo, because our regular CPG passes will not resolve them
	if i != nil && !(*jnigi.ObjectRef)(i).IsNil() {
		ref.SetRefersTo((*cpg.Declaration)(i))
	}

	return (*cpg.Expression)(ref)
}

func (g *GoLanguageFrontend) handleTypeAssertExpr(fset *token.FileSet, assert *ast.TypeAssertExpr) *cpg.CastExpression {
	cast := g.NewCastExpression(fset, assert)

	// Parse the inner expression
	expr := g.handleExpr(fset, assert.X)

	// Parse the type
	typ := g.handleType(fset, assert.Type)

	cast.SetExpression(expr)

	if typ != nil {
		cast.SetCastType(typ)
	}

	return cast
}

func (g *GoLanguageFrontend) handleType(fset *token.FileSet, typeExpr ast.Expr) *cpg.Type {
	var err error

	g.LogTrace("Parsing type %T: %s", typeExpr, code(fset, typeExpr))

	lang, err := g.GetLanguage()
	if err != nil {
		panic(err)
	}

	switch v := typeExpr.(type) {
	case *ast.Ident:
		var name string
		if g.isBuiltinType(v.Name) {
			name = v.Name
			g.LogTrace("non-fqn type: %s", name)
		} else {
			name = fmt.Sprintf("%s.%s", g.File.Name.Name, v.Name)
			g.LogTrace("fqn type: %s", name)
		}

		return cpg.TypeParser_createFrom(name, lang, g.GetCtx())
	case *ast.SelectorExpr:
		// small shortcut
		fqn := fmt.Sprintf("%s.%s", v.X.(*ast.Ident).Name, v.Sel.Name)
		g.LogTrace("FQN type: %s", fqn)
		return cpg.TypeParser_createFrom(fqn, lang, g.GetCtx())
	case *ast.StarExpr:
		t := g.handleType(fset, v.X)

		var i = jnigi.NewObjectRef(cpg.PointerOriginClass)
		err = env.GetStaticField(cpg.PointerOriginClass, "POINTER", i)
		if err != nil {
			log.Fatal(err)
		}

		g.LogTrace("Pointer to %s", t.GetName())

		return t.Reference(i)
	case *ast.ArrayType:
		t := g.handleType(fset, v.Elt)

		var i = jnigi.NewObjectRef(cpg.PointerOriginClass)
		err = env.GetStaticField(cpg.PointerOriginClass, "ARRAY", i)
		if err != nil {
			log.Fatal(err)
		}

		g.LogTrace("Array of %s", t.GetName())

		return t.Reference(i)
	case *ast.MapType:
		// we cannot properly represent Golangs built-in map types, yet so we have
		// to make a shortcut here and represent it as a Java-like map<K, V> type.
		t := cpg.TypeParser_createFrom("map", lang, g.GetCtx())
		keyType := g.handleType(fset, v.Key)
		valueType := g.handleType(fset, v.Value)

		// TODO(oxisto): Find a better way to represent casts
		(*cpg.ObjectType)(t).AddGeneric(keyType)
		(*cpg.ObjectType)(t).AddGeneric(valueType)

		return t
	case *ast.ChanType:
		// handle them similar to maps
		t := cpg.TypeParser_createFrom("chan", lang, g.GetCtx())
		chanType := g.handleType(fset, v.Value)

		(*cpg.ObjectType)(t).AddGeneric(chanType)

		return t
	case *ast.FuncType:
		var parametersTypesList, returnTypesList, name *jnigi.ObjectRef
		var parameterTypes = []*cpg.Type{}
		var returnTypes = []*cpg.Type{}

		for _, param := range v.Params.List {
			parameterTypes = append(parameterTypes, g.handleType(fset, param.Type))
		}

		parametersTypesList, err = cpg.ListOf(parameterTypes)
		if err != nil {
			log.Fatal(err)
		}

		if v.Results != nil {
			for _, ret := range v.Results.List {
				returnTypes = append(returnTypes, g.handleType(fset, ret.Type))
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
			name += g.handleType(fset, method.Type).GetName().ToString()
		}

		name += "}"

		return cpg.TypeParser_createFrom(name, lang, g.GetCtx())
	case *ast.IndexExpr:
		// This is a type with one type parameter. First we need to parse the "X" expression as a type
		var t = g.handleType(fset, v.X)

		// Then we parse the "Index" as a type parameter
		var genericType = g.handleType(fset, v.Index)

		(*cpg.ObjectType)(t).AddGeneric(genericType)

		return t
	case *ast.IndexListExpr:
		// This is a type with two type parameters. First we need to parse the "X" expression as a type
		var t = g.handleType(fset, v.X)

		// Then we parse the "Indices" as a type parameter
		for _, index := range v.Indices {
			var genericType = g.handleType(fset, index)

			(*cpg.ObjectType)(t).AddGeneric(genericType)
		}

		return t
	default:
		g.LogError("Not parsing type of type %T yet. Defaulting to unknown type", v)
	}

	return &cpg.UnknownType_getUnknown(lang).Type
}

func (g *GoLanguageFrontend) isBuiltinType(s string) bool {
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

func (g *GoLanguageFrontend) ParseName(fqn string) *cpg.Name {
	var n *cpg.Name = (*cpg.Name)(jnigi.NewObjectRef(cpg.NameClass))
	err := env.CallStaticMethod(cpg.NameKtClass, "parseName", n, g.Cast(LanguageProviderClass), cpg.NewCharSequence(fqn))
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
