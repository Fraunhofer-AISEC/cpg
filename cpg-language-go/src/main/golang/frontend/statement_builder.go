/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
package frontend

import (
	"cpg"
	"fmt"
	"go/ast"
	"go/token"

	"tekao.net/jnigi"
)

func (g *GoLanguageFrontend) NewCompoundStatement(fset *token.FileSet, astNode ast.Node) *cpg.CompoundStatement {
	return (*cpg.CompoundStatement)(g.NewStatement("CompoundStatement", fset, astNode))
}

func (g *GoLanguageFrontend) NewReturnStatement(fset *token.FileSet, astNode ast.Node) *cpg.ReturnStatement {
	return (*cpg.ReturnStatement)(g.NewStatement("ReturnStatement", fset, astNode))
}

func (g *GoLanguageFrontend) NewDeclarationStatement(fset *token.FileSet, astNode ast.Node) *cpg.DeclarationStatement {
	return (*cpg.DeclarationStatement)(g.NewStatement("DeclarationStatement", fset, astNode))
}

func (g *GoLanguageFrontend) NewIfStatement(fset *token.FileSet, astNode ast.Node) *cpg.IfStatement {
	return (*cpg.IfStatement)(g.NewStatement("IfStatement", fset, astNode))
}

func (g *GoLanguageFrontend) NewForStatement(fset *token.FileSet, astNode ast.Node) *cpg.ForStatement {
	return (*cpg.ForStatement)(g.NewStatement("ForStatement", fset, astNode))
}

func (g *GoLanguageFrontend) NewForEachStatement(fset *token.FileSet, astNode ast.Node) *cpg.ForEachStatement {
	return (*cpg.ForEachStatement)(g.NewStatement("ForEachStatement", fset, astNode))
}

func (g *GoLanguageFrontend) NewSwitchStatement(fset *token.FileSet, astNode ast.Node) *cpg.SwitchStatement {
	return (*cpg.SwitchStatement)(g.NewStatement("SwitchStatement", fset, astNode))
}

func (g *GoLanguageFrontend) NewCaseStatement(fset *token.FileSet, astNode ast.Node) *cpg.CaseStatement {
	return (*cpg.CaseStatement)(g.NewStatement("CaseStatement", fset, astNode))
}

func (g *GoLanguageFrontend) NewDefaultStatement(fset *token.FileSet, astNode ast.Node) *cpg.DefaultStatement {
	return (*cpg.DefaultStatement)(g.NewStatement("DefaultStatement", fset, astNode))
}

func (g *GoLanguageFrontend) NewStatement(typ string, fset *token.FileSet, astNode ast.Node, args ...any) *jnigi.ObjectRef {
	var node = jnigi.NewObjectRef(fmt.Sprintf("%s/%s", cpg.StatementsPackage, typ))

	// Prepend the g as the receiver
	args = append([]any{g.Cast(cpg.GraphPackage + "/MetadataProvider")}, args...)

	err := env.CallStaticMethod(
		cpg.GraphPackage+"/StatementBuilderKt",
		fmt.Sprintf("new%s", typ), node,
		args...,
	)
	if err != nil {
		panic(err)
	}

	updateCode(fset, (*cpg.Node)(node), astNode)
	updateLocation(fset, (*cpg.Node)(node), astNode)

	return node
}
