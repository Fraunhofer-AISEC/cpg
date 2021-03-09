package frontend

import (
	"cpg"
	"fmt"
	"go/ast"
	"go/token"
	"log"
	"strconv"

	"tekao.net/jnigi"
)

func (this *GoLanguageFrontend) HandleFile(fset *token.FileSet, env *jnigi.Env, file *ast.File) (tu *cpg.TranslationUnitDeclaration, err error) {
	tu = cpg.NewTranslationUnitDeclaration(fset, env, file, "src.go", this.GetCodeFromRawNode(fset, file))

	scope := this.GetScopeManager(env)

	// reset scope
	scope.ResetToGlobal(env, (*cpg.Node)(tu))

	// create a new namespace declaration, representing the package
	p := cpg.NewNamespaceDeclaration(fset, env, nil, file.Name.Name, fmt.Sprintf("package %s", file.Name.Name))

	// enter scope
	scope.EnterScope(env, (*cpg.Node)(p))

	for _, decl := range file.Decls {
		var d *cpg.Declaration

		d = this.handleDecl(fset, env, decl)

		if d != nil {
			err = scope.AddDeclaration(env, (*cpg.Declaration)(d))
			if err != nil {
				panic(err)
			}
		}
	}

	// leave scope
	scope.LeaveScope(env, (*cpg.Node)(p))

	// add it
	scope.AddDeclaration(env, (*cpg.Declaration)(p))

	return
}

func (this *GoLanguageFrontend) handleDecl(fset *token.FileSet, env *jnigi.Env, decl ast.Decl) *cpg.Declaration {
	this.LogDebug(env, "Handling declaration (%T): %+v", decl, decl)

	switch v := decl.(type) {
	case *ast.FuncDecl:
		return (*cpg.Declaration)(this.handleFuncDecl(fset, env, v))
	case *ast.GenDecl:
		return (*cpg.Declaration)(this.handleGenDecl(fset, env, v))
	default:
		this.LogError(env, "Not parsing declaration of type %T yet: %+v", v, v)
		// no match
		return nil
	}
}

func (this *GoLanguageFrontend) handleFuncDecl(fset *token.FileSet, env *jnigi.Env, funcDecl *ast.FuncDecl) *jnigi.ObjectRef {
	this.LogDebug(env, "Handling func Decl: %+v", *funcDecl)

	var scope = this.GetScopeManager(env)
	var receiver *cpg.VariableDeclaration

	var f *cpg.FunctionDeclaration
	if funcDecl.Recv != nil {
		m := cpg.NewMethodDeclaration(fset, env, funcDecl)

		// TODO: why is this a list?
		var recv = funcDecl.Recv.List[0]

		var recordType = this.handleType(env, recv.Type)

		receiver = cpg.NewVariableDeclaration(fset, env, nil)

		// TODO: should we use the FQN here? FQNs are a mess in the CPG...
		receiver.SetName(env, recv.Names[0].Name)
		receiver.SetType(env, recordType)

		this.LogDebug(env, "still here")

		err := m.SetReceiver(env, receiver)
		if err != nil {
			log.Fatal(err)
		}

		if recordType != nil {
			var recordName = (*cpg.Node)(recordType).GetName(env)

			// TODO: this will only find methods within the current translation unit
			// this is a limitation that we have for C++ as well
			record, err := this.GetScopeManager(env).GetRecordForName(env,
				this.GetScopeManager(env).GetCurrentScope(env),
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
				this.LogDebug(env, "Record: %+v", record)

				err = record.AddMethod(env, m)
				if err != nil {
					log.Fatal(err)
				}
			}
		}

		f = (*cpg.FunctionDeclaration)(m)
	} else {
		f = cpg.NewFunctionDeclaration(fset, env, funcDecl)
	}

	// note, the name must be set BEFORE entering the scope
	f.SetName(env, funcDecl.Name.Name)

	// enter scope for function
	scope.EnterScope(env, (*cpg.Node)(f))

	if receiver != nil {
		this.LogDebug(env, "Adding receiver %s", (*cpg.Node)(receiver).GetName(env))

		// add the receiver do the scope manager, so we can resolve the receiver value
		this.GetScopeManager(env).AddDeclaration(env, (*cpg.Declaration)(receiver))
	}

	// TODO: for other languages, we would enter the record declaration, if
	// this is a method; however I am not quite sure if this makes sense for
	// go, since we do not have a 'this', but rather a named receiver

	this.LogDebug(env, "Parsing function body")

	for _, param := range funcDecl.Type.Params.List {
		p := cpg.NewParamVariableDeclaration(fset, env, param)

		// TODO: more than one name?
		p.SetName(env, param.Names[0].Name)

		t := this.handleType(env, param.Type)

		p.SetType(env, t)

		// add parameter to scope
		this.GetScopeManager(env).AddDeclaration(env, (*cpg.Declaration)(p))
	}

	// parse body
	s := this.handleBlockStmt(fset, env, funcDecl.Body)

	err := f.SetBody(env, (*cpg.Statement)(s))
	if err != nil {
		log.Fatal(err)
	}

	// leave scope
	err = scope.LeaveScope(env, (*cpg.Node)(f))
	if err != nil {
		log.Fatal(err)
	}

	return (*jnigi.ObjectRef)(f)
}

