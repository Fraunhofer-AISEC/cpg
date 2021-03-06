package cpg

import (
	"log"

	"tekao.net/jnigi"
)

type Expression Statement
type CallExpression Expression
type Literal Expression
type DeclaredReferenceExpression Expression

func NewCallExpression(env *jnigi.Env) *CallExpression {
	c, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/expressions/CallExpression")
	if err != nil {
		log.Fatal(err)
	}

	return (*CallExpression)(c)
}

func NewLiteral(env *jnigi.Env) *Literal {
	l, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/expressions/Literal")
	if err != nil {
		log.Fatal(err)
	}

	return (*Literal)(l)
}

func NewDeclaredReferenceExpression(env *jnigi.Env) *DeclaredReferenceExpression {
	l, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/expressions/DeclaredReferenceExpression")
	if err != nil {
		log.Fatal(err)
	}

	return (*DeclaredReferenceExpression)(l)
}

func (e *Expression) SetType(env *jnigi.Env, t *Type) {
	(*HasType)(e).SetType(env, t)
	//(*jnigi.ObjectRef)(e).CallMethod(env, "setType", jnigi.Void, (*jnigi.ObjectRef)(t))
}

func (c *CallExpression) SetName(env *jnigi.Env, s string) {
	(*Node)(c).SetName(env, s)
}

func (c *CallExpression) AddArgument(env *jnigi.Env, e *Expression) {
	(*jnigi.ObjectRef)(c).CallMethod(env, "addArgument", jnigi.Void, (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
}

func (l *Literal) SetType(env *jnigi.Env, t *Type) {
	(*Expression)(l).SetType(env, t)
}

func (l *Literal) SetValue(env *jnigi.Env, value interface{}) {
	object, ok := value.(*jnigi.ObjectRef)

	// need to convert it to object since its a generic, which types is erased at runtime
	if ok {
		value = object.Cast("java/lang/Object")
	}

	// basic types should be just fine, i guess?

	(*jnigi.ObjectRef)(l).SetField(env, "value", value)
}

func (r *DeclaredReferenceExpression) SetName(env *jnigi.Env, s string) {
	(*Node)(r).SetName(env, s)
}
