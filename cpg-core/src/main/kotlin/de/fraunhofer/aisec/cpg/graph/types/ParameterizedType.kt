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
package de.fraunhofer.aisec.cpg.graph.types

import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.types.PointerType.PointerOrigin

/**
 * ParameterizedTypes describe types, that are passed as Paramters to Classes E.g. uninitialized
 * generics in the graph are represented as ParameterizedTypes
 */
class ParameterizedType : Type {
    constructor(type: Type) : super(type) {
        language = type.language
    }

    constructor(typeName: String?, language: Language<out LanguageFrontend>?) : super(typeName) {
        this.language = language
    }

    override fun reference(pointer: PointerOrigin?): Type {
        return PointerType(this, pointer)
    }

    override fun dereference(): Type {
        return this
    }

    override fun duplicate(): Type {
        return ParameterizedType(this)
    }
}
