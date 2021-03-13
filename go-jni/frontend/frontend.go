package frontend

import (
	"bytes"
	"cpg"
	"fmt"
	"go/ast"
	"go/printer"
	"go/token"
	"log"

	"tekao.net/jnigi"
)

var env *jnigi.Env

type GoLanguageFrontend struct {
	*jnigi.ObjectRef
	File *ast.File
}

func InitEnv(e *jnigi.Env) {
	env = e
}

func (g *GoLanguageFrontend) GetCodeFromRawNode(fset *token.FileSet, astNode ast.Node) string {
	var codeBuf bytes.Buffer
	_ = printer.Fprint(&codeBuf, fset, astNode)

	return codeBuf.String()
}

func (g *GoLanguageFrontend) GetScopeManager() *cpg.ScopeManager {
	scope, err := g.GetField(env, "scopeManager", jnigi.ObjectType("de/fraunhofer/aisec/cpg/passes/scopes/ScopeManager"))

	if err != nil {
		log.Fatal(err)

	}

	return (*cpg.ScopeManager)(scope.(*jnigi.ObjectRef))
}

func (g *GoLanguageFrontend) getLog() (logger *jnigi.ObjectRef, err error) {
	var ref interface{}

	ref, err = env.GetStaticField("de/fraunhofer/aisec/cpg/frontends/LanguageFrontend", "log", jnigi.ObjectType("org/slf4j/Logger"))

	logger = ref.(*jnigi.ObjectRef)

	return
}

func (g *GoLanguageFrontend) LogInfo(format string, args ...interface{}) (err error) {
	var logger *jnigi.ObjectRef

	if logger, err = g.getLog(); err != nil {
		return
	}

	_, err = logger.CallMethod(env, "info", jnigi.Void, cpg.NewString(fmt.Sprintf(format, args...)))

	return
}

func (g *GoLanguageFrontend) LogDebug(format string, args ...interface{}) (err error) {
	var logger *jnigi.ObjectRef

	if logger, err = g.getLog(); err != nil {
		return
	}

	_, err = logger.CallMethod(env, "debug", jnigi.Void, cpg.NewString(fmt.Sprintf(format, args...)))

	return
}

func (g *GoLanguageFrontend) LogError(format string, args ...interface{}) (err error) {
	var logger *jnigi.ObjectRef

	if logger, err = g.getLog(); err != nil {
		return
	}

	_, err = logger.CallMethod(env, "error", jnigi.Void, cpg.NewString(fmt.Sprintf(format, args...)))

	return
}
