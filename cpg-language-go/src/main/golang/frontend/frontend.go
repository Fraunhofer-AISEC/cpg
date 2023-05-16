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
package frontend

import (
	"bytes"
	"cpg"
	"fmt"
	"go/ast"
	"go/printer"
	"go/token"
	"log"

	"golang.org/x/mod/modfile"
	"tekao.net/jnigi"
)

var env *jnigi.Env

type GoLanguageFrontend struct {
	*jnigi.ObjectRef
	File       *ast.File
	Module     *modfile.File
	CommentMap ast.CommentMap
	TopLevel   string

	CurrentTU *cpg.TranslationUnitDeclaration
}

func InitEnv(e *jnigi.Env) {
	env = e
}

func (g *GoLanguageFrontend) GetCodeFromRawNode(fset *token.FileSet, astNode ast.Node) string {
	var codeBuf bytes.Buffer
	_ = printer.Fprint(&codeBuf, fset, astNode)

	return codeBuf.String()
}

func (g *GoLanguageFrontend) GetScopeManager() *cpg.ScopeManager {
	var scope = jnigi.NewObjectRef(cpg.ScopeManagerClass)
	err := g.GetField(env, "scopeManager", scope)
	if err != nil {
		log.Fatal(err)
	}

	return (*cpg.ScopeManager)(scope)
}

func (g *GoLanguageFrontend) getLog() (logger *jnigi.ObjectRef, err error) {
	logger = jnigi.NewObjectRef("org/slf4j/Logger")
	err = env.GetStaticField(cpg.LanguageFrontendClass, "log", logger)

	return
}

func (g *GoLanguageFrontend) LogInfo(format string, args ...interface{}) (err error) {
	var logger *jnigi.ObjectRef

	if logger, err = g.getLog(); err != nil {
		return
	}

	err = logger.CallMethod(env, "info", nil, cpg.NewString(fmt.Sprintf(format, args...)))

	return
}

func (g *GoLanguageFrontend) LogDebug(format string, args ...interface{}) (err error) {
	var logger *jnigi.ObjectRef

	if logger, err = g.getLog(); err != nil {
		return
	}

	err = logger.CallMethod(env, "debug", nil, cpg.NewString(fmt.Sprintf(format, args...)))

	return
}

func (g *GoLanguageFrontend) LogTrace(format string, args ...interface{}) (err error) {
	var logger *jnigi.ObjectRef

	if logger, err = g.getLog(); err != nil {
		return
	}

	err = logger.CallMethod(env, "trace", nil, cpg.NewString(fmt.Sprintf(format, args...)))

	return
}

func (g *GoLanguageFrontend) LogError(format string, args ...interface{}) (err error) {
	var logger *jnigi.ObjectRef

	if logger, err = g.getLog(); err != nil {
		return
	}

	err = logger.CallMethod(env, "error", nil, cpg.NewString(fmt.Sprintf(format, args...)))

	return
}

func (g *GoLanguageFrontend) GetLanguage() (l *cpg.Language, err error) {
	l = new(cpg.Language)
	err = g.ObjectRef.CallMethod(env, "getLanguage", l)

	return
}

func updateCode(fset *token.FileSet, node *cpg.Node, astNode ast.Node) {
	node.SetCode(code(fset, astNode))
}

func code(fset *token.FileSet, astNode ast.Node) string {
	var codeBuf bytes.Buffer
	_ = printer.Fprint(&codeBuf, fset, astNode)

	return codeBuf.String()
}

func updateLocation(fset *token.FileSet, node *cpg.Node, astNode ast.Node) {
	if astNode == nil {
		return
	}

	file := fset.File(astNode.Pos())
	if file == nil {
		return
	}

	uri, err := env.NewObject("java/net/URI", cpg.NewString(file.Name()))
	if err != nil {
		log.Fatal(err)
	}

	region := cpg.NewRegion(fset, astNode,
		fset.Position(astNode.Pos()).Line,
		fset.Position(astNode.Pos()).Column,
		fset.Position(astNode.End()).Line,
		fset.Position(astNode.End()).Column,
	)

	location := cpg.NewPhysicalLocation(fset, astNode, uri, region)

	err = node.SetLocation(location)
	if err != nil {
		log.Fatal(err)
	}
}

func updateLanguage(node *cpg.Node, frontend *GoLanguageFrontend) {
	var (
		err error
		l   *cpg.Language
	)

	l, err = frontend.GetLanguage()
	if err != nil {
		log.Fatal(err)
	}

	err = node.SetLanguge(l)
	if err != nil {
		log.Fatal(err)
	}
}
