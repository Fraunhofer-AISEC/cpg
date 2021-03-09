package cpg

import (
	"go/ast"
	"go/token"
	"log"

	"tekao.net/jnigi"
)

type Statement Node
type CompoundStatement Statement
type DeclarationStatement Statement
type IfStatement Statement

func NewCompoundStatement(fset *token.FileSet, env *jnigi.Env, astNode ast.Node) *CompoundStatement {
	s, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/CompoundStatement")
	if err != nil {
		log.Fatal(err)
	}

	updateCode(fset, env, (*Node)(s), astNode)

	return (*CompoundStatement)(s)
}

func NewDeclarationStatement(fset *token.FileSet, env *jnigi.Env, astNode ast.Node) *DeclarationStatement {
	s, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/DeclarationStatement")
	if err != nil {
		log.Fatal(err)
	}

	updateCode(fset, env, (*Node)(s), astNode)

	return (*DeclarationStatement)(s)
}

func NewIfStatement(fset *token.FileSet, env *jnigi.Env, astNode ast.Node) *IfStatement {
	s, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/IfStatement")
	if err != nil {
		log.Fatal(err)
	}

	updateCode(fset, env, (*Node)(s), astNode)

	return (*IfStatement)(s)
}

func (f *CompoundStatement) AddStatement(env *jnigi.Env, s *Statement) {
	(*jnigi.ObjectRef)(f).CallMethod(env, "addStatement", jnigi.Void, (*jnigi.ObjectRef)(s).Cast("de/fraunhofer/aisec/cpg/graph/statements/Statement"))
}

func (f *DeclarationStatement) SetSingleDeclaration(env *jnigi.Env, d *Declaration) {
	(*jnigi.ObjectRef)(f).CallMethod(env, "setSingleDeclaration", jnigi.Void, (*jnigi.ObjectRef)(d).Cast("de/fraunhofer/aisec/cpg/graph/declarations/Declaration"))
}

func (m *IfStatement) SetThenStatement(env *jnigi.Env, s *Statement) {
	(*jnigi.ObjectRef)(m).SetField(env, "thenStatement", (*jnigi.ObjectRef)(s).Cast("de/fraunhofer/aisec/cpg/graph/statements/Statement"))
}

func (m *IfStatement) SetElseStatement(env *jnigi.Env, s *Statement) {
	(*jnigi.ObjectRef)(m).SetField(env, "elseStatement", (*jnigi.ObjectRef)(s).Cast("de/fraunhofer/aisec/cpg/graph/statements/Statement"))
}

func (m *IfStatement) SetCondition(env *jnigi.Env, e *Expression) {
	(*jnigi.ObjectRef)(m).SetField(env, "condition", (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
}
