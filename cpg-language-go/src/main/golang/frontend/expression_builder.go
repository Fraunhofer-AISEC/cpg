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

func (frontend *GoLanguageFrontend) NewCallExpression(fset *token.FileSet, astNode ast.Node, callee cpg.Castable, name string) *cpg.CallExpression {
	if callee == nil {
		callee = jnigi.NewObjectRef(cpg.ExpressionClass)
	} else {
		callee = callee.Cast(cpg.ExpressionClass)
	}

	return (*cpg.CallExpression)(frontend.NewExpression("CallExpression", fset, astNode, callee, cpg.NewCharSequence(name)))
}

func (frontend *GoLanguageFrontend) NewCastExpression(fset *token.FileSet, astNode ast.Node) *cpg.CastExpression {
	return (*cpg.CastExpression)(frontend.NewExpression("CastExpression", fset, astNode))
}

func (frontend *GoLanguageFrontend) NewMemberExpression(fset *token.FileSet, astNode ast.Node, name string, base cpg.Castable) *cpg.MemberExpression {
	return (*cpg.MemberExpression)(frontend.NewExpression("MemberExpression", fset, astNode, cpg.NewCharSequence(name), base.Cast(cpg.ExpressionClass)))
}

func (frontend *GoLanguageFrontend) NewMemberCallExpression(fset *token.FileSet, astNode ast.Node, callee *cpg.Expression) *cpg.MemberCallExpression {
	return (*cpg.MemberCallExpression)(frontend.NewExpression("MemberCallExpression", fset, astNode,
		callee.Cast(cpg.ExpressionClass),
	))
}

func (frontend *GoLanguageFrontend) NewNewExpression(fset *token.FileSet, astNode ast.Node) *cpg.NewExpression {
	return (*cpg.NewExpression)(frontend.NewExpression("NewExpression", fset, astNode))
}

func (frontend *GoLanguageFrontend) NewArrayCreationExpression(fset *token.FileSet, astNode ast.Node) *cpg.ArrayCreationExpression {
	return (*cpg.ArrayCreationExpression)(frontend.NewExpression("ArrayCreationExpression", fset, astNode))
}

func (frontend *GoLanguageFrontend) NewArraySubscriptionExpression(fset *token.FileSet, astNode ast.Node) *cpg.ArraySubscriptionExpression {
	return (*cpg.ArraySubscriptionExpression)(frontend.NewExpression("ArraySubscriptionExpression", fset, astNode))
}

func (frontend *GoLanguageFrontend) NewRangeExpression(fset *token.FileSet, astNode ast.Node) *cpg.RangeExpression {
	return (*cpg.RangeExpression)(frontend.NewExpression("RangeExpression", fset, astNode))
}

func (frontend *GoLanguageFrontend) NewConstructExpression(fset *token.FileSet, astNode ast.Node) *cpg.ConstructExpression {
	return (*cpg.ConstructExpression)(frontend.NewExpression("ConstructExpression", fset, astNode))
}

func (frontend *GoLanguageFrontend) NewInitializerListExpression(fset *token.FileSet, astNode ast.Node) *cpg.InitializerListExpression {
	return (*cpg.InitializerListExpression)(frontend.NewExpression("InitializerListExpression", fset, astNode))
}

func (frontend *GoLanguageFrontend) NewBinaryOperator(fset *token.FileSet, astNode ast.Node, opCode string) *cpg.BinaryOperator {
	return (*cpg.BinaryOperator)(frontend.NewExpression("BinaryOperator", fset, astNode,
		cpg.NewString(opCode),
	))
}

func (frontend *GoLanguageFrontend) NewAssignExpression(fset *token.FileSet, astNode ast.Node, opCode string) *cpg.AssignExpression {
	return (*cpg.AssignExpression)(frontend.NewExpression("AssignExpression", fset, astNode,
		cpg.NewString(opCode),
	))
}

func (frontend *GoLanguageFrontend) NewUnaryOperator(fset *token.FileSet, astNode ast.Node, opCode string, postfix bool, prefix bool) *cpg.UnaryOperator {
	return (*cpg.UnaryOperator)(frontend.NewExpression("UnaryOperator", fset, astNode,
		cpg.NewString(opCode),
		postfix, prefix,
	))
}

func (frontend *GoLanguageFrontend) NewLiteral(fset *token.FileSet, astNode ast.Node, value cpg.Castable, typ *cpg.Type) *cpg.Literal {
	if value == nil {
		value = jnigi.NewObjectRef("java/lang/Object")
	} else {
		value = value.Cast("java/lang/Object")
	}

	if typ == nil {
		panic("typ is nil")
	}

	return (*cpg.Literal)(frontend.NewExpression("Literal", fset, astNode, value, typ.Cast(cpg.TypeClass)))
}

func (frontend *GoLanguageFrontend) NewDeclaredReferenceExpression(fset *token.FileSet, astNode ast.Node, name string) *cpg.DeclaredReferenceExpression {
	return (*cpg.DeclaredReferenceExpression)(frontend.NewExpression("DeclaredReferenceExpression", fset, astNode, cpg.NewCharSequence(name)))
}

func (frontend *GoLanguageFrontend) NewKeyValueExpression(fset *token.FileSet, astNode ast.Node) *cpg.KeyValueExpression {
	return (*cpg.KeyValueExpression)(frontend.NewExpression("KeyValueExpression", fset, astNode))
}

func (frontend *GoLanguageFrontend) NewLambdaExpression(fset *token.FileSet, astNode ast.Node) *cpg.LambdaExpression {
	return (*cpg.LambdaExpression)(frontend.NewExpression("LambdaExpression", fset, astNode))
}

func (frontend *GoLanguageFrontend) NewProblemExpression(fset *token.FileSet, astNode ast.Node, problem string) *cpg.ProblemExpression {
	return (*cpg.ProblemExpression)(frontend.NewExpression("ProblemExpression", fset, astNode, cpg.NewString(problem)))
}

func (frontend *GoLanguageFrontend) NewExpression(typ string, fset *token.FileSet, astNode ast.Node, args ...any) *jnigi.ObjectRef {
	var node = jnigi.NewObjectRef(fmt.Sprintf("%s/%s", cpg.ExpressionsPackage, typ))

	// Prepend the frontend as the receiver
	args = append([]any{frontend.Cast(cpg.GraphPackage + "/MetadataProvider")}, args...)

	err := env.CallStaticMethod(
		cpg.GraphPackage+"/ExpressionBuilderKt",
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
