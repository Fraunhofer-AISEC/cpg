package cpg

import (
	"log"

	"tekao.net/jnigi"
)

type Declaration jnigi.ObjectRef
type TranslationUnitDeclaration Declaration
type FunctionDeclaration Declaration
type MethodDeclaration FunctionDeclaration
type RecordDeclaration Declaration
type FieldDeclaration Declaration
type VariableDeclaration Declaration
type ParamVariableDeclaration Declaration
type NamespaceDeclaration Declaration

func (n *NamespaceDeclaration) SetName(env *jnigi.Env, s string) error {
	return (*Node)(n).SetName(env, s)
}

func (f *FunctionDeclaration) SetName(env *jnigi.Env, s string) error {
	return (*Node)(f).SetName(env, s)
}

func (f *FunctionDeclaration) SetType(env *jnigi.Env, t *Type) {
	(*HasType)(f).SetType(env, t)
}

func (f *FunctionDeclaration) AddParameter(env *jnigi.Env, p *ParamVariableDeclaration) {
	(*jnigi.ObjectRef)(f).CallMethod(env, "addParameter", jnigi.Void, (*jnigi.ObjectRef)(p))
}

func (f *FunctionDeclaration) SetBody(env *jnigi.Env, s *Statement) (err error) {
	_, err = (*jnigi.ObjectRef)(f).CallMethod(env, "setBody", jnigi.Void, (*jnigi.ObjectRef)(s).Cast("de/fraunhofer/aisec/cpg/graph/statements/Statement"))

	return
}

func (m *MethodDeclaration) SetName(env *jnigi.Env, s string) error {
	return (*Node)(m).SetName(env, s)
}

func (m *MethodDeclaration) SetType(env *jnigi.Env, t *Type) {
	(*HasType)(m).SetType(env, t)
}

func (p *ParamVariableDeclaration) SetType(env *jnigi.Env, t *Type) {
	(*HasType)(p).SetType(env, t)
}

func (p *ParamVariableDeclaration) SetName(env *jnigi.Env, s string) error {
	return (*Node)(p).SetName(env, s)
}

func (f *FieldDeclaration) SetName(env *jnigi.Env, s string) error {
	return (*Node)(f).SetName(env, s)
}

func (f *FieldDeclaration) SetType(env *jnigi.Env, t *Type) {
	(*HasType)(f).SetType(env, t)
}

func (v *VariableDeclaration) SetType(env *jnigi.Env, t *Type) {
	(*HasType)(v).SetType(env, t)
}

func (v *VariableDeclaration) SetName(env *jnigi.Env, s string) error {
	return (*Node)(v).SetName(env, s)
}

func (v *VariableDeclaration) SetInitializer(env *jnigi.Env, e *Expression) (err error) {
	_, err = (*jnigi.ObjectRef)(v).CallMethod(env, "setInitializer", jnigi.Void, (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))

	return
}

func (r *RecordDeclaration) SetName(env *jnigi.Env, s string) error {
	return (*Node)(r).SetName(env, s)
}

func (r *RecordDeclaration) SetKind(env *jnigi.Env, s string) error {
	return (*jnigi.ObjectRef)(r).SetField(env, "kind", NewString(env, s))
}

func NewTranslationUnitDeclaration(env *jnigi.Env) *TranslationUnitDeclaration {
	tu, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/declarations/TranslationUnitDeclaration")
	if err != nil {
		log.Fatal(err)
	}

	return (*TranslationUnitDeclaration)(tu)
}

func NewNamespaceDeclaration(env *jnigi.Env) *NamespaceDeclaration {
	tu, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/declarations/NamespaceDeclaration")
	if err != nil {
		log.Fatal(err)
	}

	return (*NamespaceDeclaration)(tu)
}

func NewFunctionDeclaration(env *jnigi.Env) *FunctionDeclaration {
	tu, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/declarations/FunctionDeclaration")
	if err != nil {
		log.Fatal(err)
	}

	return (*FunctionDeclaration)(tu)
}

func NewMethodDeclaration(env *jnigi.Env) *MethodDeclaration {
	tu, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/declarations/MethodDeclaration")
	if err != nil {
		log.Fatal(err)
	}

	return (*MethodDeclaration)(tu)
}

func NewRecordDeclaration(env *jnigi.Env) *RecordDeclaration {
	tu, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/declarations/RecordDeclaration")
	if err != nil {
		log.Fatal(err)
	}

	return (*RecordDeclaration)(tu)
}

func NewVariableDeclaration(env *jnigi.Env) *VariableDeclaration {
	tu, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/declarations/VariableDeclaration")
	if err != nil {
		log.Fatal(err)
	}

	return (*VariableDeclaration)(tu)
}

func NewParamVariableDeclaration(env *jnigi.Env) *ParamVariableDeclaration {
	tu, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/declarations/ParamVariableDeclaration")
	if err != nil {
		log.Fatal(err)
	}

	return (*ParamVariableDeclaration)(tu)
}

func NewFieldDeclaration(env *jnigi.Env) *FieldDeclaration {
	tu, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/declarations/FieldDeclaration")
	if err != nil {
		log.Fatal(err)
	}

	return (*FieldDeclaration)(tu)
}
