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
type ForEachStatement Statement

const StatementsPackage = GraphPackage + "/statements"
const StatementClass = StatementsPackage + "/Statement"
const CompoundStatementClass = StatementsPackage + "/CompoundStatement"

func (f *CompoundStatement) AddStatement(s *Statement) {
	(*jnigi.ObjectRef)(f).CallMethod(env, "addStatement", nil, (*jnigi.ObjectRef)(s).Cast(StatementClass))
}

func (f *DeclarationStatement) SetSingleDeclaration(d *Declaration) {
	(*jnigi.ObjectRef)(f).CallMethod(env, "setSingleDeclaration", nil, (*jnigi.ObjectRef)(d).Cast(DeclarationClass))
}

func (f *DeclarationStatement) AddToPropertyEdgeDeclaration(d *Declaration) {
	(*jnigi.ObjectRef)(f).CallMethod(env, "addToPropertyEdgeDeclaration", nil, (*jnigi.ObjectRef)(d).Cast(DeclarationClass))
}

func (m *IfStatement) SetThenStatement(s *Statement) {
	(*jnigi.ObjectRef)(m).SetField(env, "thenStatement", (*jnigi.ObjectRef)(s).Cast(StatementClass))
}

func (m *IfStatement) SetElseStatement(s *Statement) {
	(*jnigi.ObjectRef)(m).SetField(env, "elseStatement", (*jnigi.ObjectRef)(s).Cast(StatementClass))
}

func (m *IfStatement) SetCondition(e *Expression) {
	(*jnigi.ObjectRef)(m).SetField(env, "condition", (*jnigi.ObjectRef)(e).Cast(ExpressionClass))
}

func (i *IfStatement) SetInitializerStatement(s *Statement) {
	(*jnigi.ObjectRef)(i).SetField(env, "initializerStatement", (*jnigi.ObjectRef)(s).Cast(StatementClass))
}

func (s *SwitchStatement) SetCondition(e *Expression) {
	(*jnigi.ObjectRef)(s).SetField(env, "selector", (*jnigi.ObjectRef)(e).Cast(ExpressionClass))
}

func (sw *SwitchStatement) SetStatement(s *Statement) {
	(*jnigi.ObjectRef)(sw).SetField(env, "statement", (*jnigi.ObjectRef)(s).Cast(StatementClass))
}

func (sw *SwitchStatement) SetInitializerStatement(s *Statement) {
	(*jnigi.ObjectRef)(sw).SetField(env, "initializerStatement", (*jnigi.ObjectRef)(s).Cast(StatementClass))
}

func (fw *ForStatement) SetInitializerStatement(s *Statement) {
	(*jnigi.ObjectRef)(fw).SetField(env, "initializerStatement", (*jnigi.ObjectRef)(s).Cast(StatementClass))
}

func (fw *ForStatement) SetCondition(e *Expression) {
	(*jnigi.ObjectRef)(fw).SetField(env, "condition", (*jnigi.ObjectRef)(e).Cast(ExpressionClass))
}

func (fw *ForStatement) SetStatement(s *Statement) {
	(*jnigi.ObjectRef)(fw).SetField(env, "statement", (*jnigi.ObjectRef)(s).Cast(StatementClass))
}

func (fw *ForEachStatement) SetStatement(s *Statement) {
	(*jnigi.ObjectRef)(fw).SetField(env, "statement", (*jnigi.ObjectRef)(s).Cast(StatementClass))
}

func (fw *ForEachStatement) SetIterable(s *Statement) {
	(*jnigi.ObjectRef)(fw).CallMethod(env, "setIterable", nil, (*jnigi.ObjectRef)(s).Cast(StatementClass))
}

func (fw *ForEachStatement) SetVariable(s *Statement) {
	(*jnigi.ObjectRef)(fw).SetField(env, "variable", (*jnigi.ObjectRef)(s).Cast(StatementClass))
}

func (fw *ForStatement) SetIterationStatement(s *Statement) {
	(*jnigi.ObjectRef)(fw).SetField(env, "iterationStatement", (*jnigi.ObjectRef)(s).Cast(StatementClass))
}

func (r *ReturnStatement) SetReturnValue(e *Expression) {
	(*jnigi.ObjectRef)(r).CallMethod(env, "setReturnValue", nil, (*jnigi.ObjectRef)(e).Cast(ExpressionClass))
}
