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

import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.unknownType

/**
 * Represents a tuple of types. Primarily used in resolving function calls with multiple return
 * values.
 */
class TupleType(types: List<Type>) : Type(), HasSecondaryTypeEdge {
    var types: List<Type> = listOf()
        set(value) {
            field = value
            name = Name(value.joinToString(", ", "(", ")") { it.name.toString() })
        }

    init {
        this.types = types
    }

    override fun reference(pointer: PointerType.PointerOrigin?): Type {
        return unknownType()
    }

    override fun dereference(): Type {
        return unknownType()
    }

    override val secondaryTypes: List<Type>
        get() = types
}
