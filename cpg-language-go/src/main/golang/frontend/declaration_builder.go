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

func (g *GoLanguageFrontend) NewTranslationUnitDeclaration(fset *token.FileSet, astNode ast.Node, name string) *cpg.TranslationUnitDeclaration {
	return (*cpg.TranslationUnitDeclaration)(g.NewDeclaration("TranslationUnitDeclaration", fset, astNode, name))
}

func (g *GoLanguageFrontend) NewNamespaceDeclaration(fset *token.FileSet, astNode ast.Node, name string) *cpg.NamespaceDeclaration {
	return (*cpg.NamespaceDeclaration)(g.NewDeclaration("NamespaceDeclaration", fset, astNode, name))
}

func (g *GoLanguageFrontend) NewIncludeDeclaration(fset *token.FileSet, astNode ast.Node, name string) *cpg.IncludeDeclaration {
	return (*cpg.IncludeDeclaration)(g.NewDeclaration("IncludeDeclaration", fset, astNode, name))
}

func (g *GoLanguageFrontend) NewFunctionDeclaration(fset *token.FileSet, astNode ast.Node, name string, code string, localNameOnly bool) *cpg.FunctionDeclaration {
	return (*cpg.FunctionDeclaration)(g.NewDeclaration("FunctionDeclaration", fset, astNode, name,
		cpg.NewString(code),
		jnigi.NewObjectRef("java/lang/Object"),
		localNameOnly,
	))
}

func (g *GoLanguageFrontend) NewMethodDeclaration(fset *token.FileSet, astNode ast.Node, name string) *cpg.MethodDeclaration {
	return (*cpg.MethodDeclaration)(g.NewDeclaration("MethodDeclaration", fset, astNode, name))
}

func (g *GoLanguageFrontend) NewRecordDeclaration(fset *token.FileSet, astNode ast.Node, name string, kind string) *cpg.RecordDeclaration {
	return (*cpg.RecordDeclaration)(g.NewDeclaration("RecordDeclaration", fset, astNode, name, cpg.NewString(kind)))
}

func (g *GoLanguageFrontend) NewVariableDeclaration(fset *token.FileSet, astNode ast.Node, name string) *cpg.VariableDeclaration {
	return (*cpg.VariableDeclaration)(g.NewDeclaration("VariableDeclaration", fset, astNode, name))
}

func (g *GoLanguageFrontend) NewParamVariableDeclaration(fset *token.FileSet, astNode ast.Node, name string) *cpg.ParamVariableDeclaration {
	return (*cpg.ParamVariableDeclaration)(g.NewDeclaration("ParamVariableDeclaration", fset, astNode, name))
}

func (g *GoLanguageFrontend) NewFieldDeclaration(fset *token.FileSet, astNode ast.Node, name string) *cpg.FieldDeclaration {
	return (*cpg.FieldDeclaration)(g.NewDeclaration("FieldDeclaration", fset, astNode, name))
}

func (g *GoLanguageFrontend) NewDeclaration(typ string, fset *token.FileSet, astNode ast.Node, name string, args ...any) *jnigi.ObjectRef {
	var node = jnigi.NewObjectRef(fmt.Sprintf("%s/%s", cpg.DeclarationsPackage, typ))

	// Prepend the g and the name as the receiver and the first argument
	args = append([]any{g.Cast(MetadataProviderClass), cpg.NewCharSequence(name)}, args...)

	err := env.CallStaticMethod(
		cpg.GraphPackage+"/DeclarationBuilderKt",
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
