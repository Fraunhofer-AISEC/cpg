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
}

func InitEnv(e *jnigi.Env) {
	env = e
}

func (g *GoLanguageFrontend) SetCurrentTU(tu *cpg.TranslationUnitDeclaration) {
	err := g.SetField(env, "currentTU", (*jnigi.ObjectRef)(tu))

	if err != nil {
		log.Fatal(err)
	}
}

func (g *GoLanguageFrontend) GetCurrentTU() *cpg.TranslationUnitDeclaration {
	var tu = jnigi.NewObjectRef("de/fraunhofer/aisec/cpg/graph/declarations/TranslationUnitDeclaration")
	err := g.GetField(env, "currentTU", tu)
	if err != nil {
		log.Fatal(err)
	}

	return (*cpg.TranslationUnitDeclaration)(tu)
}

func (g *GoLanguageFrontend) GetCodeFromRawNode(fset *token.FileSet, astNode ast.Node) string {
	var codeBuf bytes.Buffer
	_ = printer.Fprint(&codeBuf, fset, astNode)

	return codeBuf.String()
}

func (g *GoLanguageFrontend) GetScopeManager() *cpg.ScopeManager {
	var scope = jnigi.NewObjectRef("de/fraunhofer/aisec/cpg/passes/scopes/ScopeManager")
	err := g.GetField(env, "scopeManager", scope)
	if err != nil {
		log.Fatal(err)
	}

	return (*cpg.ScopeManager)(scope)
}

func (g *GoLanguageFrontend) getLog() (logger *jnigi.ObjectRef, err error) {
	logger = jnigi.NewObjectRef("org/slf4j/Logger")
	err = env.GetStaticField("de/fraunhofer/aisec/cpg/frontends/LanguageFrontend", "log", logger)

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

func (g *GoLanguageFrontend) LogError(format string, args ...interface{}) (err error) {
	var logger *jnigi.ObjectRef

	if logger, err = g.getLog(); err != nil {
		return
	}

	err = logger.CallMethod(env, "error", nil, cpg.NewString(fmt.Sprintf(format, args...)))

	return
}
