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

import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.unknownType

/**
 * This type represents a [Type] that uses auto-inference (usually from an initializer) to determine
 * its actual type. It is commonly used in languages that have a special keyword, such as `auto` in
 * C++.
 *
 * Things to consider:
 * 1) This is intentionally a distinct type and not the [UnknownType]. The type is known to the
 *    compiler (or to us) at some point, e.g., after an assignment, but it is not specifically
 *    specified in the source-code.
 * 2) This should not be used to languages that have dynamic types. Once auto-type who was assigned
 *    to [Expression.type] is "resolved", it should be replaced by the actual type that it
 *    represents. Contrary to that, a [DynamicType] can change its internal type representation at
 *    any point, e.g., after the next assignment.
 */
class AutoType(language: Language<*>) : Type("auto", language) {
    override fun reference(pointer: PointerType.PointerOrigin?): Type {
        return PointerType(this, pointer)
    }

    override fun dereference(): Type {
        return unknownType()
    }
}
