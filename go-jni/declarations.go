package cpg

import (
	"go/ast"
	"go/token"
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

func (m *MethodDeclaration) SetReceiver(env *jnigi.Env, v *VariableDeclaration) error {
	return (*jnigi.ObjectRef)(m).SetField(env, "receiver", (*jnigi.ObjectRef)(v))
}

func (m *MethodDeclaration) GetReceiver(env *jnigi.Env) *VariableDeclaration {
	o, err := (*jnigi.ObjectRef)(m).GetField(env, "receiver", jnigi.ObjectType("de/fraunhofer/aisec/cpg/graph/declarations/VariableDeclaration"))

	if err != nil {
		log.Fatal(err)
	}

	return (*VariableDeclaration)(o.(*jnigi.ObjectRef))
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

func (v *VariableDeclaration) IsNil() bool {
	return (*jnigi.ObjectRef)(v).IsNil()
}

func (v *VariableDeclaration) SetInitializer(env *jnigi.Env, e *Expression) (err error) {
	_, err = (*jnigi.ObjectRef)(v).CallMethod(env, "setInitializer", jnigi.Void, (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))

	return
}

func (v *VariableDeclaration) Declaration() *Declaration {
	return (*Declaration)(v)
}

func (r *RecordDeclaration) SetName(env *jnigi.Env, s string) error {
	return (*Node)(r).SetName(env, s)
}

func (r *RecordDeclaration) SetKind(env *jnigi.Env, s string) error {
	return (*jnigi.ObjectRef)(r).SetField(env, "kind", NewString(env, s))
}

func (r *RecordDeclaration) AddMethod(env *jnigi.Env, m *MethodDeclaration) (err error) {
	_, err = (*jnigi.ObjectRef)(r).CallMethod(env, "addMethod", jnigi.Void, (*jnigi.ObjectRef)(m))

	return
}

func (r *RecordDeclaration) IsNil() bool {
	return (*jnigi.ObjectRef)(r).IsNil()
}

func (r *MethodDeclaration) IsNil() bool {
	return (*jnigi.ObjectRef)(r).IsNil()
}

func NewTranslationUnitDeclaration(fset *token.FileSet, env *jnigi.Env, astNode ast.Node, name string, code string) *TranslationUnitDeclaration {
	o, err := env.CallStaticMethod("de/fraunhofer/aisec/cpg/graph/NodeBuilder", "newTranslationUnitDeclaration", jnigi.ObjectType("de/fraunhofer/aisec/cpg/graph/declarations/TranslationUnitDeclaration"), NewString(env, name), NewString(env, code))
	if err != nil {
		log.Fatal(err)
	}

	return (*TranslationUnitDeclaration)(o.(*jnigi.ObjectRef))
}

func NewNamespaceDeclaration(fset *token.FileSet, env *jnigi.Env, astNode ast.Node, name string, code string) *NamespaceDeclaration {
	o, err := env.CallStaticMethod("de/fraunhofer/aisec/cpg/graph/NodeBuilder", "newNamespaceDeclaration", jnigi.ObjectType("de/fraunhofer/aisec/cpg/graph/declarations/NamespaceDeclaration"), NewString(env, name), NewString(env, code))
	if err != nil {
		log.Fatal(err)
	}

	return (*NamespaceDeclaration)(o.(*jnigi.ObjectRef))
}

func NewFunctionDeclaration(fset *token.FileSet, env *jnigi.Env, astNode ast.Node) *FunctionDeclaration {
	tu, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/declarations/FunctionDeclaration")
	if err != nil {
		log.Fatal(err)
	}

	updateCode(fset, env, (*Node)(tu), astNode)

	return (*FunctionDeclaration)(tu)
}

func NewMethodDeclaration(fset *token.FileSet, env *jnigi.Env, astNode ast.Node) *MethodDeclaration {
	tu, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/declarations/MethodDeclaration")
	if err != nil {
		log.Fatal(err)
	}

	updateCode(fset, env, (*Node)(tu), astNode)

	return (*MethodDeclaration)(tu)
}

func NewRecordDeclaration(fset *token.FileSet, env *jnigi.Env, astNode ast.Node) *RecordDeclaration {
	tu, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/declarations/RecordDeclaration")
	if err != nil {
		log.Fatal(err)
	}

	updateCode(fset, env, (*Node)(tu), astNode)

	return (*RecordDeclaration)(tu)
}

func NewVariableDeclaration(fset *token.FileSet, env *jnigi.Env, astNode ast.Node) *VariableDeclaration {
	tu, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/declarations/VariableDeclaration")
	if err != nil {
		log.Fatal(err)
	}

	updateCode(fset, env, (*Node)(tu), astNode)

	return (*VariableDeclaration)(tu)
}

func NewParamVariableDeclaration(fset *token.FileSet, env *jnigi.Env, astNode ast.Node) *ParamVariableDeclaration {
	tu, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/declarations/ParamVariableDeclaration")
	if err != nil {
		log.Fatal(err)
	}

	updateCode(fset, env, (*Node)(tu), astNode)

	return (*ParamVariableDeclaration)(tu)
}

func NewFieldDeclaration(fset *token.FileSet, env *jnigi.Env, astNode ast.Node) *FieldDeclaration {
	tu, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/declarations/FieldDeclaration")
	if err != nil {
		log.Fatal(err)
	}

	updateCode(fset, env, (*Node)(tu), astNode)

	return (*FieldDeclaration)(tu)
}
