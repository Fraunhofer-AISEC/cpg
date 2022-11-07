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
	"go/ast"
	"go/token"
	"log"
	"runtime/debug"
)

func (frontend *GoLanguageFrontend) NewTranslationUnitDeclaration(fset *token.FileSet, astNode ast.Node, name string, code string) *cpg.TranslationUnitDeclaration {
	tu, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/declarations/TranslationUnitDeclaration")
	if err != nil {
		log.Fatal(err)
		debug.PrintStack()
	}

	updateCode(fset, (*cpg.Node)(tu), astNode)
	updateLocation(fset, (*cpg.Node)(tu), astNode)
	updateLanguage((*cpg.Node)(tu), frontend)

	(*cpg.Node)(tu).SetName(name)

	return (*cpg.TranslationUnitDeclaration)(tu)
}

func (frontend *GoLanguageFrontend) NewNamespaceDeclaration(fset *token.FileSet, astNode ast.Node, name string, code string) *cpg.NamespaceDeclaration {
	tu, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/declarations/NamespaceDeclaration")
	if err != nil {
		log.Fatal(err)
		debug.PrintStack()
	}

	updateCode(fset, (*cpg.Node)(tu), astNode)
	updateLocation(fset, (*cpg.Node)(tu), astNode)
	updateLanguage((*cpg.Node)(tu), frontend)

	(*cpg.Node)(tu).SetName(name)

	return (*cpg.NamespaceDeclaration)(tu)
}

func (frontend *GoLanguageFrontend) NewIncludeDeclaration(fset *token.FileSet, astNode ast.Node) *cpg.IncludeDeclaration {
	tu, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/declarations/IncludeDeclaration")
	if err != nil {
		log.Fatal(err)
		debug.PrintStack()
	}

	updateCode(fset, (*cpg.Node)(tu), astNode)
	updateLocation(fset, (*cpg.Node)(tu), astNode)
	updateLanguage((*cpg.Node)(tu), frontend)

	return (*cpg.IncludeDeclaration)(tu)
}

func (frontend *GoLanguageFrontend) NewFunctionDeclaration(fset *token.FileSet, astNode ast.Node) *cpg.FunctionDeclaration {
	tu, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/declarations/FunctionDeclaration")
	if err != nil {
		log.Fatal(err)
		debug.PrintStack()
	}

	updateCode(fset, (*cpg.Node)(tu), astNode)
	updateLocation(fset, (*cpg.Node)(tu), astNode)
	updateLanguage((*cpg.Node)(tu), frontend)

	return (*cpg.FunctionDeclaration)(tu)
}

func (frontend *GoLanguageFrontend) NewMethodDeclaration(fset *token.FileSet, astNode ast.Node) *cpg.MethodDeclaration {
	tu, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/declarations/MethodDeclaration")
	if err != nil {
		log.Fatal(err)
		debug.PrintStack()
	}

	updateCode(fset, (*cpg.Node)(tu), astNode)
	updateLocation(fset, (*cpg.Node)(tu), astNode)
	updateLanguage((*cpg.Node)(tu), frontend)

	return (*cpg.MethodDeclaration)(tu)
}

func (frontend *GoLanguageFrontend) NewRecordDeclaration(fset *token.FileSet, astNode ast.Node) *cpg.RecordDeclaration {
	tu, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/declarations/RecordDeclaration")
	if err != nil {
		log.Fatal(err)
		debug.PrintStack()
	}

	updateCode(fset, (*cpg.Node)(tu), astNode)
	updateLocation(fset, (*cpg.Node)(tu), astNode)
	updateLanguage((*cpg.Node)(tu), frontend)

	return (*cpg.RecordDeclaration)(tu)
}

func (frontend *GoLanguageFrontend) NewVariableDeclaration(fset *token.FileSet, astNode ast.Node) *cpg.VariableDeclaration {
	tu, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/declarations/VariableDeclaration")
	if err != nil {
		log.Fatal(err)
		debug.PrintStack()
	}

	updateCode(fset, (*cpg.Node)(tu), astNode)
	updateLocation(fset, (*cpg.Node)(tu), astNode)
	updateLanguage((*cpg.Node)(tu), frontend)

	return (*cpg.VariableDeclaration)(tu)
}

func (frontend *GoLanguageFrontend) NewParamVariableDeclaration(fset *token.FileSet, astNode ast.Node) *cpg.ParamVariableDeclaration {
	tu, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/declarations/ParamVariableDeclaration")
	if err != nil {
		log.Fatal(err)
		debug.PrintStack()
	}

	updateCode(fset, (*cpg.Node)(tu), astNode)
	updateLocation(fset, (*cpg.Node)(tu), astNode)
	updateLanguage((*cpg.Node)(tu), frontend)

	return (*cpg.ParamVariableDeclaration)(tu)
}

func (frontend *GoLanguageFrontend) NewFieldDeclaration(fset *token.FileSet, astNode ast.Node) *cpg.FieldDeclaration {
	tu, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/declarations/FieldDeclaration")
	if err != nil {
		log.Fatal(err)
		debug.PrintStack()
	}

	updateCode(fset, (*cpg.Node)(tu), astNode)
	updateLocation(fset, (*cpg.Node)(tu), astNode)
	updateLanguage((*cpg.Node)(tu), frontend)

	return (*cpg.FieldDeclaration)(tu)
}
