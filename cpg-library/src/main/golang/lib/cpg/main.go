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
package main

import (
	"cpg"
	"cpg/frontend"
	"go/ast"
	"go/parser"
	"go/token"

	"log"
	"unsafe"

	"tekao.net/jnigi"
)

//#include <jni.h>
import "C"

func main() {

}

//export Java_de_fraunhofer_aisec_cpg_frontends_golang_GoLanguageFrontend_parseInternal
func Java_de_fraunhofer_aisec_cpg_frontends_golang_GoLanguageFrontend_parseInternal(envPointer *C.JNIEnv, thisPtr C.jobject, arg1 C.jobject, arg2 C.jobject, arg3 C.jobject) C.jobject {
	env := jnigi.WrapEnv(unsafe.Pointer(envPointer))

	goFrontend := &frontend.GoLanguageFrontend{
		jnigi.WrapJObject(
			uintptr(thisPtr),
			"de/fraunhofer/aisec/cpg/frontends/golang/GoLanguageFrontend",
			false,
		),
		nil,
		nil,
		ast.CommentMap{},
	}

	srcObject := jnigi.WrapJObject(uintptr(arg1), "java/lang/String", false)
	pathObject := jnigi.WrapJObject(uintptr(arg2), "java/lang/String", false)
	topLevelObject := jnigi.WrapJObject(uintptr(arg3), "java/lang/String", false)

	frontend.InitEnv(env)
	cpg.InitEnv(env)

	src, err := srcObject.CallMethod(env, "getBytes", jnigi.Byte|jnigi.Array)
	if err != nil {
		log.Fatal(err)
	}

	path, err := pathObject.CallMethod(env, "getBytes", jnigi.Byte|jnigi.Array)
	if err != nil {
		log.Fatal(err)
	}

	topLevel, err := topLevelObject.CallMethod(env, "getBytes", jnigi.Byte|jnigi.Array)
	if err != nil {
		log.Fatal(err)
	}

	fset := token.NewFileSet()
	file, err := parser.ParseFile(fset, string(path.([]byte)), string(src.([]byte)), parser.ParseComments)
	if err != nil {
		log.Fatal(err)
	}

	goFrontend.CommentMap = ast.NewCommentMap(fset, file, file.Comments)

	_, err = goFrontend.ParseModule(string(topLevel.([]byte)))
	if err != nil {
		goFrontend.LogError("Error occurred while looking for Go modules file: %v", err)
	}

	goFrontend.File = file

	tu, err := goFrontend.HandleFile(fset, file, string(path.([]byte)))
	if err != nil {
		log.Fatal(err)
	}

	return C.jobject((*jnigi.ObjectRef)(tu).JObject())
}
