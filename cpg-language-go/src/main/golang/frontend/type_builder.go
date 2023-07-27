//
// Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
//                    $$$$$$\  $$$$$$$\   $$$$$$\
//                   $$  __$$\ $$  __$$\ $$  __$$\
//                   $$ /  \__|$$ |  $$ |$$ /  \__|
//                   $$ |      $$$$$$$  |$$ |$$$$\
//                   $$ |      $$  ____/ $$ |\_$$ |
//                   $$ |  $$\ $$ |      $$ |  $$ |
//                   \$$$$$   |$$ |      \$$$$$   |
//                    \______/ \__|       \______/
//
//

package frontend

import (
	"cpg"
	"fmt"
	"log"
	"strings"
	"tekao.net/jnigi"
)

func (frontend *GoLanguageFrontend) unknownType() *cpg.Type {
	return frontend.newType("MetadataProvider", "UnknownType")
}

func (frontend *GoLanguageFrontend) primitiveType(typeName string) *cpg.Type {
	return frontend.newType("LanguageProvider", "PrimitiveType", cpg.NewCharSequence(typeName))
}

func (frontend *GoLanguageFrontend) objectType(typeName string) *cpg.Type {
	return frontend.newType("LanguageProvider", "ObjectType", cpg.NewCharSequence(typeName))
}

func (frontend *GoLanguageFrontend) newType(providerClass string, typ string, args ...any) *cpg.Type {
	var node cpg.Type

	// Prepend the frontend as the receiver
	args = append([]any{frontend.Cast(cpg.GraphPackage + fmt.Sprintf("/%s", providerClass))}, args...)

	err := env.CallStaticMethod(
		cpg.GraphPackage+"/TypeBuilderKt",
		fmt.Sprintf("%s%s", strings.ToLower(string(typ[0])), typ[1:]), &node,
		args...,
	)
	if err != nil {
		log.Fatal(err)
	}

	return &node
}

func (frontend *GoLanguageFrontend) ref(t *cpg.Type, typ string) *cpg.Type {
	var (
		refType *jnigi.ObjectRef
		args    []any
	)

	refType = jnigi.NewObjectRef(cpg.TypeClass)

	args = []any{
		frontend.Cast(cpg.GraphPackage + fmt.Sprintf("/%s", "ContextProvider")),
		t,
	}

	err := env.CallStaticMethod(
		cpg.GraphPackage+"/TypeBuilderKt",
		typ,
		refType,
		args...,
	)
	if err != nil {
		panic(err)
	}

	return &cpg.Type{ObjectRef: refType}
}
