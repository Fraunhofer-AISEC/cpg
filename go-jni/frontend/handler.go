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

func (this *GoLanguageFrontend) HandleFile(fset *token.FileSet, file *ast.File) (tu *cpg.TranslationUnitDeclaration, err error) {
	tu = cpg.NewTranslationUnitDeclaration(fset, file, "src.go", this.GetCodeFromRawNode(fset, file))

	scope := this.GetScopeManager()

	// reset scope
	scope.ResetToGlobal((*cpg.Node)(tu))

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

func (this *GoLanguageFrontend) handleDecl(fset *token.FileSet, decl ast.Decl) *cpg.Declaration {
	this.LogDebug("Handling declaration (%T): %+v", decl, decl)

	switch v := decl.(type) {
	case *ast.FuncDecl:
		return (*cpg.Declaration)(this.handleFuncDecl(fset, v))
	case *ast.GenDecl:
		return (*cpg.Declaration)(this.handleGenDecl(fset, v))
	default:
		this.LogError("Not parsing declaration of type %T yet: %+v", v, v)
		// no match
		return nil
	}
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

		this.LogDebug("still here")

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

	var t *cpg.Type
	// TODO: for now, just the first result type
	if funcDecl.Type.Results == nil {
		// its proably void
		t = cpg.TypeParser_createFrom("void", false)
	} else {
		t = this.handleType(funcDecl.Type.Results.List[0].Type)
	}

	this.LogDebug("Function has return type %s", (*cpg.Node)(t).GetName())

	f.SetType(t)

	// note, the name must be set BEFORE entering the scope
	f.SetName(funcDecl.Name.Name)

	// enter scope for function
	scope.EnterScope((*cpg.Node)(f))

	if receiver != nil {
		this.LogDebug("Adding receiver %s", (*cpg.Node)(receiver).GetName())

		// add the receiver do the scope manager, so we can resolve the receiver value
		this.GetScopeManager().AddDeclaration((*cpg.Declaration)(receiver))
	}

	// TODO: for other languages, we would enter the record declaration, if
	// this is a method; however I am not quite sure if this makes sense for
	// go, since we do not have a 'this', but rather a named receiver

	this.LogDebug("Parsing function body")

	for _, param := range funcDecl.Type.Params.List {
		p := cpg.NewParamVariableDeclaration(fset, param)

		// TODO: more than one name?
		p.SetName(param.Names[0].Name)

		t := this.handleType(param.Type)

		p.SetType(t)

		// add parameter to scope
		this.GetScopeManager().AddDeclaration((*cpg.Declaration)(p))
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

func (this *GoLanguageFrontend) handleStructTypeSpec(fset *token.FileSet, typeDecl *ast.TypeSpec, structType *ast.StructType) *cpg.RecordDeclaration {
	r := cpg.NewRecordDeclaration(fset, typeDecl)

	r.SetKind("struct")
	r.SetName(typeDecl.Name.Name)

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
	r.SetName(typeDecl.Name.Name)

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

func (this *GoLanguageFrontend) handleStmt(fset *token.FileSet, stmt ast.Stmt) *cpg.Statement {
	this.LogDebug("Handling statement (%T): %+v", stmt, stmt)

	switch v := stmt.(type) {
	case *ast.ExprStmt:
		// in our cpg, each expression is also a statement,
		// so we do not need an expression statement wrapper
		return (*cpg.Statement)(this.handleExpr(fset, v.X))
	case *ast.AssignStmt:
		return (*cpg.Statement)(this.handleAssignStmt(fset, v))
	case *ast.DeclStmt:
		return (*cpg.Statement)(this.handleDeclStmt(fset, v))
	case *ast.IfStmt:
		return (*cpg.Statement)(this.handleIfStmt(fset, v))
	case *ast.SwitchStmt:
		return (*cpg.Statement)(this.handleSwitchStmt(fset, v))
	case *ast.CaseClause:
		return (*cpg.Statement)(this.handleCaseClause(fset, v))
	default:
		this.LogError("Not parsing statement of type %T yet: %+v", v, v)
	}

	return nil
}

func (this *GoLanguageFrontend) handleExpr(fset *token.FileSet, expr ast.Expr) *cpg.Expression {
	this.LogDebug("Handling expression (%T): %+v", expr, expr)

	switch v := expr.(type) {
	case *ast.CallExpr:
		return (*cpg.Expression)(this.handleCallExpr(fset, v))
	case *ast.BinaryExpr:
		return (*cpg.Expression)(this.handleBinaryExpr(fset, v))
	case *ast.SelectorExpr:
		return (*cpg.Expression)(this.handleSelectorExpr(fset, v))
	case *ast.BasicLit:
		return (*cpg.Expression)(this.handleBasicLit(fset, v))
	case *ast.Ident:
		return (*cpg.Expression)(this.handleIdent(fset, v))
	default:
		this.LogError("Could not parse expression of type %T: %+v", v, v)
	}

	// TODO: return an error instead?
	return nil
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

	return (*cpg.Expression)(stmt)
}

func (this *GoLanguageFrontend) handleIfStmt(fset *token.FileSet, ifStmt *ast.IfStmt) (expr *cpg.Expression) {
	this.LogDebug("Handling if statement: %+v", *ifStmt)

	stmt := cpg.NewIfStatement(fset, ifStmt)

	var scope = this.GetScopeManager()

	// TODO: initializer

	scope.EnterScope((*cpg.Node)(stmt))

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

	// this is a little trick, to not add the case statement in handleStmt because we added it already
	// otherwise, the order is screwed up.
	return nil
}

func (this *GoLanguageFrontend) handleCallExpr(fset *token.FileSet, callExpr *ast.CallExpr) *cpg.CallExpression {
	var c *cpg.CallExpression
	// parse the Fun field, to see which kind of expression it is
	var reference = this.handleExpr(fset, callExpr.Fun)

	if reference == nil {
		return nil
	}

	name := (*cpg.Node)(reference).GetName()

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
		c.SetName(name)
	}

	for _, arg := range callExpr.Args {
		e := this.handleExpr(fset, arg)

		if e != nil {
			c.AddArgument(e)
		}
	}

	// reference.disconnectFromGraph()

	return c
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
func (this *GoLanguageFrontend) handleSelectorExpr(fset *token.FileSet, selectorExpr *ast.SelectorExpr) *cpg.MemberExpression {
	base := this.handleExpr(fset, selectorExpr.X)

	m := cpg.NewMemberExpression(fset, selectorExpr)

	m.SetBase(base)
	(*cpg.Node)(m).SetName(selectorExpr.Sel.Name)

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

	return m
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

func (this *GoLanguageFrontend) handleIdent(fset *token.FileSet, ident *ast.Ident) *cpg.DeclaredReferenceExpression {
	ref := cpg.NewDeclaredReferenceExpression(fset, ident)

	ref.SetName(ident.Name)

	return ref
}

func (this *GoLanguageFrontend) handleType(typeExpr ast.Expr) *cpg.Type {
	this.LogDebug("Parsing type %T: %+v", typeExpr, typeExpr)

	switch v := typeExpr.(type) {
	case *ast.Ident:
		// make it a fqn according to the current package to make things easier
		fqn := fmt.Sprintf("%s.%s", this.File.Name.Name, v.Name)
		return cpg.TypeParser_createFrom(fqn, false)
	case *ast.SelectorExpr:
		// small shortcut
		fqn := fmt.Sprintf("%s.%s", v.X.(*ast.Ident).Name, v.Sel.Name)
		return cpg.TypeParser_createFrom(fqn, false)
	case *ast.StarExpr:
		t := this.handleType(v.X)

		i, err := env.GetStaticField("de/fraunhofer/aisec/cpg/graph/types/PointerType$PointerOrigin", "POINTER", jnigi.ObjectType("de/fraunhofer/aisec/cpg/graph/types/PointerType$PointerOrigin"))
		if err != nil {
			log.Fatal(err)
		}

		return t.Reference(i.(*jnigi.ObjectRef))
	case *ast.FuncType:
		// for now, we are only interested in the return type
		return this.handleType(v.Results.List[0].Type)
	}

	return cpg.UnknownType_getUnknown()
}
