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

func (f *FunctionDeclaration) AddParameter(p *ParamVariableDeclaration) {
	(*jnigi.ObjectRef)(f).CallMethod(env, "addParameter", jnigi.Void, (*jnigi.ObjectRef)(p))
}

func (f *FunctionDeclaration) SetBody(s *Statement) (err error) {
	_, err = (*jnigi.ObjectRef)(f).CallMethod(env, "setBody", jnigi.Void, (*jnigi.ObjectRef)(s).Cast("de/fraunhofer/aisec/cpg/graph/statements/Statement"))

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
	o, err := (*jnigi.ObjectRef)(m).GetField(env, "receiver", jnigi.ObjectType("de/fraunhofer/aisec/cpg/graph/declarations/VariableDeclaration"))

	if err != nil {
		log.Fatal(err)
		debug.PrintStack()
	}

	return (*VariableDeclaration)(o.(*jnigi.ObjectRef))
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
	_, err = (*jnigi.ObjectRef)(v).CallMethod(env, "setInitializer", jnigi.Void, (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))

	return
}

func (v *VariableDeclaration) Declaration() *Declaration {
	return (*Declaration)(v)
}

func (t *TranslationUnitDeclaration) GetIncludeByName(s string) *IncludeDeclaration {
	i, err := (*jnigi.ObjectRef)(t).CallMethod(env, "getIncludeByName", jnigi.ObjectType("de/fraunhofer/aisec/cpg/graph/declarations/IncludeDeclaration"), NewString(s))
	if err != nil {
		log.Fatal(err)
		debug.PrintStack()
	}

	return (*IncludeDeclaration)(i.(*jnigi.ObjectRef))
}

func (r *RecordDeclaration) SetName(s string) error {
	return (*Node)(r).SetName(s)
}

func (r *RecordDeclaration) SetKind(s string) error {
	return (*jnigi.ObjectRef)(r).SetField(env, "kind", NewString(s))
}

func (r *RecordDeclaration) AddMethod(m *MethodDeclaration) (err error) {
	_, err = (*jnigi.ObjectRef)(r).CallMethod(env, "addMethod", jnigi.Void, (*jnigi.ObjectRef)(m))

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

func NewTranslationUnitDeclaration(fset *token.FileSet, astNode ast.Node, name string, code string) *TranslationUnitDeclaration {
	o, err := env.CallStaticMethod("de/fraunhofer/aisec/cpg/graph/NodeBuilder", "newTranslationUnitDeclaration", jnigi.ObjectType("de/fraunhofer/aisec/cpg/graph/declarations/TranslationUnitDeclaration"), NewString(name), NewString(code))
	if err != nil {
		log.Fatal(err)
		debug.PrintStack()
	}

	return (*TranslationUnitDeclaration)(o.(*jnigi.ObjectRef))
}

func NewNamespaceDeclaration(fset *token.FileSet, astNode ast.Node, name string, code string) *NamespaceDeclaration {
	o, err := env.CallStaticMethod("de/fraunhofer/aisec/cpg/graph/NodeBuilder", "newNamespaceDeclaration", jnigi.ObjectType("de/fraunhofer/aisec/cpg/graph/declarations/NamespaceDeclaration"), NewString(name), NewString(code))
	if err != nil {
		log.Fatal(err)
		debug.PrintStack()
	}

	return (*NamespaceDeclaration)(o.(*jnigi.ObjectRef))
}

func NewIncludeDeclaration(fset *token.FileSet, astNode ast.Node) *IncludeDeclaration {
	tu, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/declarations/IncludeDeclaration")
	if err != nil {
		log.Fatal(err)
		debug.PrintStack()
	}

	updateCode(fset, (*Node)(tu), astNode)
	updateLocation(fset, (*Node)(tu), astNode)

	return (*IncludeDeclaration)(tu)
}

func NewFunctionDeclaration(fset *token.FileSet, astNode ast.Node) *FunctionDeclaration {
	tu, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/declarations/FunctionDeclaration")
	if err != nil {
		log.Fatal(err)
		debug.PrintStack()
	}

	updateCode(fset, (*Node)(tu), astNode)
	updateLocation(fset, (*Node)(tu), astNode)

	return (*FunctionDeclaration)(tu)
}

func NewMethodDeclaration(fset *token.FileSet, astNode ast.Node) *MethodDeclaration {
	tu, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/declarations/MethodDeclaration")
	if err != nil {
		log.Fatal(err)
		debug.PrintStack()
	}

	updateCode(fset, (*Node)(tu), astNode)
	updateLocation(fset, (*Node)(tu), astNode)

	return (*MethodDeclaration)(tu)
}

func NewRecordDeclaration(fset *token.FileSet, astNode ast.Node) *RecordDeclaration {
	tu, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/declarations/RecordDeclaration")
	if err != nil {
		log.Fatal(err)
		debug.PrintStack()
	}

	updateCode(fset, (*Node)(tu), astNode)
	updateLocation(fset, (*Node)(tu), astNode)

	return (*RecordDeclaration)(tu)
}

func NewVariableDeclaration(fset *token.FileSet, astNode ast.Node) *VariableDeclaration {
	tu, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/declarations/VariableDeclaration")
	if err != nil {
		log.Fatal(err)
		debug.PrintStack()
	}

	updateCode(fset, (*Node)(tu), astNode)
	updateLocation(fset, (*Node)(tu), astNode)

	return (*VariableDeclaration)(tu)
}

func NewParamVariableDeclaration(fset *token.FileSet, astNode ast.Node) *ParamVariableDeclaration {
	tu, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/declarations/ParamVariableDeclaration")
	if err != nil {
		log.Fatal(err)
		debug.PrintStack()
	}

	updateCode(fset, (*Node)(tu), astNode)
	updateLocation(fset, (*Node)(tu), astNode)

	return (*ParamVariableDeclaration)(tu)
}

func NewFieldDeclaration(fset *token.FileSet, astNode ast.Node) *FieldDeclaration {
	tu, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/declarations/FieldDeclaration")
	if err != nil {
		log.Fatal(err)
		debug.PrintStack()
	}

	updateCode(fset, (*Node)(tu), astNode)
	updateLocation(fset, (*Node)(tu), astNode)

	return (*FieldDeclaration)(tu)
}
