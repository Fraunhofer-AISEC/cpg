package cpg

import (
	"log"

	"tekao.net/jnigi"
)

type GoLanguageFrontend jnigi.ObjectRef

func (g *GoLanguageFrontend) GetScopeManager(env *jnigi.Env) *ScopeManager {
	scope, err := (*jnigi.ObjectRef)(g).GetField(env, "scopeManager", jnigi.ObjectType("de/fraunhofer/aisec/cpg/passes/scopes/ScopeManager"))

	if err != nil {
		log.Fatal(err)
	}

	return (*ScopeManager)(scope.(*jnigi.ObjectRef))
}
