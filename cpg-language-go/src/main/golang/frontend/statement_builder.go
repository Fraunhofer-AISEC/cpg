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

func (frontend *GoLanguageFrontend) NewCompoundStatement(fset *token.FileSet, astNode ast.Node) *cpg.CompoundStatement {
	return (*cpg.CompoundStatement)(frontend.NewStatement("CompoundStatement", fset, astNode))
}

func (frontend *GoLanguageFrontend) NewReturnStatement(fset *token.FileSet, astNode ast.Node) *cpg.ReturnStatement {
	return (*cpg.ReturnStatement)(frontend.NewStatement("ReturnStatement", fset, astNode))
}

func (frontend *GoLanguageFrontend) NewDeclarationStatement(fset *token.FileSet, astNode ast.Node) *cpg.DeclarationStatement {
	return (*cpg.DeclarationStatement)(frontend.NewStatement("DeclarationStatement", fset, astNode))
}

func (frontend *GoLanguageFrontend) NewIfStatement(fset *token.FileSet, astNode ast.Node) *cpg.IfStatement {
	return (*cpg.IfStatement)(frontend.NewStatement("IfStatement", fset, astNode))
}

func (frontend *GoLanguageFrontend) NewForStatement(fset *token.FileSet, astNode ast.Node) *cpg.ForStatement {
	return (*cpg.ForStatement)(frontend.NewStatement("ForStatement", fset, astNode))
}

func (frontend *GoLanguageFrontend) NewForEachStatement(fset *token.FileSet, astNode ast.Node) *cpg.ForEachStatement {
	return (*cpg.ForEachStatement)(frontend.NewStatement("ForEachStatement", fset, astNode))
}

func (frontend *GoLanguageFrontend) NewSwitchStatement(fset *token.FileSet, astNode ast.Node) *cpg.SwitchStatement {
	return (*cpg.SwitchStatement)(frontend.NewStatement("SwitchStatement", fset, astNode))
}

func (frontend *GoLanguageFrontend) NewCaseStatement(fset *token.FileSet, astNode ast.Node) *cpg.CaseStatement {
	return (*cpg.CaseStatement)(frontend.NewStatement("CaseStatement", fset, astNode))
}

func (frontend *GoLanguageFrontend) NewDefaultStatement(fset *token.FileSet, astNode ast.Node) *cpg.DefaultStatement {
	return (*cpg.DefaultStatement)(frontend.NewStatement("DefaultStatement", fset, astNode))
}

func (frontend *GoLanguageFrontend) NewStatement(typ string, fset *token.FileSet, astNode ast.Node, args ...any) *jnigi.ObjectRef {
	var node = jnigi.NewObjectRef(fmt.Sprintf("%s/%s", cpg.StatementsPackage, typ))

	// Prepend the frontend as the receiver
	args = append([]any{frontend.Cast(cpg.GraphPackage + "/MetadataProvider")}, args...)

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
