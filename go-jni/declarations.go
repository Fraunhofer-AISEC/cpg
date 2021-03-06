package cpg

import (
	"log"

	"tekao.net/jnigi"
)

type Declaration jnigi.ObjectRef
type TranslationUnitDeclaration Declaration
type FunctionDeclaration Declaration
type VariableDeclaration Declaration
type ParamVariableDeclaration Declaration

func (t *TranslationUnitDeclaration) AddDeclaration(env *jnigi.Env, d *jnigi.ObjectRef) (err error) {
	_, err = (*jnigi.ObjectRef)(t).CallMethod(env, "addDeclaration", jnigi.Void, d.Cast("de/fraunhofer/aisec/cpg/graph/declarations/Declaration"))

	return
}

func (f *FunctionDeclaration) SetName(env *jnigi.Env, s string) error {
	return (*Node)(f).SetName(env, s)
}

func (f *FunctionDeclaration) AddParameter(env *jnigi.Env, p *ParamVariableDeclaration) {
	(*jnigi.ObjectRef)(f).CallMethod(env, "addParameter", jnigi.Void, (*jnigi.ObjectRef)(p))
}

func (f *FunctionDeclaration) SetBody(env *jnigi.Env, s *Statement) (err error) {
	_, err = (*jnigi.ObjectRef)(f).CallMethod(env, "setBody", jnigi.Void, (*jnigi.ObjectRef)(s).Cast("de/fraunhofer/aisec/cpg/graph/statements/Statement"))

	return
}

func (p *ParamVariableDeclaration) SetType(env *jnigi.Env, t *Type) {
	(*HasType)(p).SetType(env, t)
}

func (p *ParamVariableDeclaration) SetName(env *jnigi.Env, s string) error {
	return (*Node)(p).SetName(env, s)
}

func (v *VariableDeclaration) SetType(env *jnigi.Env, t *Type) {
	(*HasType)(v).SetType(env, t)
	//(*jnigi.ObjectRef)(v).CallMethod(env, "setType", jnigi.Void, (*jnigi.ObjectRef)(t))
}

func (v *VariableDeclaration) SetName(env *jnigi.Env, s string) error {
	return (*Node)(v).SetName(env, s)
}

func (v *VariableDeclaration) SetInitializer(env *jnigi.Env, e *Expression) (err error) {
	_, err = (*jnigi.ObjectRef)(v).CallMethod(env, "setInitializer", jnigi.Void, (*jnigi.ObjectRef)(v).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))

	return
}

func NewTranslationUnitDeclaration(env *jnigi.Env) *TranslationUnitDeclaration {
	tu, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/declarations/TranslationUnitDeclaration")
	if err != nil {
		log.Fatal(err)
	}

	return (*TranslationUnitDeclaration)(tu)
}

func NewFunctionDeclaration(env *jnigi.Env) *FunctionDeclaration {
	tu, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/declarations/FunctionDeclaration")
	if err != nil {
		log.Fatal(err)
	}

	return (*FunctionDeclaration)(tu)
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
