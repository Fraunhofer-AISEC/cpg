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
type SwitchStatement Statement
type CaseStatement Statement
type DefaultStatement Statement

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

func NewSwitchStatement(fset *token.FileSet, env *jnigi.Env, astNode ast.Node) *SwitchStatement {
	s, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/SwitchStatement")
	if err != nil {
		log.Fatal(err)
	}

	updateCode(fset, env, (*Node)(s), astNode)

	return (*SwitchStatement)(s)
}

func NewCaseStatement(fset *token.FileSet, env *jnigi.Env, astNode ast.Node) *CaseStatement {
	s, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/CaseStatement")
	if err != nil {
		log.Fatal(err)
	}

	updateCode(fset, env, (*Node)(s), astNode)

	return (*CaseStatement)(s)
}

func NewDefaultStatement(fset *token.FileSet, env *jnigi.Env, astNode ast.Node) *DefaultStatement {
	s, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/DefaultStatement")
	if err != nil {
		log.Fatal(err)
	}

	updateCode(fset, env, (*Node)(s), astNode)

	return (*DefaultStatement)(s)
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

func (s *SwitchStatement) SetCondition(env *jnigi.Env, e *Expression) {
	(*jnigi.ObjectRef)(s).SetField(env, "selector", (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
}

func (sw *SwitchStatement) SetStatement(env *jnigi.Env, s *Statement) {
	(*jnigi.ObjectRef)(sw).SetField(env, "statement", (*jnigi.ObjectRef)(s).Cast("de/fraunhofer/aisec/cpg/graph/statements/Statement"))
}

func (sw *SwitchStatement) SetInitializerStatement(env *jnigi.Env, s *Statement) {
	(*jnigi.ObjectRef)(sw).SetField(env, "initializerStatement", (*jnigi.ObjectRef)(s).Cast("de/fraunhofer/aisec/cpg/graph/statements/Statement"))
}
