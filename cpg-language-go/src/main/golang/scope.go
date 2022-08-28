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

import "tekao.net/jnigi"

type ScopeManager jnigi.ObjectRef
type Scope jnigi.ObjectRef

func (s *ScopeManager) EnterScope(n *Node) {
	(*jnigi.ObjectRef)(s).CallMethod(env, "enterScope", nil, (*jnigi.ObjectRef)(n).Cast("de/fraunhofer/aisec/cpg/graph/Node"))
}

func (s *ScopeManager) LeaveScope(n *Node) (err error) {
	var scope = jnigi.NewObjectRef("de/fraunhofer/aisec/cpg/passes/scopes/Scope")
	err = (*jnigi.ObjectRef)(s).CallMethod(env, "leaveScope", scope, (*jnigi.ObjectRef)(n).Cast("de/fraunhofer/aisec/cpg/graph/Node"))

	return err
}

func (s *ScopeManager) ResetToGlobal(n *Node) {
	(*jnigi.ObjectRef)(s).CallMethod(env, "resetToGlobal", nil, (*jnigi.ObjectRef)(n).Cast("de/fraunhofer/aisec/cpg/graph/declarations/TranslationUnitDeclaration"))
}

func (s *ScopeManager) GetCurrentScope() *Scope {
	var o = jnigi.NewObjectRef("de/fraunhofer/aisec/cpg/passes/scopes/Scope")
	(*jnigi.ObjectRef)(s).CallMethod(env, "getCurrentScope", o)

	return (*Scope)(o)
}

func (s *ScopeManager) GetCurrentFunction() *FunctionDeclaration {
	var o = jnigi.NewObjectRef("de/fraunhofer/aisec/cpg/graph/declarations/FunctionDeclaration")
	(*jnigi.ObjectRef)(s).CallMethod(env, "getCurrentFunction", o)

	return (*FunctionDeclaration)(o)
}

func (s *ScopeManager) GetCurrentBlock() *CompoundStatement {
	var o = jnigi.NewObjectRef("de/fraunhofer/aisec/cpg/graph/statements/CompoundStatement")
	(*jnigi.ObjectRef)(s).CallMethod(env, "getCurrentBlock", o)

	return (*CompoundStatement)(o)
}

func (s *ScopeManager) GetRecordForName(scope *Scope, recordName string) (record *RecordDeclaration, err error) {
	var o = jnigi.NewObjectRef("de/fraunhofer/aisec/cpg/graph/declarations/RecordDeclaration")

	err = (*jnigi.ObjectRef)(s).CallMethod(env,
		"getRecordForName",
		o,
		(*jnigi.ObjectRef)(scope).Cast("de/fraunhofer/aisec/cpg/passes/scopes/Scope"),
		NewString(recordName))

	record = (*RecordDeclaration)(o)

	return
}

func (s *ScopeManager) AddDeclaration(d *Declaration) (err error) {
	err = (*jnigi.ObjectRef)(s).CallMethod(env, "addDeclaration", nil, (*jnigi.ObjectRef)(d).Cast("de/fraunhofer/aisec/cpg/graph/declarations/Declaration"))

	return
}
