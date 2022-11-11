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
	"runtime/debug"

	"tekao.net/jnigi"
)

type Declaration jnigi.ObjectRef
type IncludeDeclaration jnigi.ObjectRef
type TranslationUnitDeclaration Declaration
type FunctionDeclaration Declaration
type MethodDeclaration FunctionDeclaration
type RecordDeclaration Declaration
type FieldDeclaration Declaration
type VariableDeclaration Declaration
type ParamVariableDeclaration Declaration
type NamespaceDeclaration Declaration

func (n *NamespaceDeclaration) SetName(s string) error {
	return (*Node)(n).SetName(s)
}

func (n *IncludeDeclaration) SetName(s string) error {
	return (*Node)(n).SetName(s)
}

func (n *IncludeDeclaration) SetFilename(s string) error {
	return (*jnigi.ObjectRef)(n).SetField(env, "filename", NewString(s))
}

func (f *FunctionDeclaration) SetName(s string) error {
	return (*Node)(f).SetName(s)
}

func (f *FunctionDeclaration) SetType(t *Type) {
	(*HasType)(f).SetType(t)
}

func (f *FunctionDeclaration) SetReturnTypes(types []*Type) (err error) {
	var list *jnigi.ObjectRef

	list, err = ListOf[*Type](types)
	if err != nil {
		return err
	}

	// Stupid workaround, since casting does not work. See
	// https://github.com/timob/jnigi/issues/60
	var funcDecl = jnigi.WrapJObject(uintptr((*jnigi.ObjectRef)(f).JObject()), "de/fraunhofer/aisec/cpg/graph/declarations/FunctionDeclaration", false)

	err = (*jnigi.ObjectRef)(funcDecl).CallMethod(env, "setReturnTypes", nil, list.Cast("java/util/List"))

	return
}

func (f *FunctionDeclaration) AddParameter(p *ParamVariableDeclaration) {
	(*jnigi.ObjectRef)(f).CallMethod(env, "addParameter", nil, (*jnigi.ObjectRef)(p))
}

func (f *FunctionDeclaration) SetBody(s *Statement) (err error) {
	err = (*jnigi.ObjectRef)(f).CallMethod(env, "setBody", nil, (*jnigi.ObjectRef)(s).Cast("de/fraunhofer/aisec/cpg/graph/statements/Statement"))

	return
}

func (m *MethodDeclaration) SetName(s string) error {
	return (*Node)(m).SetName(s)
}

func (m *MethodDeclaration) SetType(t *Type) {
	(*HasType)(m).SetType(t)
}

func (m *MethodDeclaration) SetReceiver(v *VariableDeclaration) error {
	return (*jnigi.ObjectRef)(m).SetField(env, "receiver", (*jnigi.ObjectRef)(v))
}

func (m *MethodDeclaration) GetReceiver() *VariableDeclaration {
	o := jnigi.NewObjectRef("de/fraunhofer/aisec/cpg/graph/declarations/VariableDeclaration")
	err := (*jnigi.ObjectRef)(m).GetField(env, "receiver", o)

	if err != nil {
		log.Fatal(err)
		debug.PrintStack()
	}

	return (*VariableDeclaration)(o)
}

func (p *ParamVariableDeclaration) SetType(t *Type) {
	(*HasType)(p).SetType(t)
}

func (p *ParamVariableDeclaration) SetName(s string) error {
	return (*Node)(p).SetName(s)
}

func (f *FieldDeclaration) SetName(s string) error {
	return (*Node)(f).SetName(s)
}

func (f *FieldDeclaration) SetType(t *Type) {
	(*HasType)(f).SetType(t)
}

func (v *VariableDeclaration) SetType(t *Type) {
	(*HasType)(v).SetType(t)
}

func (v *VariableDeclaration) SetName(s string) error {
	return (*Node)(v).SetName(s)
}

func (v *VariableDeclaration) IsNil() bool {
	return (*jnigi.ObjectRef)(v).IsNil()
}

func (v *VariableDeclaration) SetInitializer(e *Expression) (err error) {
	err = (*jnigi.ObjectRef)(v).CallMethod(env, "setInitializer", nil, (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))

	return
}

func (v *VariableDeclaration) Declaration() *Declaration {
	return (*Declaration)(v)
}

func (t *TranslationUnitDeclaration) GetIncludeByName(s string) *IncludeDeclaration {
	var i = jnigi.NewObjectRef("de/fraunhofer/aisec/cpg/graph/declarations/IncludeDeclaration")
	err := (*jnigi.ObjectRef)(t).CallMethod(env, "getIncludeByName", i, NewString(s))
	if err != nil {
		log.Fatal(err)
		debug.PrintStack()
	}

	return (*IncludeDeclaration)(i)
}

func (r *RecordDeclaration) SetName(s string) error {
	return (*Node)(r).SetName(s)
}

func (r *RecordDeclaration) SetKind(s string) error {
	return (*jnigi.ObjectRef)(r).SetField(env, "kind", NewString(s))
}

func (r *RecordDeclaration) AddMethod(m *MethodDeclaration) (err error) {
	err = (*jnigi.ObjectRef)(r).CallMethod(env, "addMethod", nil, (*jnigi.ObjectRef)(m))

	return
}

func (r *RecordDeclaration) AddSuperClass(t *Type) (err error) {
	(*jnigi.ObjectRef)(r).CallMethod(env, "addSuperClass", nil, t)

	return
}

func (r *RecordDeclaration) IsNil() bool {
	return (*jnigi.ObjectRef)(r).IsNil()
}

func (r *MethodDeclaration) IsNil() bool {
	return (*jnigi.ObjectRef)(r).IsNil()
}

func (r *CompoundStatement) IsNil() bool {
	return (*jnigi.ObjectRef)(r).IsNil()
}

func (c *CaseStatement) SetCaseExpression(e *Expression) error {
	return (*jnigi.ObjectRef)(c).SetField(env, "caseExpression", (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
}
