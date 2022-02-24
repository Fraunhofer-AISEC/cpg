/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *                    $$$$$$\  $$$$$$$\   $$$$$$\
 *                   $$  __$$\ $$  __$$\ $$  __$$\
 *                   $$ /  \__|$$ |  $$ |$$ /  \__|
 *                   $$ |      $$$$$$$  |$$ |$$$$\
 *                   $$ |      $$  ____/ $$ |\_$$ |
 *                   $$ |  $$\ $$ |      $$ |  $$ |
 *                   \$$$$$   |$$ |      \$$$$$   |
 *                    \______/ \__|       \______/
 *
 */
package cpg

import (
	"go/ast"
	"go/token"
	"log"

	"tekao.net/jnigi"
)

type Expression Statement
type CallExpression Expression
type NewExpression Expression
type ArrayCreationExpression Expression
type ArraySubscriptionExpression Expression
type ConstructExpression Expression
type InitializerListExpression Expression
type MemberCallExpression CallExpression
type MemberExpression Expression
type BinaryOperator Expression
type UnaryOperator Expression
type Literal Expression
type DeclaredReferenceExpression Expression
type KeyValueExpression Expression

func NewCallExpression(fset *token.FileSet, astNode ast.Node) *CallExpression {
	c, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/expressions/CallExpression")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*Node)(c), astNode)
	updateLocation(fset, (*Node)(c), astNode)

	return (*CallExpression)(c)
}

func NewMemberExpression(fset *token.FileSet, astNode ast.Node) *MemberExpression {
	c, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/expressions/MemberExpression")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*Node)(c), astNode)
	updateLocation(fset, (*Node)(c), astNode)

	return (*MemberExpression)(c)
}

func NewMemberCallExpression(fset *token.FileSet, astNode ast.Node) *MemberCallExpression {
	c, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/expressions/MemberCallExpression")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*Node)(c), astNode)
	updateLocation(fset, (*Node)(c), astNode)

	return (*MemberCallExpression)(c)
}

func NewNewExpression(fset *token.FileSet, astNode ast.Node) *NewExpression {
	c, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/expressions/NewExpression")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*Node)(c), astNode)
	updateLocation(fset, (*Node)(c), astNode)

	return (*NewExpression)(c)
}

func NewArrayCreationExpression(fset *token.FileSet, astNode ast.Node) *ArrayCreationExpression {
	c, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/expressions/ArrayCreationExpression")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*Node)(c), astNode)
	updateLocation(fset, (*Node)(c), astNode)

	return (*ArrayCreationExpression)(c)
}

func NewArraySubscriptionExpression(fset *token.FileSet, astNode ast.Node) *ArraySubscriptionExpression {
	c, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/expressions/ArraySubscriptionExpression")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*Node)(c), astNode)
	updateLocation(fset, (*Node)(c), astNode)

	return (*ArraySubscriptionExpression)(c)
}

func NewConstructExpression(fset *token.FileSet, astNode ast.Node) *ConstructExpression {
	c, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/expressions/ConstructExpression")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*Node)(c), astNode)
	updateLocation(fset, (*Node)(c), astNode)

	return (*ConstructExpression)(c)
}

func NewInitializerListExpression(fset *token.FileSet, astNode ast.Node) *InitializerListExpression {
	l, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/expressions/InitializerListExpression")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*Node)(l), astNode)
	updateLocation(fset, (*Node)(l), astNode)

	return (*InitializerListExpression)(l)
}

func NewBinaryOperator(fset *token.FileSet, astNode ast.Node) *BinaryOperator {
	c, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/expressions/BinaryOperator")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*Node)(c), astNode)
	updateLocation(fset, (*Node)(c), astNode)

	return (*BinaryOperator)(c)
}

func NewUnaryOperator(fset *token.FileSet, astNode ast.Node) *UnaryOperator {
	c, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/expressions/UnaryOperator")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*Node)(c), astNode)
	updateLocation(fset, (*Node)(c), astNode)

	return (*UnaryOperator)(c)
}

func NewLiteral(fset *token.FileSet, astNode ast.Node) *Literal {
	l, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/expressions/Literal")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*Node)(l), astNode)
	updateLocation(fset, (*Node)(l), astNode)

	return (*Literal)(l)
}

func NewDeclaredReferenceExpression(fset *token.FileSet, astNode ast.Node) *DeclaredReferenceExpression {
	l, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/expressions/DeclaredReferenceExpression")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*Node)(l), astNode)
	updateLocation(fset, (*Node)(l), astNode)

	return (*DeclaredReferenceExpression)(l)
}

func NewKeyValueExpression(fset *token.FileSet, astNode ast.Node) *KeyValueExpression {
	k, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/expressions/KeyValueExpression")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*Node)(k), astNode)
	updateLocation(fset, (*Node)(k), astNode)

	return (*KeyValueExpression)(k)
}

func (e *Expression) SetType(t *Type) {
	(*HasType)(e).SetType(t)
}

func (c *CallExpression) SetName(s string) {
	(*Node)(c).SetName(s)
}

