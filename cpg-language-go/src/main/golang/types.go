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

type Type struct{ *jnigi.ObjectRef }
type ObjectType Type

const TypesPackage = GraphPackage + "/types"
const TypeClass = TypesPackage + "/Type"
const ObjectTypeClass = TypesPackage + "/ObjectType"
const UnknownTypeClass = TypesPackage + "/UnknownType"
const TypeParserClass = TypesPackage + "/TypeParser"
const PointerTypeClass = TypesPackage + "/PointerType"
const FunctionTypeClass = TypesPackage + "/FunctionType"
const PointerOriginClass = PointerTypeClass + "$PointerOrigin"

func (t *Type) ConvertToGo(o *jnigi.ObjectRef) error {
	t.ObjectRef = o
	return nil
}

func (t *Type) ConvertToJava() (obj *jnigi.ObjectRef, err error) {
	return t.ObjectRef, nil
}

func (*Type) GetClassName() string {
	return TypeClass
}

func (*Type) IsArray() bool {
	return false
}

func (*ObjectType) GetClassName() string {
	return ObjectTypeClass
}

type UnknownType struct {
	Type
}

func (*UnknownType) GetClassName() string {
	return UnknownTypeClass
}

type HasType jnigi.ObjectRef

func InitEnv(e *jnigi.Env) {
	env = e
}

func TypeParser_createFrom(s string, l *Language) *Type {
	var t Type
	err := env.CallStaticMethod(TypeParserClass, "createFrom", &t, NewCharSequence(s), l)
	if err != nil {
		log.Fatal(err)

	}

	return &t
}

func UnknownType_getUnknown(l *Language) *UnknownType {
	var t UnknownType
	err := env.CallStaticMethod(UnknownTypeClass, "getUnknownType", &t, l)
	if err != nil {
		log.Fatal(err)

	}

	return &t
}

func (t *Type) GetRoot() *Type {
	var root Type
	err := t.CallMethod(env, "getRoot", &root)
	if err != nil {
		log.Fatal(err)
	}

	return &root
}

func (t *Type) Reference(o *jnigi.ObjectRef) *Type {
	var refType Type
	err := t.CallMethod(env, "reference", &refType, (*jnigi.ObjectRef)(o).Cast(PointerOriginClass))

	if err != nil {
		log.Fatal(err)
	}

	return &refType
}

func (h *HasType) SetType(t *Type) {
	if t != nil {
		(*jnigi.ObjectRef)(h).CallMethod(env, "setType", nil, t.Cast(TypeClass))
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

func (t *Type) GetName() (fn *Name) {
	return (*Node)(t.ObjectRef).GetName()
}

func (t *ObjectType) AddGeneric(g *Type) {
	// Stupid workaround, since casting does not work. See
	// https://github.com/timob/jnigi/issues/60
	var objType = jnigi.WrapJObject(uintptr(t.JObject()), ObjectTypeClass, false)
	err := objType.CallMethod(env, "addGeneric", nil, g.Cast(TypeClass))
	if err != nil {
		log.Fatal(err)
	}
}

func FunctionType_ComputeType(decl *FunctionDeclaration) (t *Type, err error) {
	var funcType Type

	err = env.CallStaticMethod(FunctionTypeClass, "computeType", &t, decl)
	if err != nil {
		return nil, err
	}

	return &funcType, nil
}
