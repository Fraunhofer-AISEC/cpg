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
	"log"

	"tekao.net/jnigi"
)

type Expression Statement

func (e *Expression) ConvertToGo(o *jnigi.ObjectRef) error {
	*e = (Expression)(*o)
	return nil
}

func (e *Expression) GetClassName() string {
	return "de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"
}

func (e *Expression) IsArray() bool {
	return false
}

type CallExpression Expression
type CastExpression Expression
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

func (e *Expression) SetType(t *Type) {
	(*HasType)(e).SetType(t)
}

func (c *CallExpression) SetName(s string) {
	(*Node)(c).SetName(s)
}

func (c *CallExpression) SetFqn(s string) {
	(*jnigi.ObjectRef)(c).SetField(env, "fqn", NewString(s))
}

func (c *CastExpression) SetExpression(e *Expression) {
	(*jnigi.ObjectRef)(c).CallMethod(env, "setExpression", nil, (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
}

func (c *CastExpression) SetCastType(t *Type) {
	(*jnigi.ObjectRef)(c).CallMethod(env, "setCastType", nil, t)
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
	var expr Expression
	err := (*jnigi.ObjectRef)(m).GetField(env, "base", &expr)
	if err != nil {
		log.Fatal(err)
	}

	return &expr
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
	(*jnigi.ObjectRef)(c).CallMethod(env, "addArgument", nil, (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
}

func (b *BinaryOperator) SetLHS(e *Expression) {
	(*jnigi.ObjectRef)(b).CallMethod(env, "setLhs", nil, (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
}

func (b *BinaryOperator) SetRHS(e *Expression) {
	(*jnigi.ObjectRef)(b).CallMethod(env, "setRhs", nil, (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
}

func (b *BinaryOperator) SetOperatorCode(s string) (err error) {
	return (*jnigi.ObjectRef)(b).SetField(env, "operatorCode", NewString(s))
}

func (u *UnaryOperator) SetInput(e *Expression) {
	(*jnigi.ObjectRef)(u).CallMethod(env, "setInput", nil, (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
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
	(*jnigi.ObjectRef)(r).CallMethod(env, "setRefersTo", nil, (*jnigi.ObjectRef)(d).Cast("de/fraunhofer/aisec/cpg/graph/declarations/Declaration"))
}

func (r *ArrayCreationExpression) AddDimension(e *Expression) {
	(*jnigi.ObjectRef)(r).CallMethod(env, "addDimension", nil, (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
}

func (r *ArraySubscriptionExpression) SetArrayExpression(e *Expression) {
	(*jnigi.ObjectRef)(r).CallMethod(env, "setArrayExpression", nil, (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
}

func (r *ArraySubscriptionExpression) SetSubscriptExpression(e *Expression) {
	(*jnigi.ObjectRef)(r).CallMethod(env, "setSubscriptExpression", nil, (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
}

func (c *ConstructExpression) AddArgument(e *Expression) {
	(*jnigi.ObjectRef)(c).CallMethod(env, "addArgument", nil, (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
}

func (c *ConstructExpression) AddPrevDFG(n *Node) {
	(*jnigi.ObjectRef)(c).CallMethod(env, "addPrevDFG", nil, (*jnigi.ObjectRef)(n).Cast("de/fraunhofer/aisec/cpg/graph/Node"))
}

func (n *NewExpression) SetInitializer(e *Expression) (err error) {
	err = (*jnigi.ObjectRef)(n).CallMethod(env, "setInitializer", nil, (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))

	return
}

func (c *InitializerListExpression) AddInitializer(e *Expression) {
	(*jnigi.ObjectRef)(c).CallMethod(env, "addInitializer", nil, (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
}

func (k *KeyValueExpression) SetKey(e *Expression) {
	(*jnigi.ObjectRef)(k).CallMethod(env, "setKey", nil, (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
}

func (k *KeyValueExpression) SetValue(e *Expression) {
	(*jnigi.ObjectRef)(k).CallMethod(env, "setValue", nil, (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
}
