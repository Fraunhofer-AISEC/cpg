/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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
import de.fraunhofer.aisec.cpg.graph.unknownType

/**
 * This type represents a [Type] that uses auto-inference (usually from an initializer) to determine
 * its actual type. It is commonly used in dynamically typed languages or in languages that have a
 * special keyword, such as `auto` in C++.
 *
 * Note: This is intentionally a distinct type and not the [UnknownType].
 */
class AutoType(ctx: TranslationContext, override var language: Language<*>) :
    Type(ctx, "auto", language) {
    override fun reference(pointer: PointerType.PointerOrigin?): Type {
        return PointerType(ctx, this, pointer)
    }

    override fun dereference(): Type {
        return unknownType()
    }
}
