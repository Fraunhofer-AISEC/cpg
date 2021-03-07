package main

import (
	"bytes"
	"cpg"
	"fmt"
	"go/ast"
	"go/parser"
	"go/printer"
	"go/token"
	"log"
	"strconv"
	"unsafe"

	"tekao.net/jnigi"
)

//#include <jni.h>
import "C"

func main() {

}

func handleFuncDecl(this *cpg.GoLanguageFrontend, fset *token.FileSet, env *jnigi.Env, funcDecl *ast.FuncDecl) *jnigi.ObjectRef {
	this.LogDebug(env, "Handling func Decl: %+v", *funcDecl)

	var f *cpg.FunctionDeclaration
	if funcDecl.Recv != nil {
		m := cpg.NewMethodDeclaration(env)

		// TODO: why is this a list?
		var recv = funcDecl.Recv.List[0]

		// TODO: currently, we lose the name of our receiver, not sure how to fix that
		// and we probably need it for member call expression

		var recordType = handleType(env, recv.Type)

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
		f = cpg.NewFunctionDeclaration(env)
	}

	f.SetName(env, funcDecl.Name.Name)

	// TODO: for other languages, we would enter the record declaration, if
	// this is a method; however I am not quite sure if this makes sense for
	// go, since we do not have a 'this', but rather a named receiver

	// enter scope for function body
	this.GetScopeManager(env).EnterScope(env, (*cpg.Node)(f))

	for _, param := range funcDecl.Type.Params.List {
		p := cpg.NewParamVariableDeclaration(env)
		// TODO: more than one name?
		p.SetName(env, param.Names[0].Name)

		t := handleType(env, param.Type)

		p.SetType(env, t)

		// add parameter to scope
		this.GetScopeManager(env).AddDeclaration(env, (*cpg.Declaration)(p))
	}

	// parse body
	s := handleBlockStmt(this, fset, env, funcDecl.Body)

	err := f.SetBody(env, (*cpg.Statement)(s))
	if err != nil {
		log.Printf("%s", err)
	}

	// leave scope
	this.GetScopeManager(env).LeaveScope(env, (*cpg.Node)(f))

	return (*jnigi.ObjectRef)(f)
}

func handleGenDecl(this *cpg.GoLanguageFrontend, fset *token.FileSet, env *jnigi.Env, genDecl *ast.GenDecl) *jnigi.ObjectRef {
	// TODO: Handle multiple declarations
	for _, spec := range genDecl.Specs {
		switch v := spec.(type) {
		case *ast.ValueSpec:
			return (*jnigi.ObjectRef)(handleValueSpec(this, fset, env, v))
		case *ast.TypeSpec:
			return (*jnigi.ObjectRef)(handleTypeSpec(this, fset, env, v))
		default:
			this.LogError(env, "Not parsing specication of type %T yet: %+v", v, v)
		}
	}

	return nil
}

func handleValueSpec(this *cpg.GoLanguageFrontend, fset *token.FileSet, env *jnigi.Env, valueDecl *ast.ValueSpec) *cpg.Declaration {
	// TODO: more names
	var ident = valueDecl.Names[0]

	d := (cpg.NewVariableDeclaration(env))
	d.SetName(env, ident.Name)

	if valueDecl.Type != nil {
		t := handleType(env, valueDecl.Type)

		d.SetType(env, t)
	}

	// add an initializer
	if len(valueDecl.Values) > 0 {
		fmt.Printf("initializer: %v\n", valueDecl.Values[0])

		// TODO: How to deal with multiple values
		var expr = handleExpr( /*this,*/ fset, env, valueDecl.Values[0])

		err := d.SetInitializer(env, expr)
		if err != nil {
			log.Fatal(err)
		}
	}

	return (*cpg.Declaration)(d)
}

func handleTypeSpec(this *cpg.GoLanguageFrontend, fset *token.FileSet, env *jnigi.Env, typeDecl *ast.TypeSpec) *cpg.Declaration {
	err := this.LogInfo(env, "Type specifier with name %s and type (%T, %+v)", typeDecl.Name.Name, typeDecl.Type, typeDecl.Type)
	if err != nil {
		log.Fatal(err)
	}

	switch v := typeDecl.Type.(type) {
	case *ast.StructType:
		return (*cpg.Declaration)(handleStructTypeSpec(this, fset, env, typeDecl, v))
	case *ast.InterfaceType:
		return (*cpg.Declaration)(handleInterfaceTypeSpec(this, fset, env, typeDecl, v))
	}

	return nil
}

