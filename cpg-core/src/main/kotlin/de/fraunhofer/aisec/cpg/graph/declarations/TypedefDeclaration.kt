/*
 * Copyright (c) 2020, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.declarations

import de.fraunhofer.aisec.cpg.graph.types.DeclaresType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import java.util.*
import org.apache.commons.lang3.builder.ToStringBuilder

/** Represents a type alias definition as found in C/C++: `typedef unsigned long ulong;` */
class TypedefDeclaration : Declaration(), DeclaresType {
    /** The already existing type that is to be aliased */
    var type: Type = UnknownType.getUnknownType(null)

    /** The newly created alias to be defined */
    var alias: Type = UnknownType.getUnknownType(null)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TypedefDeclaration) return false
        return super.equals(other) && type == other.type && alias == other.alias
    }

    override fun hashCode() = Objects.hash(super.hashCode(), type, alias)

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .append("type", type)
            .append("alias", alias)
            .toString()
    }

    override val declaredType: Type
        get() = type

    /*override val declaredType: Type
    get() = alias*/
}
