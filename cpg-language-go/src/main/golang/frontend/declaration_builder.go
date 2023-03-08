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

func (frontend *GoLanguageFrontend) NewTranslationUnitDeclaration(fset *token.FileSet, astNode ast.Node, name string) *cpg.TranslationUnitDeclaration {
	return (*cpg.TranslationUnitDeclaration)(frontend.NewDeclaration("TranslationUnitDeclaration", fset, astNode, name))
}

func (frontend *GoLanguageFrontend) NewNamespaceDeclaration(fset *token.FileSet, astNode ast.Node, name string) *cpg.NamespaceDeclaration {
	return (*cpg.NamespaceDeclaration)(frontend.NewDeclaration("NamespaceDeclaration", fset, astNode, name))
}

func (frontend *GoLanguageFrontend) NewIncludeDeclaration(fset *token.FileSet, astNode ast.Node, name string) *cpg.IncludeDeclaration {
	return (*cpg.IncludeDeclaration)(frontend.NewDeclaration("IncludeDeclaration", fset, astNode, name))
}

func (frontend *GoLanguageFrontend) NewFunctionDeclaration(fset *token.FileSet, astNode ast.Node, name string, code string, localNameOnly bool) *cpg.FunctionDeclaration {
	return (*cpg.FunctionDeclaration)(frontend.NewDeclaration("FunctionDeclaration", fset, astNode, name,
		cpg.NewString(code),
		jnigi.NewObjectRef("java/lang/Object"),
		localNameOnly,
	))
}

func (frontend *GoLanguageFrontend) NewMethodDeclaration(fset *token.FileSet, astNode ast.Node, name string) *cpg.MethodDeclaration {
	return (*cpg.MethodDeclaration)(frontend.NewDeclaration("MethodDeclaration", fset, astNode, name))
}

func (frontend *GoLanguageFrontend) NewRecordDeclaration(fset *token.FileSet, astNode ast.Node, name string, kind string) *cpg.RecordDeclaration {
	return (*cpg.RecordDeclaration)(frontend.NewDeclaration("RecordDeclaration", fset, astNode, name, cpg.NewString(kind)))
}

func (frontend *GoLanguageFrontend) NewVariableDeclaration(fset *token.FileSet, astNode ast.Node, name string) *cpg.VariableDeclaration {
	return (*cpg.VariableDeclaration)(frontend.NewDeclaration("VariableDeclaration", fset, astNode, name))
}

func (frontend *GoLanguageFrontend) NewParamVariableDeclaration(fset *token.FileSet, astNode ast.Node, name string) *cpg.ParamVariableDeclaration {
	return (*cpg.ParamVariableDeclaration)(frontend.NewDeclaration("ParamVariableDeclaration", fset, astNode, name))
}

func (frontend *GoLanguageFrontend) NewFieldDeclaration(fset *token.FileSet, astNode ast.Node, name string) *cpg.FieldDeclaration {
	return (*cpg.FieldDeclaration)(frontend.NewDeclaration("FieldDeclaration", fset, astNode, name))
}

func (frontend *GoLanguageFrontend) NewDeclaration(typ string, fset *token.FileSet, astNode ast.Node, name string, args ...any) *jnigi.ObjectRef {
	var node = jnigi.NewObjectRef(fmt.Sprintf("%s/%s", cpg.DeclarationsPackage, typ))

	// Prepend the frontend and the name as the receiver and the first argument
	args = append([]any{frontend.Cast(MetadataProviderClass), cpg.NewCharSequence(name)}, args...)

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
