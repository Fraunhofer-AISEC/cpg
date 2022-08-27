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

func (t *Type) ConvertToGo(o *jnigi.ObjectRef) error {
	*t = (Type)(*o)
	return nil
}

func (*Type) GetClassName() string {
	return "de/fraunhofer/aisec/cpg/graph/types/Type"
}

func (*Type) IsArray() bool {
	return false
}

type ObjectType jnigi.ObjectRef

func (t *ObjectType) ConvertToGo(o *jnigi.ObjectRef) error {
	*t = (ObjectType)(*o)
	return nil
}

func (*ObjectType) GetClassName() string {
	return "de/fraunhofer/aisec/cpg/graph/types/ObjectType"
}

func (*ObjectType) IsArray() bool {
	return false
}

type HasType jnigi.ObjectRef

func InitEnv(e *jnigi.Env) {
	env = e
}

func TypeParser_createFrom(s string, resolveAlias bool) *Type {
	var t Type
	err := env.CallStaticMethod("de/fraunhofer/aisec/cpg/graph/types/TypeParser", "createFrom", &t, NewString(s), resolveAlias)
	if err != nil {
		log.Fatal(err)

	}

	return &t
}

func UnknownType_getUnknown() *Type {
	var t Type
	err := env.CallStaticMethod("de/fraunhofer/aisec/cpg/graph/types/UnknownType", "getUnknownType", &t)
	if err != nil {
		log.Fatal(err)

	}

	return &t
}

func (h *Type) GetRoot() *Type {
	var t Type
	err := (*jnigi.ObjectRef)(h).CallMethod(env, "getRoot", &t)
	if err != nil {
		log.Fatal(err)
	}

	return &t
}

func (t *Type) Reference(o *jnigi.ObjectRef) *Type {
	var refType Type
	err := (*jnigi.ObjectRef)(t).CallMethod(env, "reference", &refType, (*jnigi.ObjectRef)(o).Cast("de/fraunhofer/aisec/cpg/graph/types/PointerType$PointerOrigin"))

	if err != nil {
		log.Fatal(err)
	}

	return &refType
}

func (h *HasType) SetType(t *Type) {
	if t != nil {
		(*jnigi.ObjectRef)(h).CallMethod(env, "setType", nil, (*jnigi.ObjectRef)(t).Cast("de/fraunhofer/aisec/cpg/graph/types/Type"))
	}
}

func (h *HasType) GetType() *Type {
	var t Type
	err := (*jnigi.ObjectRef)(h).CallMethod(env, "getType", &t)
	if err != nil {
		log.Fatal(err)
	}

	return &t
}

func (t *ObjectType) AddGeneric(g *Type) {
	// Stupid workaround, since casting does not work. See
	// https://github.com/timob/jnigi/issues/60
	var objType = jnigi.WrapJObject(uintptr((*jnigi.ObjectRef)(t).JObject()), "de/fraunhofer/aisec/cpg/graph/types/ObjectType", false)
	err := objType.CallMethod(env, "addGeneric", nil, (*jnigi.ObjectRef)(g).Cast("de/fraunhofer/aisec/cpg/graph/types/Type"))
	if err != nil {
		log.Fatal(err)
	}
}
