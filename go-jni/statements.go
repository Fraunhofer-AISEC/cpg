package cpg

import (
	"log"

	"tekao.net/jnigi"
)

type Statement Node
type CompoundStatement Statement
type DeclarationStatement Statement

func NewCompoundStatement(env *jnigi.Env) *CompoundStatement {
	s, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/CompoundStatement")
	if err != nil {
		log.Fatal(err)
	}

	return (*CompoundStatement)(s)
}

func NewDeclarationStatement(env *jnigi.Env) *DeclarationStatement {
	s, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/DeclarationStatement")
	if err != nil {
		log.Fatal(err)
	}

	return (*DeclarationStatement)(s)
}

func (f *CompoundStatement) AddStatement(env *jnigi.Env, s *Statement) {
	(*jnigi.ObjectRef)(f).CallMethod(env, "addStatement", jnigi.Void, (*jnigi.ObjectRef)(s).Cast("de/fraunhofer/aisec/cpg/graph/statements/Statement"))
}

func (f *DeclarationStatement) SetSingleDeclaration(env *jnigi.Env, d *Declaration) {
	(*jnigi.ObjectRef)(f).CallMethod(env, "setSingleDeclaration", jnigi.Void, (*jnigi.ObjectRef)(d).Cast("de/fraunhofer/aisec/cpg/graph/declarations/Declaration"))
}