func (this *GoLanguageFrontend) handleGenDecl(fset *token.FileSet, env *jnigi.Env, genDecl *ast.GenDecl) *jnigi.ObjectRef {
	// TODO: Handle multiple declarations
	for _, spec := range genDecl.Specs {
		switch v := spec.(type) {
		case *ast.ValueSpec:
			return (*jnigi.ObjectRef)(this.handleValueSpec(fset, env, v))
		case *ast.TypeSpec:
			return (*jnigi.ObjectRef)(this.handleTypeSpec(fset, env, v))
		default:
			this.LogError(env, "Not parsing specication of type %T yet: %+v", v, v)
		}
	}

	return nil
}

func (this *GoLanguageFrontend) handleValueSpec(fset *token.FileSet, env *jnigi.Env, valueDecl *ast.ValueSpec) *cpg.Declaration {
	// TODO: more names
	var ident = valueDecl.Names[0]

	d := (cpg.NewVariableDeclaration(fset, env, valueDecl))

	d.SetName(env, ident.Name)

	if valueDecl.Type != nil {
		t := this.handleType(env, valueDecl.Type)

		d.SetType(env, t)
	}

	// add an initializer
	if len(valueDecl.Values) > 0 {
		// TODO: How to deal with multiple values
		var expr = this.handleExpr(fset, env, valueDecl.Values[0])

		err := d.SetInitializer(env, expr)
		if err != nil {
			log.Fatal(err)
		}
	}

	return (*cpg.Declaration)(d)
}

func (this *GoLanguageFrontend) handleTypeSpec(fset *token.FileSet, env *jnigi.Env, typeDecl *ast.TypeSpec) *cpg.Declaration {
	err := this.LogInfo(env, "Type specifier with name %s and type (%T, %+v)", typeDecl.Name.Name, typeDecl.Type, typeDecl.Type)
	if err != nil {
		log.Fatal(err)
	}

	switch v := typeDecl.Type.(type) {
	case *ast.StructType:
		return (*cpg.Declaration)(this.handleStructTypeSpec(fset, env, typeDecl, v))
	case *ast.InterfaceType:
		return (*cpg.Declaration)(this.handleInterfaceTypeSpec(fset, env, typeDecl, v))
	}

	return nil
}

func (this *GoLanguageFrontend) handleStructTypeSpec(fset *token.FileSet, env *jnigi.Env, typeDecl *ast.TypeSpec, structType *ast.StructType) *cpg.RecordDeclaration {
	r := cpg.NewRecordDeclaration(fset, env, typeDecl)

	r.SetKind(env, "struct")
	r.SetName(env, typeDecl.Name.Name)

	var scope = this.GetScopeManager(env)

	scope.EnterScope(env, (*cpg.Node)(r))

	// TODO: parse members

	if !structType.Incomplete {
		for _, field := range structType.Fields.List {
			this.LogDebug(env, "Handling field %s", field.Names[0].Name)

			f := cpg.NewFieldDeclaration(fset, env, field)

			// TODO: Multiple names?
			f.SetName(env, field.Names[0].Name)

			t := this.handleType(env, field.Type)

			f.SetType(env, t)

			scope.AddDeclaration(env, (*cpg.Declaration)(f))
		}
	}

	scope.LeaveScope(env, (*cpg.Node)(r))

	return r
}

