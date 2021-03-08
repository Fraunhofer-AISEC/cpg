package frontend

import (
	"cpg"
	"fmt"
	"log"

	"tekao.net/jnigi"
)

type GoLanguageFrontend jnigi.ObjectRef

func (g *GoLanguageFrontend) GetScopeManager(env *jnigi.Env) *cpg.ScopeManager {
	scope, err := (*jnigi.ObjectRef)(g).GetField(env, "scopeManager", jnigi.ObjectType("de/fraunhofer/aisec/cpg/passes/scopes/ScopeManager"))

	if err != nil {
		log.Fatal(err)
	}

	return (*cpg.ScopeManager)(scope.(*jnigi.ObjectRef))
}

func (g *GoLanguageFrontend) getLog(env *jnigi.Env) (logger *jnigi.ObjectRef, err error) {
	var ref interface{}

	ref, err = env.GetStaticField("de/fraunhofer/aisec/cpg/frontends/LanguageFrontend", "log", jnigi.ObjectType("org/slf4j/Logger"))

	logger = ref.(*jnigi.ObjectRef)

	return
}

func (g *GoLanguageFrontend) LogInfo(env *jnigi.Env, format string, args ...interface{}) (err error) {
	var logger *jnigi.ObjectRef

	if logger, err = g.getLog(env); err != nil {
		return
	}

	_, err = logger.CallMethod(env, "info", jnigi.Void, cpg.NewString(env, fmt.Sprintf(format, args...)))

	return
}

func (g *GoLanguageFrontend) LogDebug(env *jnigi.Env, format string, args ...interface{}) (err error) {
	var logger *jnigi.ObjectRef

	if logger, err = g.getLog(env); err != nil {
		return
	}

	_, err = logger.CallMethod(env, "debug", jnigi.Void, cpg.NewString(env, fmt.Sprintf(format, args...)))

	return
}

func (g *GoLanguageFrontend) LogError(env *jnigi.Env, format string, args ...interface{}) (err error) {
	var logger *jnigi.ObjectRef

	if logger, err = g.getLog(env); err != nil {
		return
	}

	_, err = logger.CallMethod(env, "error", jnigi.Void, cpg.NewString(env, fmt.Sprintf(format, args...)))

	return
}
