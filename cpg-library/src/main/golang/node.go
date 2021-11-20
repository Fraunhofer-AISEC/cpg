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
	"bytes"
	"go/ast"
	"go/printer"
	"go/token"
	"log"

	"tekao.net/jnigi"
)

type Node jnigi.ObjectRef

func (n *Node) SetName(s string) error {
	return (*jnigi.ObjectRef)(n).SetField(env, "name", NewString(s))
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

func (n *Node) GetName() string {
	o, _ := (*jnigi.ObjectRef)(n).CallMethod(env, "getName", jnigi.ObjectType("java/lang/String"))

	if o == nil {
		return ""
	}

	b, err := o.(*jnigi.ObjectRef).CallMethod(env, "getBytes", jnigi.Byte|jnigi.Array)
	if err != nil {
		log.Fatal(err)
	}

	return string(b.([]byte))
}

func updateCode(fset *token.FileSet, node *Node, astNode ast.Node) {
	var codeBuf bytes.Buffer
	_ = printer.Fprint(&codeBuf, fset, astNode)

	node.SetCode(codeBuf.String())
}

func updateLocation(fset *token.FileSet, node *Node, astNode ast.Node) {
	if astNode == nil {
		return
	}

	file := fset.File(astNode.Pos())
	if file == nil {
		return
	}

	uri, err := env.NewObject("java/net/URI", NewString(file.Name()))
	if err != nil {
		log.Fatal(err)
	}

	region := NewRegion(fset, astNode,
		fset.Position(astNode.Pos()).Line,
		fset.Position(astNode.Pos()).Column,
		fset.Position(astNode.End()).Line,
		fset.Position(astNode.End()).Column,
	)

	location := NewPhysicalLocation(fset, astNode, uri, region)

	err = node.SetLocation(location)
	if err != nil {
		log.Fatal(err)
	}
}