func (this *GoLanguageFrontend) handleInterfaceTypeSpec(fset *token.FileSet, env *jnigi.Env, typeDecl *ast.TypeSpec, interfaceType *ast.InterfaceType) *cpg.RecordDeclaration {
	r := cpg.NewRecordDeclaration(fset, env, typeDecl)

	r.SetKind(env, "interface")
	r.SetName(env, typeDecl.Name.Name)

	var scope = this.GetScopeManager(env)

	scope.EnterScope(env, (*cpg.Node)(r))

	if !interfaceType.Incomplete {
		for _, method := range interfaceType.Methods.List {
			m := cpg.NewMethodDeclaration(fset, env, method)

			t := this.handleType(env, method.Type)

			m.SetType(env, t)
			m.SetName(env, method.Names[0].Name)

			scope.AddDeclaration(env, (*cpg.Declaration)(m))
		}
	}

	scope.LeaveScope(env, (*cpg.Node)(r))

	return r
}

func (this *GoLanguageFrontend) handleBlockStmt(fset *token.FileSet, env *jnigi.Env, blockStmt *ast.BlockStmt) *cpg.CompoundStatement {
	c := cpg.NewCompoundStatement(fset, env, blockStmt)

	// enter scope
	this.GetScopeManager(env).EnterScope(env, (*cpg.Node)(c))

	for _, stmt := range blockStmt.List {
		var s *cpg.Statement

		s = this.handleStmt(fset, env, stmt)

		if s != nil {
			// add statement
			c.AddStatement(env, s)
		}
	}

	// leave scope
	this.GetScopeManager(env).LeaveScope(env, (*cpg.Node)(c))

	return c
}

func (this *GoLanguageFrontend) handleStmt(fset *token.FileSet, env *jnigi.Env, stmt ast.Stmt) *cpg.Statement {
	this.LogDebug(env, "Handling statement (%T): %+v", stmt, stmt)

	switch v := stmt.(type) {
	case *ast.ExprStmt:
		// in our cpg, each expression is also a statement,
		// so we do not need an expression statement wrapper
		return (*cpg.Statement)(this.handleExpr(fset, env, v.X))
	case *ast.AssignStmt:
		return (*cpg.Statement)(this.handleAssignStmt(fset, env, v))
	case *ast.DeclStmt:
		return (*cpg.Statement)(this.handleDeclStmt(fset, env, v))
	case *ast.IfStmt:
		return (*cpg.Statement)(this.handleIfStmt(fset, env, v))
	default:
		this.LogError(env, "Not parsing statement of type %T yet: %+v", v, v)
	}

	return nil
}

func (this *GoLanguageFrontend) handleExpr(fset *token.FileSet, env *jnigi.Env, expr ast.Expr) *cpg.Expression {
	this.LogDebug(env, "Handling expression (%T): %+v", expr, expr)

	switch v := expr.(type) {
	case *ast.CallExpr:
		return (*cpg.Expression)(this.handleCallExpr(fset, env, v))
	case *ast.BinaryExpr:
		return (*cpg.Expression)(this.handleBinaryExpr(fset, env, v))
	case *ast.SelectorExpr:
		return (*cpg.Expression)(this.handleSelectorExpr(fset, env, v))
	case *ast.BasicLit:
		return (*cpg.Expression)(this.handleBasicLit(fset, env, v))
	case *ast.Ident:
		return (*cpg.Expression)(this.handleIdent(fset, env, v))
	default:
		this.LogError(env, "Could not parse expression of type %T: %+v", v, v)
	}

	// TODO: return an error instead?
	return nil
}

