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

func (frontend *GoLanguageFrontend) NewCallExpression(fset *token.FileSet, astNode ast.Node) *cpg.CallExpression {
	c, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/expressions/CallExpression")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*cpg.Node)(c), astNode)
	updateLocation(fset, (*cpg.Node)(c), astNode)
	updateLanguage((*cpg.Node)(c), frontend)

	return (*cpg.CallExpression)(c)
}

func (frontend *GoLanguageFrontend) NewCastExpression(fset *token.FileSet, astNode ast.Node) *cpg.CastExpression {
	c, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/expressions/CastExpression")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*cpg.Node)(c), astNode)
	updateLocation(fset, (*cpg.Node)(c), astNode)
	updateLanguage((*cpg.Node)(c), frontend)

	return (*cpg.CastExpression)(c)
}

func (frontend *GoLanguageFrontend) NewMemberExpression(fset *token.FileSet, astNode ast.Node) *cpg.MemberExpression {
	c, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/expressions/MemberExpression")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*cpg.Node)(c), astNode)
	updateLocation(fset, (*cpg.Node)(c), astNode)
	updateLanguage((*cpg.Node)(c), frontend)

	return (*cpg.MemberExpression)(c)
}

func (frontend *GoLanguageFrontend) NewMemberCallExpression(fset *token.FileSet, astNode ast.Node) *cpg.MemberCallExpression {
	c, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/expressions/MemberCallExpression")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*cpg.Node)(c), astNode)
	updateLocation(fset, (*cpg.Node)(c), astNode)
	updateLanguage((*cpg.Node)(c), frontend)

	return (*cpg.MemberCallExpression)(c)
}

func (frontend *GoLanguageFrontend) NewNewExpression(fset *token.FileSet, astNode ast.Node) *cpg.NewExpression {
	c, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/expressions/NewExpression")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*cpg.Node)(c), astNode)
	updateLocation(fset, (*cpg.Node)(c), astNode)
	updateLanguage((*cpg.Node)(c), frontend)

	return (*cpg.NewExpression)(c)
}

func (frontend *GoLanguageFrontend) NewArrayCreationExpression(fset *token.FileSet, astNode ast.Node) *cpg.ArrayCreationExpression {
	c, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/expressions/ArrayCreationExpression")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*cpg.Node)(c), astNode)
	updateLocation(fset, (*cpg.Node)(c), astNode)
	updateLanguage((*cpg.Node)(c), frontend)

	return (*cpg.ArrayCreationExpression)(c)
}

func (frontend *GoLanguageFrontend) NewArraySubscriptionExpression(fset *token.FileSet, astNode ast.Node) *cpg.ArraySubscriptionExpression {
	c, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/expressions/ArraySubscriptionExpression")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*cpg.Node)(c), astNode)
	updateLocation(fset, (*cpg.Node)(c), astNode)
	updateLanguage((*cpg.Node)(c), frontend)

	return (*cpg.ArraySubscriptionExpression)(c)
}

func (frontend *GoLanguageFrontend) NewConstructExpression(fset *token.FileSet, astNode ast.Node) *cpg.ConstructExpression {
	c, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/expressions/ConstructExpression")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*cpg.Node)(c), astNode)
	updateLocation(fset, (*cpg.Node)(c), astNode)
	updateLanguage((*cpg.Node)(c), frontend)

	return (*cpg.ConstructExpression)(c)
}

func (frontend *GoLanguageFrontend) NewInitializerListExpression(fset *token.FileSet, astNode ast.Node) *cpg.InitializerListExpression {
	l, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/expressions/InitializerListExpression")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*cpg.Node)(l), astNode)
	updateLocation(fset, (*cpg.Node)(l), astNode)
	updateLanguage((*cpg.Node)(l), frontend)

	return (*cpg.InitializerListExpression)(l)
}

func (frontend *GoLanguageFrontend) NewBinaryOperator(fset *token.FileSet, astNode ast.Node) *cpg.BinaryOperator {
	c, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/expressions/BinaryOperator")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*cpg.Node)(c), astNode)
	updateLocation(fset, (*cpg.Node)(c), astNode)
	updateLanguage((*cpg.Node)(c), frontend)

	return (*cpg.BinaryOperator)(c)
}

func (frontend *GoLanguageFrontend) NewUnaryOperator(fset *token.FileSet, astNode ast.Node) *cpg.UnaryOperator {
	c, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/expressions/UnaryOperator")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*cpg.Node)(c), astNode)
	updateLocation(fset, (*cpg.Node)(c), astNode)
	updateLanguage((*cpg.Node)(c), frontend)

	return (*cpg.UnaryOperator)(c)
}

func (frontend *GoLanguageFrontend) NewLiteral(fset *token.FileSet, astNode ast.Node) *cpg.Literal {
	l, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/expressions/Literal")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*cpg.Node)(l), astNode)
	updateLocation(fset, (*cpg.Node)(l), astNode)
	updateLanguage((*cpg.Node)(l), frontend)

	return (*cpg.Literal)(l)
}

func (frontend *GoLanguageFrontend) NewDeclaredReferenceExpression(fset *token.FileSet, astNode ast.Node) *cpg.DeclaredReferenceExpression {
	l, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/expressions/DeclaredReferenceExpression")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*cpg.Node)(l), astNode)
	updateLocation(fset, (*cpg.Node)(l), astNode)
	updateLanguage((*cpg.Node)(l), frontend)

	return (*cpg.DeclaredReferenceExpression)(l)
}

func (frontend *GoLanguageFrontend) NewKeyValueExpression(fset *token.FileSet, astNode ast.Node) *cpg.KeyValueExpression {
	k, err := env.NewObject("de/fraunhofer/aisec/cpg/graph/statements/expressions/KeyValueExpression")
	if err != nil {
		log.Fatal(err)

	}

	updateCode(fset, (*cpg.Node)(k), astNode)
	updateLocation(fset, (*cpg.Node)(k), astNode)
	updateLanguage((*cpg.Node)(k), frontend)

	return (*cpg.KeyValueExpression)(k)
}