func handleStructTypeSpec(this *cpg.GoLanguageFrontend, fset *token.FileSet, env *jnigi.Env, typeDecl *ast.TypeSpec, structType *ast.StructType) *cpg.RecordDeclaration {
	r := cpg.NewRecordDeclaration(env)

	r.SetKind(env, "struct")
	r.SetName(env, typeDecl.Name.Name)

	var scope = this.GetScopeManager(env)

	scope.EnterScope(env, (*cpg.Node)(r))

	// TODO: parse members

	if !structType.Incomplete {
		for _, field := range structType.Fields.List {
			f := cpg.NewFieldDeclaration(env)

			// TODO: Multiple names?
			f.SetName(env, field.Names[0].Name)

			t := handleType(env, field.Type)

			f.SetType(env, t)

			scope.AddDeclaration(env, (*cpg.Declaration)(f))
		}
	}

	scope.LeaveScope(env, (*cpg.Node)(r))

	return r
}

func handleInterfaceTypeSpec(this *cpg.GoLanguageFrontend, fset *token.FileSet, env *jnigi.Env, typeDecl *ast.TypeSpec, interfaceType *ast.InterfaceType) *cpg.RecordDeclaration {
	r := cpg.NewRecordDeclaration(env)

	r.SetKind(env, "interface")
	r.SetName(env, typeDecl.Name.Name)

	var scope = this.GetScopeManager(env)

	scope.EnterScope(env, (*cpg.Node)(r))

	if !interfaceType.Incomplete {
		for _, method := range interfaceType.Methods.List {
			m := cpg.NewMethodDeclaration(env)

			t := handleType(env, method.Type)

			m.SetType(env, t)
			m.SetName(env, method.Names[0].Name)

			scope.AddDeclaration(env, (*cpg.Declaration)(m))
		}
	}

	scope.LeaveScope(env, (*cpg.Node)(r))

	return r
}

