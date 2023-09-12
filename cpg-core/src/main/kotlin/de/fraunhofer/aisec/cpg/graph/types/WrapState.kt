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

import de.fraunhofer.aisec.cpg.graph.types.PointerType.PointerOrigin
import java.util.*

/**
 * A [WrapState] holds information how a "wrapped" type (for example a [PointerType]) is built from
 * its element types(s). This can potentially be a chain of different pointer/array operations.
 */
class WrapState {
    /** The total depth of "wrapping". This is usually equal to [Type.referenceDepth] */
    var depth = 0

    /**
     * An array of [PointerType.PointerOrigin] values, applied in the order the types are wrapped
     * in.
     */
    var pointerOrigins: Array<PointerOrigin?> = arrayOf(PointerOrigin.ARRAY)

    /** True, if the original type was a [ReferenceType]. It is then stored in [referenceType]. */
    var isReference = false

    /** The [ReferenceType], if [isReference] is true. */
    var referenceType: ReferenceType? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WrapState) return false

        return depth == other.depth &&
            isReference == other.isReference &&
            pointerOrigins.contentEquals(other.pointerOrigins) &&
            referenceType == other.referenceType
    }

    override fun hashCode(): Int {
        return Objects.hash(depth, isReference, pointerOrigins.contentHashCode(), referenceType)
    }
}
