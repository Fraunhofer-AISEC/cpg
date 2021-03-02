package cpg

import (
	"log"

	"tekao.net/jnigi"
)

type Statement Node
type CompoundStatement Statement

func NewCompoundStatement(env *jnigi.Env) *CompoundStatement {
	s, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/CompoundStatement")
	if err != nil {
		log.Fatal(err)
	}

	return (*CompoundStatement)(s)
}

func (f *CompoundStatement) AddStatement(env *jnigi.Env, s *Statement) {
	(*jnigi.ObjectRef)(f).CallMethod(env, "addStatement", jnigi.Void, (*jnigi.ObjectRef)(s).Cast("de/fraunhofer/aisec/cpg/graph/statements/Statement"))
}
