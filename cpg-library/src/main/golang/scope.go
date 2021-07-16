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
	(*jnigi.ObjectRef)(s).CallMethod(env, "enterScope", jnigi.Void, (*jnigi.ObjectRef)(n).Cast("de/fraunhofer/aisec/cpg/graph/Node"))
}

func (s *ScopeManager) LeaveScope(n *Node) (err error) {
	_, err = (*jnigi.ObjectRef)(s).CallMethod(env, "leaveScope", jnigi.ObjectType("de/fraunhofer/aisec/cpg/passes/scopes/Scope"), (*jnigi.ObjectRef)(n).Cast("de/fraunhofer/aisec/cpg/graph/Node"))

	return err
}

func (s *ScopeManager) ResetToGlobal(n *Node) {
	(*jnigi.ObjectRef)(s).CallMethod(env, "resetToGlobal", jnigi.Void, (*jnigi.ObjectRef)(n).Cast("de/fraunhofer/aisec/cpg/graph/declarations/TranslationUnitDeclaration"))
}

func (s *ScopeManager) GetCurrentScope() *Scope {
	o, _ := (*jnigi.ObjectRef)(s).CallMethod(env, "getCurrentScope", jnigi.ObjectType("de/fraunhofer/aisec/cpg/passes/scopes/Scope"))

	return (*Scope)(o.(*jnigi.ObjectRef))
}

func (s *ScopeManager) GetCurrentFunction() *FunctionDeclaration {
	o, _ := (*jnigi.ObjectRef)(s).CallMethod(env, "getCurrentFunction", jnigi.ObjectType("de/fraunhofer/aisec/cpg/graph/declarations/FunctionDeclaration"))

	return (*FunctionDeclaration)(o.(*jnigi.ObjectRef))
}

func (s *ScopeManager) GetCurrentBlock() *CompoundStatement {
	o, _ := (*jnigi.ObjectRef)(s).CallMethod(env, "getCurrentBlock", jnigi.ObjectType("de/fraunhofer/aisec/cpg/graph/statements/CompoundStatement"))

	return (*CompoundStatement)(o.(*jnigi.ObjectRef))
}

func (s *ScopeManager) GetRecordForName(scope *Scope, recordName string) (record *RecordDeclaration, err error) {
	var o interface{}

	o, err = (*jnigi.ObjectRef)(s).CallMethod(env,
		"getRecordForName",
		jnigi.ObjectType("de/fraunhofer/aisec/cpg/graph/declarations/RecordDeclaration"),
		(*jnigi.ObjectRef)(scope).Cast("de/fraunhofer/aisec/cpg/passes/scopes/Scope"),
		NewString(recordName))

	record = (*RecordDeclaration)(o.(*jnigi.ObjectRef))

	return
}

func (s *ScopeManager) AddDeclaration(d *Declaration) (err error) {
	_, err = (*jnigi.ObjectRef)(s).CallMethod(env, "addDeclaration", jnigi.Void, (*jnigi.ObjectRef)(d).Cast("de/fraunhofer/aisec/cpg/graph/declarations/Declaration"))

	return
}
