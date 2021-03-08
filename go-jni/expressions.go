package cpg

import (
	"go/ast"
	"go/token"
	"log"

	"tekao.net/jnigi"
)

type Expression Statement
type CallExpression Expression
type MemberCallExpression CallExpression
type BinaryOperator Expression
type Literal Expression
type DeclaredReferenceExpression Expression

func NewCallExpression(fset *token.FileSet, env *jnigi.Env, astNode ast.Node) *CallExpression {
	c, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/expressions/CallExpression")
	if err != nil {
		log.Fatal(err)
	}

	updateCode(fset, env, (*Node)(c), astNode)

	return (*CallExpression)(c)
}

func NewMemberCallExpression(fset *token.FileSet, env *jnigi.Env, astNode ast.Node) *MemberCallExpression {
	c, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/expressions/MemberCallExpression")
	if err != nil {
		log.Fatal(err)
	}

	updateCode(fset, env, (*Node)(c), astNode)

	return (*MemberCallExpression)(c)
}

func NewBinaryOperator(fset *token.FileSet, env *jnigi.Env, astNode ast.Node) *BinaryOperator {
	c, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/expressions/BinaryOperator")
	if err != nil {
		log.Fatal(err)
	}

	updateCode(fset, env, (*Node)(c), astNode)

	return (*BinaryOperator)(c)
}

func NewLiteral(fset *token.FileSet, env *jnigi.Env, astNode ast.Node) *Literal {
	l, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/expressions/Literal")
	if err != nil {
		log.Fatal(err)
	}

	updateCode(fset, env, (*Node)(l), astNode)

	return (*Literal)(l)
}

func NewDeclaredReferenceExpression(fset *token.FileSet, env *jnigi.Env, astNode ast.Node) *DeclaredReferenceExpression {
	l, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/expressions/DeclaredReferenceExpression")
	if err != nil {
		log.Fatal(err)
	}

	updateCode(fset, env, (*Node)(l), astNode)

	return (*DeclaredReferenceExpression)(l)
}

func (e *Expression) SetType(env *jnigi.Env, t *Type) {
	(*HasType)(e).SetType(env, t)
	//(*jnigi.ObjectRef)(e).CallMethod(env, "setType", jnigi.Void, (*jnigi.ObjectRef)(t))
}

func (c *CallExpression) SetName(env *jnigi.Env, s string) {
	(*Node)(c).SetName(env, s)
}

func (c *CallExpression) SetFqn(env *jnigi.Env, s string) {
	(*jnigi.ObjectRef)(c).SetField(env, "fqn", NewString(env, s))
}

func (c *MemberCallExpression) SetName(env *jnigi.Env, s string) {
	(*Node)(c).SetName(env, s)
}

func (c *MemberCallExpression) SetFqn(env *jnigi.Env, s string) {
	(*CallExpression)(c).SetFqn(env, s)
}

func (m *MemberCallExpression) SetBase(env *jnigi.Env, n *Node) {
	(*jnigi.ObjectRef)(m).SetField(env, "base", (*jnigi.ObjectRef)(n).Cast("de/fraunhofer/aisec/cpg/graph/Node"))
}

func (m *MemberCallExpression) SetMember(env *jnigi.Env, n *Node) {
	(*jnigi.ObjectRef)(m).SetField(env, "member", (*jnigi.ObjectRef)(n).Cast("de/fraunhofer/aisec/cpg/graph/Node"))
}

func (m *MemberCallExpression) Expression() *Expression {
	return (*Expression)(m)
}

func (r *DeclaredReferenceExpression) Expression() *Expression {
	return (*Expression)(r)
}

func (r *DeclaredReferenceExpression) Node() *Node {
	return (*Node)(r)
}

func (c *CallExpression) AddArgument(env *jnigi.Env, e *Expression) {
	(*jnigi.ObjectRef)(c).CallMethod(env, "addArgument", jnigi.Void, (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
}

func (b *BinaryOperator) SetLHS(env *jnigi.Env, e *Expression) {
	(*jnigi.ObjectRef)(b).CallMethod(env, "setLhs", jnigi.Void, (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
}

func (b *BinaryOperator) SetRHS(env *jnigi.Env, e *Expression) {
	(*jnigi.ObjectRef)(b).CallMethod(env, "setRhs", jnigi.Void, (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
}

func (b *BinaryOperator) SetOperatorCode(env *jnigi.Env, s string) (err error) {
	return (*jnigi.ObjectRef)(b).SetField(env, "operatorCode", NewString(env, s))
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

func (r *DeclaredReferenceExpression) SetRefersTo(env *jnigi.Env, d *Declaration) {
	(*jnigi.ObjectRef)(r).CallMethod(env, "setRefersTo", jnigi.Void, (*jnigi.ObjectRef)(d).Cast("de/fraunhofer/aisec/cpg/graph/declarations/Declaration"))
}
