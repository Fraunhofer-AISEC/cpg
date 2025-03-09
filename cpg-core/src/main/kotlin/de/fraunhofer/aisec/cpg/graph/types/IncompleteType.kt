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

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.types.PointerType.PointerOrigin

/**
 * IncompleteTypes are defined as object with unknown size. For instance: void, arrays of unknown
 * length, forward declared classes in C++
 *
 * Right now we are only dealing with void for objects with unknown size, therefore the name is
 * fixed to void. However, this can be changed in the future, in order to support other objects with
 * unknown size apart from void. Therefore, this Type is not called VoidType
 */
class IncompleteType : Type {
    constructor(ctx: TranslationContext, language: Language<*>) : super(ctx, "void", language)

    /** @return PointerType to a IncompleteType, e.g. void* */
    override fun reference(pointer: PointerOrigin?): Type {
        return PointerType(ctx, this, pointer)
    }

    /** @return dereferencing void results in void therefore the same type is returned */
    override fun dereference(): Type {
        return this
    }

    override fun equals(other: Any?): Boolean {
        return other is IncompleteType
    }

    override fun hashCode() = super.hashCode()
}