func handleBlockStmt(this *cpg.GoLanguageFrontend, fset *token.FileSet, env *jnigi.Env, blockStmt *ast.BlockStmt) *cpg.CompoundStatement {
	c := cpg.NewCompoundStatement(env)

	// enter scope
	this.GetScopeManager(env).EnterScope(env, (*cpg.Node)(c))

	for _, stmt := range blockStmt.List {
		var s *cpg.Statement

		switch v := stmt.(type) {
		case *ast.ExprStmt:
			// in our cpg, each expression is also a statement,
			// so we do not need an expression statement wrapper
			s = (*cpg.Statement)(handleExpr(fset, env, v.X))
			fmt.Printf("exprStmt: %+v\n", v)
			fmt.Printf("statement: %+v\n", s)
		case *ast.AssignStmt:
			s = (*cpg.Statement)(handleAssignStmt(fset, env, v))
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

func handleExpr(fset *token.FileSet, env *jnigi.Env, expr ast.Expr) *cpg.Expression {
	switch v := expr.(type) {
	case *ast.CallExpr:
		return (*cpg.Expression)(handleCallExpr(fset, env, v))
	case *ast.BinaryExpr:
		return (*cpg.Expression)(handleBinaryExpr(fset, env, v))
	case *ast.BasicLit:
		return (*cpg.Expression)(handleBasicLit(fset, env, v))
	case *ast.Ident:
		return (*cpg.Expression)(handleIdent(fset, env, v))
	default:
		log.Printf("Could not parse expression of type %T: %+v\n", v, v)
	}

	// TODO: return an error instead?
	return nil
}

func handleAssignStmt(fset *token.FileSet, env *jnigi.Env, assignStmt *ast.AssignStmt) (expr *cpg.Expression) {
	// TODO: more than one Rhs?!
	rhs := handleExpr(fset, env, assignStmt.Rhs[0])

	if assignStmt.Tok == token.DEFINE {
		// lets create a variable declaration (wrapped with a declaration stmt) with this, because we define the variable here
		stmt := cpg.NewDeclarationStatement(env)

		// TODO: assignment of multiple values
		d := cpg.NewVariableDeclaration(env)

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

func handleCallExpr(fset *token.FileSet, env *jnigi.Env, callExpr *ast.CallExpr) *cpg.CallExpression {
	c := cpg.NewCallExpression(env)

	var nameBuf bytes.Buffer
	_ = printer.Fprint(&nameBuf, fset, callExpr.Fun)

	c.SetName(env, nameBuf.String())

	for _, arg := range callExpr.Args {
		e := handleExpr(fset, env, arg)
		fmt.Printf("%+v\n", e)

		if e != nil {
			c.AddArgument(env, e)
		}
	}

	return c
}

func handleBinaryExpr(fset *token.FileSet, env *jnigi.Env, binaryExpr *ast.BinaryExpr) *cpg.BinaryOperator {
	b := cpg.NewBinaryOperator(env)

	lhs := handleExpr(fset, env, binaryExpr.X)
	rhs := handleExpr(fset, env, binaryExpr.Y)

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

func handleBasicLit(fset *token.FileSet, env *jnigi.Env, lit *ast.BasicLit) *cpg.Literal {
	l := cpg.NewLiteral(env)

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

func handleIdent(fset *token.FileSet, env *jnigi.Env, ident *ast.Ident) *cpg.DeclaredReferenceExpression {
	ref := cpg.NewDeclaredReferenceExpression(env)

	ref.SetName(env, ident.Name)

	return ref
}

func handleType(env *jnigi.Env, typeExpr ast.Expr) *cpg.Type {
	log.Printf("Parsing type %T: %+v\n", typeExpr, typeExpr)

	switch v := typeExpr.(type) {
	case *ast.Ident:
		return cpg.TypeParser_createFrom(env, v.Name, false)
	case *ast.FuncType:
		// for now, we are only interested in the return type
		return handleType(env, v.Results.List[0].Type)
	}

	return nil
}

//export Java_de_fraunhofer_aisec_cpg_frontends_golang_GoLanguageFrontend_parseInternal
func Java_de_fraunhofer_aisec_cpg_frontends_golang_GoLanguageFrontend_parseInternal(envPointer *C.JNIEnv, thisPtr C.jobject, arg1 C.jobject) C.jobject {
	//func Java_de_fraunhofer_aisec_cpg_JNITest_parse(envPointer *C.JNIEnv, clazz C.jclass, arg1 C.jobject) C.jobject {
	env := jnigi.WrapEnv(unsafe.Pointer(envPointer))

	this := (*cpg.GoLanguageFrontend)(jnigi.WrapJObject(uintptr(thisPtr), "de/fraunhofer/aisec/cpg/frontends/golang/GoLanguageFrontend", false))

	srcObject := jnigi.WrapJObject(uintptr(arg1), "java/lang/String", false)

	tu := cpg.NewTranslationUnitDeclaration(env)

	scope := this.GetScopeManager(env)

	// reset scope
	scope.ResetToGlobal(env, (*cpg.Node)(tu))

	src, err := srcObject.CallMethod(env, "getBytes", jnigi.Byte|jnigi.Array)
	if err != nil {
		log.Fatal(err)
	}

	fset := token.NewFileSet()
	file, err := parser.ParseFile(fset, "src.go", string(src.([]byte)), 0)

	if err != nil {
		panic(err)
	}

	// create a new namespace declaration, representing the package
	p := cpg.NewNamespaceDeclaration(env)

	p.SetName(env, file.Name.Name)

	// enter scope
	scope.EnterScope(env, (*cpg.Node)(p))

	for _, decl := range file.Decls {
		fmt.Printf("%+v\n", decl)

		var d *jnigi.ObjectRef

		switch v := decl.(type) {
		case *ast.FuncDecl:
			d = handleFuncDecl(this, fset, env, v)
		case *ast.GenDecl:
			d = handleGenDecl(this, fset, env, v)
		default:
			this.LogError(env, "Not parsing declaration of type %T yet: %+v", v, v)
			// no match
		}

		fmt.Printf("parsed decl: %v\n", d)

		if d != nil {
			fmt.Printf("%+v\n", d)

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

	return C.jobject((*jnigi.ObjectRef)(tu).JObject())
}
