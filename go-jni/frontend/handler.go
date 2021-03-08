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
	tu = cpg.NewTranslationUnitDeclaration(fset, env, file)

	scope := this.GetScopeManager(env)

	// reset scope
	scope.ResetToGlobal(env, (*cpg.Node)(tu))

	// create a new namespace declaration, representing the package
	p := cpg.NewNamespaceDeclaration(fset, env, nil)

	p.SetName(env, file.Name.Name)

	// enter scope
	scope.EnterScope(env, (*cpg.Node)(p))

	for _, decl := range file.Decls {
		fmt.Printf("%+v\n", decl)

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

	var f *cpg.FunctionDeclaration
	if funcDecl.Recv != nil {
		m := cpg.NewMethodDeclaration(fset, env, funcDecl)

		// TODO: why is this a list?
		var recv = funcDecl.Recv.List[0]

		var recordType = this.handleType(env, recv.Type)

		v := cpg.NewVariableDeclaration(fset, env, nil)

		// TODO: should we use the FQN here? FQNs are a mess in the CPG...
		v.SetName(env, recv.Names[0].Name)
		v.SetType(env, recordType)

		err := m.SetReceiver(env, v)
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

			if record != nil {
				// now this gets a little bit hacky, we will add it to the record declaration
				// this is strictly speaking not 100 % true, since the method property edge is
				// marked as AST and in Go a method is not part of the struct's AST but is declared
				// outside. In the future, we need to differentiate between just the associated members
				// of the class and the pure AST nodes declared in the struct itself
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

	f.SetName(env, funcDecl.Name.Name)

	// TODO: for other languages, we would enter the record declaration, if
	// this is a method; however I am not quite sure if this makes sense for
	// go, since we do not have a 'this', but rather a named receiver

	var scope = this.GetScopeManager(env)

	// enter scope for function body
	scope.EnterScope(env, (*cpg.Node)(f))

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
		log.Printf("%s", err)
	}

	// leave scope
	scope.LeaveScope(env, (*cpg.Node)(f))

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
		fmt.Printf("initializer: %v\n", valueDecl.Values[0])

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

		switch v := stmt.(type) {
		case *ast.ExprStmt:
			// in our cpg, each expression is also a statement,
			// so we do not need an expression statement wrapper
			s = (*cpg.Statement)(this.handleExpr(fset, env, v.X))
			fmt.Printf("exprStmt: %+v\n", v)
			fmt.Printf("statement: %+v\n", s)
		case *ast.AssignStmt:
			s = (*cpg.Statement)(this.handleAssignStmt(fset, env, v))
			fmt.Printf("assignment: %+v\n", v)
		default:
			fmt.Printf("%T: %+v\n", stmt, stmt)
		}

		if s != nil {
			// add statement
			c.AddStatement(env, s)
		}
	}

	// leave scope
	this.GetScopeManager(env).LeaveScope(env, (*cpg.Node)(c))

	return c
}

func (this *GoLanguageFrontend) handleExpr(fset *token.FileSet, env *jnigi.Env, expr ast.Expr) *cpg.Expression {
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
		log.Printf("Could not parse expression of type %T: %+v\n", v, v)
	}

	// TODO: return an error instead?
	return nil
}

func (this *GoLanguageFrontend) handleAssignStmt(fset *token.FileSet, env *jnigi.Env, assignStmt *ast.AssignStmt) (expr *cpg.Expression) {
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
	}

	return
}

func (this *GoLanguageFrontend) handleCallExpr(fset *token.FileSet, env *jnigi.Env, callExpr *ast.CallExpr) *cpg.CallExpression {
	var c *cpg.CallExpression

	switch v := callExpr.Fun.(type) {
	case *ast.SelectorExpr:
		name := v.Sel.Name

		// not sure if this always succeeds
		var method = (*cpg.MethodDeclaration)((*jnigi.ObjectRef)(this.GetScopeManager(env).GetCurrentFunction(env)).Cast("de/fraunhofer/aisec/cpg/graph/declarations/MethodDeclaration"))

		// this is a dot call, either qualified a package or a member call to a struct
		if method != nil {
			recv := method.GetReceiver(env)

			this.LogDebug(env, "Sel is %+v", v)
			this.LogDebug(env, "Sel.X is %T: %+v", v.X, v.X)

			// TODO: sel.X could be another expr and so on...
			if recv != nil && (*cpg.Node)(recv).GetName(env) == v.X.(*ast.Ident).Name {
				receiverName := (*cpg.Node)(recv).GetName(env)
				fqn := fmt.Sprintf("%s.%s", receiverName, name)

				this.LogDebug(env, "Fun is a member call to %s for receiver %s", name, receiverName)

				m := cpg.NewMemberCallExpression(fset, env, callExpr)
				m.SetName(env, name)
				m.SetFqn(env, fqn)

				// TODO: see above, handle base generically
				base := cpg.NewDeclaredReferenceExpression(fset, env, v.X)
				base.SetName(env, receiverName)
				base.SetRefersTo(env, recv.Declaration())

				member := cpg.NewDeclaredReferenceExpression(fset, env, v.Sel)
				member.SetName(env, name)
				// TODO: use Sel.Object, if available?

				m.SetBase(env, base.Node())
				m.SetMember(env, member.Node())

				c = (*cpg.CallExpression)(m)
			}
		}

		if c == nil {
			packageName := v.X.(*ast.Ident).Name
			fqn := fmt.Sprintf("%s.%s", packageName, name)

			// dot call using a qualified package name

			this.LogDebug(env, "Handling qualified call expression to %s", fqn)

			c = cpg.NewCallExpression(fset, env, callExpr)
			c.SetName(env, name)
			c.SetFqn(env, fqn)
		}
	case *ast.Ident:
		this.LogDebug(env, "Handling regular call expression to %s", v.Name)

		c = cpg.NewCallExpression(fset, env, callExpr)
		c.SetName(env, v.Name)
		// TODO: set FQN based on current package
		//c.SetFqn()

	default:
		this.LogError(env, "Not sure what we are calling here (%T): %+v", v, v)
		return nil
	}

	for _, arg := range callExpr.Args {
		e := this.handleExpr(fset, env, arg)

		if e != nil {
			c.AddArgument(env, e)
		}
	}

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
func (this *GoLanguageFrontend) handleSelectorExpr(fset *token.FileSet, env *jnigi.Env, selectorExpr *ast.SelectorExpr) *cpg.BinaryOperator {
	/*
			var record = lang.getScopeManager().getCurrentRecord();

		      base =
		          NodeBuilder.newDeclaredReferenceExpression(
		              "this",
		              record != null ? record.getThis().getType() : UnknownType.getUnknownType(),
		              base.getCode());
		      base.setLocation(location);
	*/

	/*
			  MemberExpression memberExpression =
		        NodeBuilder.newMemberExpression(
		            base,
		            UnknownType.getUnknownType(),
		            ctx.getFieldName().toString(),
		            ctx.isPointerDereference() ? "->" : ".",
		            ctx.getRawSignature());
	*/
	return nil
}

func (this *GoLanguageFrontend) handleBasicLit(fset *token.FileSet, env *jnigi.Env, lit *ast.BasicLit) *cpg.Literal {
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
	log.Printf("Parsing type %T: %+v\n", typeExpr, typeExpr)

	switch v := typeExpr.(type) {
	case *ast.Ident:
		return cpg.TypeParser_createFrom(env, v.Name, false)
	case *ast.FuncType:
		// for now, we are only interested in the return type
		return this.handleType(env, v.Results.List[0].Type)
	}

	return nil
}
