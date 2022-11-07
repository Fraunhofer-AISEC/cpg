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
)

func (frontend *GoLanguageFrontend) NewCompoundStatement(fset *token.FileSet, astNode ast.Node) *cpg.CompoundStatement {
	s, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/CompoundStatement")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*cpg.Node)(s), astNode)
	updateLocation(fset, (*cpg.Node)(s), astNode)
	updateLanguage((*cpg.Node)(s), frontend)

	return (*cpg.CompoundStatement)(s)
}

func (frontend *GoLanguageFrontend) NewReturnStatement(fset *token.FileSet, astNode ast.Node) *cpg.ReturnStatement {
	s, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/ReturnStatement")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*cpg.Node)(s), astNode)
	updateLocation(fset, (*cpg.Node)(s), astNode)
	updateLanguage((*cpg.Node)(s), frontend)

	return (*cpg.ReturnStatement)(s)
}

func (frontend *GoLanguageFrontend) NewDeclarationStatement(fset *token.FileSet, astNode ast.Node) *cpg.DeclarationStatement {
	s, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/DeclarationStatement")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*cpg.Node)(s), astNode)
	updateLocation(fset, (*cpg.Node)(s), astNode)
	updateLanguage((*cpg.Node)(s), frontend)

	return (*cpg.DeclarationStatement)(s)
}

func (frontend *GoLanguageFrontend) NewIfStatement(fset *token.FileSet, astNode ast.Node) *cpg.IfStatement {
	s, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/IfStatement")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*cpg.Node)(s), astNode)
	updateLocation(fset, (*cpg.Node)(s), astNode)
	updateLanguage((*cpg.Node)(s), frontend)

	return (*cpg.IfStatement)(s)
}

func (frontend *GoLanguageFrontend) NewForStatement(fset *token.FileSet, astNode ast.Node) *cpg.ForStatement {
	s, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/ForStatement")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*cpg.Node)(s), astNode)
	updateLocation(fset, (*cpg.Node)(s), astNode)
	updateLanguage((*cpg.Node)(s), frontend)

	return (*cpg.ForStatement)(s)
}

func (frontend *GoLanguageFrontend) NewSwitchStatement(fset *token.FileSet, astNode ast.Node) *cpg.SwitchStatement {
	s, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/SwitchStatement")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*cpg.Node)(s), astNode)
	updateLocation(fset, (*cpg.Node)(s), astNode)
	updateLanguage((*cpg.Node)(s), frontend)

	return (*cpg.SwitchStatement)(s)
}

func (frontend *GoLanguageFrontend) NewCaseStatement(fset *token.FileSet, astNode ast.Node) *cpg.CaseStatement {
	s, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/CaseStatement")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*cpg.Node)(s), astNode)
	updateLocation(fset, (*cpg.Node)(s), astNode)
	updateLanguage((*cpg.Node)(s), frontend)

	return (*cpg.CaseStatement)(s)
}

func (frontend *GoLanguageFrontend) NewDefaultStatement(fset *token.FileSet, astNode ast.Node) *cpg.DefaultStatement {
	s, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/DefaultStatement")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*cpg.Node)(s), astNode)
	updateLocation(fset, (*cpg.Node)(s), astNode)
	updateLanguage((*cpg.Node)(s), frontend)

	return (*cpg.DefaultStatement)(s)
}