func (c *CallExpression) SetFqn(s string) {
	(*jnigi.ObjectRef)(c).SetField(env, "fqn", NewString(s))
}

func (c *MemberCallExpression) SetName(s string) {
	(*Node)(c).SetName(s)
}

func (c *MemberCallExpression) SetFqn(s string) {
	(*CallExpression)(c).SetFqn(s)
}

func (m *MemberCallExpression) SetBase(e *Expression) {
	(*jnigi.ObjectRef)(m).SetField(env, "base", (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
}

func (m *MemberCallExpression) SetMember(n *Node) {
	(*jnigi.ObjectRef)(m).SetField(env, "member", (*jnigi.ObjectRef)(n).Cast("de/fraunhofer/aisec/cpg/graph/Node"))
}

func (m *MemberCallExpression) Expression() *Expression {
	return (*Expression)(m)
}

func (m *MemberExpression) SetBase(e *Expression) {
	(*jnigi.ObjectRef)(m).SetField(env, "base", (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
}

func (m *MemberExpression) GetBase() *Expression {
	i, err := (*jnigi.ObjectRef)(m).GetField(env, "base", jnigi.ObjectType("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
	if err != nil {
		log.Fatal(err)
	}

	return (*Expression)(i.(*jnigi.ObjectRef))
}

func (e *Expression) GetName() string {
	return (*Node)(e).GetName()
}

func (r *DeclaredReferenceExpression) Expression() *Expression {
	return (*Expression)(r)
}

func (r *DeclaredReferenceExpression) Node() *Node {
	return (*Node)(r)
}

func (c *CallExpression) AddArgument(e *Expression) {
	(*jnigi.ObjectRef)(c).CallMethod(env, "addArgument", jnigi.Void, (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
}

func (b *BinaryOperator) SetLHS(e *Expression) {
	(*jnigi.ObjectRef)(b).CallMethod(env, "setLhs", jnigi.Void, (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
}

func (b *BinaryOperator) SetRHS(e *Expression) {
	(*jnigi.ObjectRef)(b).CallMethod(env, "setRhs", jnigi.Void, (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
}

func (b *BinaryOperator) SetOperatorCode(s string) (err error) {
	return (*jnigi.ObjectRef)(b).SetField(env, "operatorCode", NewString(s))
}

func (u *UnaryOperator) SetInput(e *Expression) {
	(*jnigi.ObjectRef)(u).CallMethod(env, "setInput", jnigi.Void, (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
}

func (u *UnaryOperator) SetOperatorCode(s string) (err error) {
	return (*jnigi.ObjectRef)(u).SetField(env, "operatorCode", NewString(s))
}

func (l *Literal) SetType(t *Type) {
	(*Expression)(l).SetType(t)
}

func (l *Literal) SetValue(value interface{}) {
	object, ok := value.(*jnigi.ObjectRef)

	// need to convert it to object since its a generic, which types is erased at runtime
	if ok {
		value = object.Cast("java/lang/Object")
	}

	// basic types should be just fine, i guess?

	(*jnigi.ObjectRef)(l).SetField(env, "value", value)
}

func (r *DeclaredReferenceExpression) SetName(s string) {
	(*Node)(r).SetName(s)
}

func (r *DeclaredReferenceExpression) SetRefersTo(d *Declaration) {
	(*jnigi.ObjectRef)(r).CallMethod(env, "setRefersTo", jnigi.Void, (*jnigi.ObjectRef)(d).Cast("de/fraunhofer/aisec/cpg/graph/declarations/Declaration"))
}

func (r *ArrayCreationExpression) AddDimension(e *Expression) {
	(*jnigi.ObjectRef)(r).CallMethod(env, "addDimension", jnigi.Void, (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
}

func (r *ArraySubscriptionExpression) SetArrayExpression(e *Expression) {
	(*jnigi.ObjectRef)(r).CallMethod(env, "setArrayExpression", jnigi.Void, (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
}

func (r *ArraySubscriptionExpression) SetSubscriptExpression(e *Expression) {
	(*jnigi.ObjectRef)(r).CallMethod(env, "setSubscriptExpression", jnigi.Void, (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
}

func (c *ConstructExpression) AddArgument(e *Expression) {
	(*jnigi.ObjectRef)(c).CallMethod(env, "addArgument", jnigi.Void, (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
}

func (n *NewExpression) SetInitializer(e *Expression) (err error) {
	_, err = (*jnigi.ObjectRef)(n).CallMethod(env, "setInitializer", jnigi.Void, (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))

	return
}

func (c *InitializerListExpression) AddInitializer(e *Expression) {
	(*jnigi.ObjectRef)(c).CallMethod(env, "addInitializer", jnigi.Void, (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
}

func (k *KeyValueExpression) SetKey(e *Expression) {
	(*jnigi.ObjectRef)(k).CallMethod(env, "setKey", jnigi.Void, (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
}

func (k *KeyValueExpression) SetValue(e *Expression) {
	(*jnigi.ObjectRef)(k).CallMethod(env, "setValue", jnigi.Void, (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
}
