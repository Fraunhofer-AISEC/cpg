/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.graph.types.PointerType.PointerOrigin
import de.fraunhofer.aisec.cpg.graph.unknownType
import java.util.*
import org.apache.commons.lang3.builder.ToStringBuilder

/**
 * ReferenceTypes describe CPP References (int&amp;), which represent an alternative name for a
 * variable. It is necessary to make this distinction, and not just rely on the original type as it
 * is required for matching parameters in function arguments to discover which implementation is
 * called.
 */
class ReferenceType : Type, SecondOrderType {
    override var elementType: Type = unknownType()

    constructor() : super()

    constructor(reference: Type) : super() {
        language = reference.language
        name = reference.name.append("&")
        this.elementType = reference
    }

    /**
     * @return Referencing a ReferenceType results in a PointerType to the original ReferenceType
     */
    override fun reference(pointer: PointerOrigin?): Type {
        return PointerType(this, pointer)
    }

    /**
     * @return Dereferencing a ReferenceType equals to dereferencing the original (non-reference)
     *   type
     */
    override fun dereference(): Type {
        return elementType.dereference()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReferenceType) return false
        return super.equals(other) && elementType == other.elementType
    }

    override fun hashCode() = Objects.hash(super.hashCode(), elementType)

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("elementType", elementType)
            .append("name", name)
            .append("typeOrigin", typeOrigin)
            .toString()
    }
}
