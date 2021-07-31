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

type Statement Node
type CompoundStatement Statement
type ReturnStatement Statement
type DeclarationStatement Statement
type IfStatement Statement
type SwitchStatement Statement
type CaseStatement Statement
type DefaultStatement Statement
type ForStatement Statement

func NewCompoundStatement(fset *token.FileSet, astNode ast.Node) *CompoundStatement {
	s, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/CompoundStatement")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*Node)(s), astNode)
	updateLocation(fset, (*Node)(s), astNode)

	return (*CompoundStatement)(s)
}

func NewReturnStatement(fset *token.FileSet, astNode ast.Node) *ReturnStatement {
	s, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/ReturnStatement")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*Node)(s), astNode)
	updateLocation(fset, (*Node)(s), astNode)

	return (*ReturnStatement)(s)
}

func NewDeclarationStatement(fset *token.FileSet, astNode ast.Node) *DeclarationStatement {
	s, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/DeclarationStatement")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*Node)(s), astNode)
	updateLocation(fset, (*Node)(s), astNode)

	return (*DeclarationStatement)(s)
}

func NewIfStatement(fset *token.FileSet, astNode ast.Node) *IfStatement {
	s, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/IfStatement")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*Node)(s), astNode)
	updateLocation(fset, (*Node)(s), astNode)

	return (*IfStatement)(s)
}

func NewForStatement(fset *token.FileSet, astNode ast.Node) *ForStatement {
	s, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/ForStatement")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*Node)(s), astNode)
	updateLocation(fset, (*Node)(s), astNode)

	return (*ForStatement)(s)
}

func NewSwitchStatement(fset *token.FileSet, astNode ast.Node) *SwitchStatement {
	s, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/SwitchStatement")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*Node)(s), astNode)
	updateLocation(fset, (*Node)(s), astNode)

	return (*SwitchStatement)(s)
}

func NewCaseStatement(fset *token.FileSet, astNode ast.Node) *CaseStatement {
	s, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/CaseStatement")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*Node)(s), astNode)
	updateLocation(fset, (*Node)(s), astNode)

	return (*CaseStatement)(s)
}

func NewDefaultStatement(fset *token.FileSet, astNode ast.Node) *DefaultStatement {
	s, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/DefaultStatement")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*Node)(s), astNode)
	updateLocation(fset, (*Node)(s), astNode)

	return (*DefaultStatement)(s)
}

func (f *CompoundStatement) AddStatement(s *Statement) {
	(*jnigi.ObjectRef)(f).CallMethod(env, "addStatement", jnigi.Void, (*jnigi.ObjectRef)(s).Cast("de/fraunhofer/aisec/cpg/graph/statements/Statement"))
}

func (f *DeclarationStatement) SetSingleDeclaration(d *Declaration) {
	(*jnigi.ObjectRef)(f).CallMethod(env, "setSingleDeclaration", jnigi.Void, (*jnigi.ObjectRef)(d).Cast("de/fraunhofer/aisec/cpg/graph/declarations/Declaration"))
}

func (m *IfStatement) SetThenStatement(s *Statement) {
	(*jnigi.ObjectRef)(m).SetField(env, "thenStatement", (*jnigi.ObjectRef)(s).Cast("de/fraunhofer/aisec/cpg/graph/statements/Statement"))
}

func (m *IfStatement) SetElseStatement(s *Statement) {
	(*jnigi.ObjectRef)(m).SetField(env, "elseStatement", (*jnigi.ObjectRef)(s).Cast("de/fraunhofer/aisec/cpg/graph/statements/Statement"))
}

func (m *IfStatement) SetCondition(e *Expression) {
	(*jnigi.ObjectRef)(m).SetField(env, "condition", (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
}

func (i *IfStatement) SetInitializerStatement(s *Statement) {
	(*jnigi.ObjectRef)(i).SetField(env, "initializerStatement", (*jnigi.ObjectRef)(s).Cast("de/fraunhofer/aisec/cpg/graph/statements/Statement"))
}

func (s *SwitchStatement) SetCondition(e *Expression) {
	(*jnigi.ObjectRef)(s).SetField(env, "selector", (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
}

func (sw *SwitchStatement) SetStatement(s *Statement) {
	(*jnigi.ObjectRef)(sw).SetField(env, "statement", (*jnigi.ObjectRef)(s).Cast("de/fraunhofer/aisec/cpg/graph/statements/Statement"))
}

func (sw *SwitchStatement) SetInitializerStatement(s *Statement) {
	(*jnigi.ObjectRef)(sw).SetField(env, "initializerStatement", (*jnigi.ObjectRef)(s).Cast("de/fraunhofer/aisec/cpg/graph/statements/Statement"))
}

func (fw *ForStatement) SetInitializerStatement(s *Statement) {
	(*jnigi.ObjectRef)(fw).SetField(env, "initializerStatement", (*jnigi.ObjectRef)(s).Cast("de/fraunhofer/aisec/cpg/graph/statements/Statement"))
}

func (fw *ForStatement) SetCondition(e *Expression) {
	(*jnigi.ObjectRef)(fw).SetField(env, "condition", (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
}

func (fw *ForStatement) SetStatement(s *Statement) {
	(*jnigi.ObjectRef)(fw).SetField(env, "statement", (*jnigi.ObjectRef)(s).Cast("de/fraunhofer/aisec/cpg/graph/statements/Statement"))
}

func (fw *ForStatement) SetIterationStatement(s *Statement) {
	(*jnigi.ObjectRef)(fw).SetField(env, "iterationStatement", (*jnigi.ObjectRef)(s).Cast("de/fraunhofer/aisec/cpg/graph/statements/Statement"))
}

func (r *ReturnStatement) SetReturnValue(e *Expression) {
	(*jnigi.ObjectRef)(r).CallMethod(env, "setReturnValue", jnigi.Void, (*jnigi.ObjectRef)(e).Cast("de/fraunhofer/aisec/cpg/graph/statements/expressions/Expression"))
}
