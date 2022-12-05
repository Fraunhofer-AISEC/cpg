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

	"tekao.net/jnigi"
)

type Node jnigi.ObjectRef
type Name jnigi.ObjectRef

const CPGPackage = "de/fraunhofer/aisec/cpg"
const GraphPackage = CPGPackage + "/graph"
const NodeClass = GraphPackage + "/Node"
const NameClass = GraphPackage + "/Name"

func (n *Node) Cast(className string) *jnigi.ObjectRef {
	return (*jnigi.ObjectRef)(n).Cast(className)
}

func (n *Node) SetLanguge(l *Language) error {
	return (*jnigi.ObjectRef)(n).CallMethod(env, "setLanguage", nil, l)
}

func (n *Node) SetCode(s string) error {
	return (*jnigi.ObjectRef)(n).SetField(env, "code", NewString(s))
}

func (n *Node) SetComment(s string) error {
	return (*jnigi.ObjectRef)(n).SetField(env, "comment", NewString(s))
}

func (n *Node) SetLocation(location *PhysicalLocation) error {
	return (*jnigi.ObjectRef)(n).SetField(env, "location", (*jnigi.ObjectRef)(location))
}

func (n *Node) SetFullName(name *Name) {
	err := (*jnigi.ObjectRef)(n).CallMethod(env, "setFullName", nil, name)
	if err != nil {
		log.Fatal(err)
	}
}

func (n *Node) GetFullName() (fn *Name) {
	var o = jnigi.NewObjectRef(NameClass)
	err := (*jnigi.ObjectRef)(n).CallMethod(env, "getFullName", o)
	if err != nil {
		log.Fatal(err)
	}

	return (*Name)(o)
}

func (n *Node) GetName() string {
	var o = jnigi.NewObjectRef("java/lang/String")
	_ = (*jnigi.ObjectRef)(n).CallMethod(env, "getName", o)

	if o == nil {
		return ""
	}

	var b []byte
	err := o.CallMethod(env, "getBytes", &b)
	if err != nil {
		log.Fatal(err)
	}

	return string(b)
}

func (n *Name) ConvertToGo(o *jnigi.ObjectRef) error {
	*n = (Name)(*o)
	return nil
}

func (n *Name) ConvertToJava() (obj *jnigi.ObjectRef, err error) {
	return (*jnigi.ObjectRef)(n), nil
}

func (n *Name) GetClassName() string {
	return NameClass
}

func (n *Name) Cast(className string) *jnigi.ObjectRef {
	return (*jnigi.ObjectRef)(n).Cast(className)
}

func (n *Name) IsArray() bool {
	return false
}