func (this *GoLanguageFrontend) handleAssignStmt(fset *token.FileSet, env *jnigi.Env, assignStmt *ast.AssignStmt) (expr *cpg.Expression) {
	this.LogDebug(env, "Handling assignment statement: %+v", assignStmt)

	// TODO: more than one Rhs?!
	rhs := this.handleExpr(fset, env, assignStmt.Rhs[0])

	if assignStmt.Tok == token.DEFINE {
		// lets create a variable declaration (wrapped with a declaration stmt) with this, because we define the variable here
		stmt := cpg.NewDeclarationStatement(fset, env, assignStmt)

		// TODO: assignment of multiple values
		d := cpg.NewVariableDeclaration(fset, env, assignStmt)

		var name = assignStmt.Lhs[0].(*ast.Ident).Name
		d.SetName(env, name)

		if rhs != nil {
			d.SetInitializer(env, rhs)
		}

		stmt.SetSingleDeclaration(env, (*cpg.Declaration)(d))

		expr = (*cpg.Expression)(stmt)
	} else {
		lhs := this.handleExpr(fset, env, assignStmt.Lhs[0])

		b := cpg.NewBinaryOperator(fset, env, assignStmt)

		b.SetOperatorCode(env, "=")
		b.SetLHS(env, lhs)
		b.SetRHS(env, rhs)

		expr = (*cpg.Expression)(b)
	}

	return
}

func (this *GoLanguageFrontend) handleDeclStmt(fset *token.FileSet, env *jnigi.Env, declStmt *ast.DeclStmt) (expr *cpg.Expression) {
	this.LogDebug(env, "Handling declaration statement: %+v", *declStmt)

	// lets create a variable declaration (wrapped with a declaration stmt) with this,
	// because we define the variable here
	stmt := cpg.NewDeclarationStatement(fset, env, declStmt)

	d := this.handleDecl(fset, env, declStmt.Decl)

	stmt.SetSingleDeclaration(env, (*cpg.Declaration)(d))

	return (*cpg.Expression)(stmt)
}

func (this *GoLanguageFrontend) handleIfStmt(fset *token.FileSet, env *jnigi.Env, ifStmt *ast.IfStmt) (expr *cpg.Expression) {
	this.LogDebug(env, "Handling if statement: %+v", *ifStmt)

	stmt := cpg.NewIfStatement(fset, env, ifStmt)

	var scope = this.GetScopeManager(env)

	// TODO: initializer

	scope.EnterScope(env, (*cpg.Node)(stmt))

	cond := this.handleExpr(fset, env, ifStmt.Cond)
	stmt.SetCondition(env, cond)

	then := this.handleBlockStmt(fset, env, ifStmt.Body)
	stmt.SetThenStatement(env, (*cpg.Statement)(then))

	els := this.handleStmt(fset, env, ifStmt.Else)
	if els != nil {
		stmt.SetElseStatement(env, (*cpg.Statement)(els))
	}

	scope.LeaveScope(env, (*cpg.Node)(stmt))

	return (*cpg.Expression)(stmt)
}

func (this *GoLanguageFrontend) handleCallExpr(fset *token.FileSet, env *jnigi.Env, callExpr *ast.CallExpr) *cpg.CallExpression {
	var c *cpg.CallExpression

	// parse the Fun field, to see which kind of expression it is
	var reference = this.handleExpr(fset, env, callExpr.Fun)

	if reference == nil {
		return nil
	}

	name := (*cpg.Node)(reference).GetName(env)

	isMemberExpression, err := (*jnigi.ObjectRef)(reference).IsInstanceOf(env, "de/fraunhofer/aisec/cpg/graph/statements/expressions/MemberExpression")
	if err != nil {
		log.Fatal(err)
	}

	if isMemberExpression {
		baseName := (*cpg.Node)((*cpg.MemberExpression)(reference).GetBase(env)).GetName(env)
		// this is not 100% accurate since it should be rather the type not the base name
		// but FQNs are really broken in the CPG so this is ok for now
		fqn := fmt.Sprintf("%s.%s", baseName, name)

		this.LogDebug(env, "Fun is a member call to %s", name)

		m := cpg.NewMemberCallExpression(fset, env, callExpr)
		m.SetName(env, name)
		m.SetFqn(env, fqn)

		member := cpg.NewDeclaredReferenceExpression(fset, env, nil)
		member.SetName(env, name)

		m.SetBase(env, (*cpg.MemberExpression)(reference).GetBase(env))
		m.SetMember(env, member.Node())

		c = (*cpg.CallExpression)(m)
	} else {
		this.LogDebug(env, "Handling regular call expression to %s", name)

		c = cpg.NewCallExpression(fset, env, callExpr)
		c.SetName(env, name)
	}

	for _, arg := range callExpr.Args {
		e := this.handleExpr(fset, env, arg)

		if e != nil {
			c.AddArgument(env, e)
		}
	}

	// reference.disconnectFromGraph()

	return c
}

