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
	f := cpg.NewFunctionDeclaration(env)

	f.SetName(env, funcDecl.Name.Name)

	// enter scope
	this.GetScopeManager(env).EnterScope(env, (*cpg.Node)(f))

	for _, field := range funcDecl.Type.Params.List {
		p := cpg.NewParamVariableDeclaration(env)
		// TODO: more than one name?
		p.SetName(env, field.Names[0].Name)

		var typeNameBuf bytes.Buffer
		_ = printer.Fprint(&typeNameBuf, fset, field.Type)

		t := cpg.TypeParser_createFrom(env, typeNameBuf.String(), false)

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
		default:
			fmt.Printf("%+v\n", stmt)
		}

		if s != nil {
			// add statement to scope
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
	case *ast.BasicLit:
		return (*cpg.Expression)(handleBasicLit(fset, env, v))
	case *ast.Ident:
		return (*cpg.Expression)(handleIdent(fset, env, v))
	}

	// TODO: return an error instead?
	return nil
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

func handleBasicLit(fset *token.FileSet, env *jnigi.Env, lit *ast.BasicLit) *cpg.Literal {
	l := cpg.NewLiteral(env)

	var value interface{}
	var t *cpg.Type

	switch lit.Kind {
	case token.STRING:
		// strip the "
		value = cpg.NewString(env, lit.Value[1:len(lit.Value)-1])
		t = cpg.TypeParser_createFrom(env, "string", false)
		break
	case token.INT:
		i, _ := strconv.ParseInt(lit.Value, 10, 64)
		value = int(i)
		t = cpg.TypeParser_createFrom(env, "int", false)
		break
	case token.FLOAT:
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

//export Java_de_fraunhofer_aisec_cpg_frontends_golang_GoLanguageFrontend_parseInternal
func Java_de_fraunhofer_aisec_cpg_frontends_golang_GoLanguageFrontend_parseInternal(envPointer *C.JNIEnv, thisPtr C.jobject, arg1 C.jobject) C.jobject {
	//func Java_de_fraunhofer_aisec_cpg_JNITest_parse(envPointer *C.JNIEnv, clazz C.jclass, arg1 C.jobject) C.jobject {
	env := jnigi.WrapEnv(unsafe.Pointer(envPointer))

	this := (*cpg.GoLanguageFrontend)(jnigi.WrapJObject(uintptr(thisPtr), "de/fraunhofer/aisec/cpg/frontends/golang/GoLanguageFrontend", false))

	srcObject := jnigi.WrapJObject(uintptr(arg1), "java/lang/String", false)

	tu := cpg.NewTranslationUnitDeclaration(env)

	src, err := srcObject.CallMethod(env, "getBytes", jnigi.Byte|jnigi.Array)
	if err != nil {
		log.Fatal(err)
	}

	fset := token.NewFileSet()
	file, err := parser.ParseFile(fset, "src.go", string(src.([]byte)), 0)

	if err != nil {
		panic(err)
	}

	for _, decl := range file.Decls {
		fmt.Printf("%+v\n", decl)

		var d *jnigi.ObjectRef

		switch v := decl.(type) {
		case *ast.FuncDecl:
			d = handleFuncDecl(this, fset, env, v)
		case *ast.GenDecl:
			// ignore for now
			break
		default:
			// no match
		}

		if d != nil {
			fmt.Printf("%+v\n", d)

			err = tu.AddDeclaration(env, d)
			if err != nil {
				panic(err)
			}
		}
	}

	return C.jobject((*jnigi.ObjectRef)(tu).JObject())
}
