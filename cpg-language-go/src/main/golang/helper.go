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
package cpg

import "tekao.net/jnigi"

type Castable interface {
	Cast(className string) *jnigi.CastedObjectRef
}

func ListOf[T Castable](slice []T) (list *jnigi.ObjectRef, err error) {
	list, err = env.NewObject("java/util/ArrayList")
	if err != nil {
		return nil, err
	}

	for _, t := range slice {
		var dummy bool
		if err := list.CallMethod(env, "add", &dummy, t.Cast("java/lang/Object")); err != nil {
			return nil, err
		}
	}

	return
}

func StringOf(str string) (obj *jnigi.ObjectRef, err error) {
	obj, err = env.NewObject("java/lang/String", []byte(str))
	if err != nil {
		return nil, err
	}

	return
}