func (this *GoLanguageFrontend) handleBinaryExpr(fset *token.FileSet, env *jnigi.Env, binaryExpr *ast.BinaryExpr) *cpg.BinaryOperator {
	b := cpg.NewBinaryOperator(fset, env, binaryExpr)

	lhs := this.handleExpr(fset, env, binaryExpr.X)
	rhs := this.handleExpr(fset, env, binaryExpr.Y)

	err := b.SetOperatorCode(env, binaryExpr.Op.String())
	if err != nil {
		log.Fatal(err)
	}

	if lhs != nil {
		b.SetLHS(env, lhs)
	}

	if rhs != nil {
		b.SetRHS(env, rhs)
	}

	return b
}
func (this *GoLanguageFrontend) handleSelectorExpr(fset *token.FileSet, env *jnigi.Env, selectorExpr *ast.SelectorExpr) *cpg.MemberExpression {
	base := this.handleExpr(fset, env, selectorExpr.X)

	m := cpg.NewMemberExpression(fset, env, selectorExpr)

	m.SetBase(env, base)
	(*cpg.Node)(m).SetName(env, selectorExpr.Sel.Name)

	// check, if the base relates to a receiver
	var method = (*cpg.MethodDeclaration)((*jnigi.ObjectRef)(this.GetScopeManager(env).GetCurrentFunction(env)).Cast("de/fraunhofer/aisec/cpg/graph/declarations/MethodDeclaration"))

	if method != nil && !method.IsNil() {
		recv := method.GetReceiver(env)

		// this refers to our receiver
		if (*cpg.Node)(recv).GetName(env) == (*cpg.Node)(base).GetName(env) {
			// For now we just let the VariableUsageResolver handle this. Therefore,
			// we can not differentiate between field access to a receiver, an object
			// or a const field within a package at this point.
			//(*cpg.DeclaredReferenceExpression)(base).SetRefersTo(env, recv.Declaration())
		}
	}

	return m
}

func (this *GoLanguageFrontend) handleBasicLit(fset *token.FileSet, env *jnigi.Env, lit *ast.BasicLit) *cpg.Literal {
	this.LogDebug(env, "Handling literal %+v", *lit)

	l := cpg.NewLiteral(fset, env, lit)

	var value interface{}
	var t *cpg.Type

	switch lit.Kind {
	case token.STRING:
		// strip the "
		value = cpg.NewString(env, lit.Value[1:len(lit.Value)-1])
		t = cpg.TypeParser_createFrom(env, "string", false)
	case token.INT:
		i, _ := strconv.ParseInt(lit.Value, 10, 64)
		value = cpg.NewInteger(env, int(i))
		t = cpg.TypeParser_createFrom(env, "int", false)
	case token.FLOAT:
		// default seems to be float64
		f, _ := strconv.ParseFloat(lit.Value, 64)
		value = cpg.NewDouble(env, f)
		t = cpg.TypeParser_createFrom(env, "float64", false)
	case token.IMAG:
	case token.CHAR:
		value = cpg.NewString(env, lit.Value)
		break
	}

	l.SetType(env, t)
	l.SetValue(env, value)

	return l
}

func (this *GoLanguageFrontend) handleIdent(fset *token.FileSet, env *jnigi.Env, ident *ast.Ident) *cpg.DeclaredReferenceExpression {
	ref := cpg.NewDeclaredReferenceExpression(fset, env, ident)

	ref.SetName(env, ident.Name)

	return ref
}

func (this *GoLanguageFrontend) handleType(env *jnigi.Env, typeExpr ast.Expr) *cpg.Type {
	this.LogDebug(env, "Parsing type %T: %+v", typeExpr, typeExpr)

	switch v := typeExpr.(type) {
	case *ast.Ident:
		return cpg.TypeParser_createFrom(env, v.Name, false)
	case *ast.FuncType:
		// for now, we are only interested in the return type
		return this.handleType(env, v.Results.List[0].Type)
	}

	return nil
}
