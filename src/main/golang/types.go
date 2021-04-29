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
	"C"

	"tekao.net/jnigi"
)
import (
	"log"
)

var env *jnigi.Env

type Type jnigi.ObjectRef
type ObjectType jnigi.ObjectRef
type HasType jnigi.ObjectRef

func InitEnv(e *jnigi.Env) {
	env = e
}

func TypeParser_createFrom(s string, resolveAlias bool) *Type {
	t, err := env.CallStaticMethod("de/fraunhofer/aisec/cpg/graph/types/TypeParser", "createFrom", jnigi.ObjectType("de/fraunhofer/aisec/cpg/graph/types/Type"), NewString(s), resolveAlias)
	if err != nil {
		log.Fatal(err)

	}

	return (*Type)(t.(*jnigi.ObjectRef))
}

func UnknownType_getUnknown() *Type {
	t, err := env.CallStaticMethod("de/fraunhofer/aisec/cpg/graph/types/UnknownType", "getUnknownType", jnigi.ObjectType("de/fraunhofer/aisec/cpg/graph/types/UnknownType"))
	if err != nil {
		log.Fatal(err)

	}

	return (*Type)(t.(*jnigi.ObjectRef))
}

func (h *Type) GetRoot() *Type {
	o, err := (*jnigi.ObjectRef)(h).CallMethod(env, "getRoot", jnigi.ObjectType("de/fraunhofer/aisec/cpg/graph/types/Type"))
	if err != nil {
		log.Fatal(err)

	}

	return (*Type)(o.(*jnigi.ObjectRef))
}

func (t *Type) Reference(o *jnigi.ObjectRef) *Type {
	i, err := (*jnigi.ObjectRef)(t).CallMethod(env, "reference", jnigi.ObjectType("de/fraunhofer/aisec/cpg/graph/types/Type"), (*jnigi.ObjectRef)(o).Cast("de/fraunhofer/aisec/cpg/graph/types/PointerType$PointerOrigin"))

	if err != nil {
		log.Fatal(err)
	}

	return (*Type)(i.(*jnigi.ObjectRef))
}

func (h *HasType) SetType(t *Type) {
	if t != nil {
		(*jnigi.ObjectRef)(h).CallMethod(env, "setType", jnigi.Void, (*jnigi.ObjectRef)(t).Cast("de/fraunhofer/aisec/cpg/graph/types/Type"))
	}
}

func (h *HasType) GetType() *Type {
	i, err := (*jnigi.ObjectRef)(h).CallMethod(env, "getType", jnigi.ObjectType("de/fraunhofer/aisec/cpg/graph/types/Type"))
	if err != nil {
		log.Fatal(err)
	}

	return (*Type)(i.(*jnigi.ObjectRef))
}

func (t *ObjectType) AddGeneric(g *Type) {
	_, err := (*jnigi.ObjectRef)(t).Cast("de/fraunhofer/aisec/cpg/graph/types/ObjectType").CallMethod(env, "addGeneric", jnigi.Void, (*jnigi.ObjectRef)(g).Cast("de/fraunhofer/aisec/cpg/graph/types/Type"))
	if err != nil {
		log.Fatal(err)
	}
}
