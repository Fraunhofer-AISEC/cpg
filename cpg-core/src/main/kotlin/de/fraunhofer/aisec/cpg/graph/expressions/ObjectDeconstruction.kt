/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.expressions

import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgesOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.persistence.Relationship
import java.util.Objects

/**
 * Deconstructs an object of a specified [type] into its [components]. If a component is a
 * [NamedDeconstruction], its name defines which field of the object it is bound to (data flows by
 * name); otherwise components are matched by position (data flows by index, e.g. tuple/array/
 * tuple-struct elements).
 *
 * In Rust, this models struct, tuple, tuple-struct, enum-variant, and slice/array patterns, e.g.
 *
 * ```rust
 * match shape {
 *     Point { x, y } => ...,       // named components -> bind by field name
 *     Circle(radius) => ...,       // positional component -> bind by position
 *     [first, .., last] => ...,    // positional components of a slice pattern
 * }
 * ```
 *
 * `Point { x, y }` and `Circle(radius)` each become one [ObjectDeconstruction], typed to `Point` /
 * `Circle` respectively, with one [components] entry per bound field/element.
 */
class ObjectDeconstruction : Deconstruction(), HasType.TypeObserver {
    @Relationship("COMPONENTS") var componentEdges = astEdgesOf<Expression>()
    var components by unwrapping(ObjectDeconstruction::componentEdges)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ObjectDeconstruction) return false
        return super.equals(other) && components == other.components
    }

    override fun hashCode() = Objects.hash(super.hashCode(), components)

    override fun typeChanged(newType: Type, src: HasType) {
        val type = type
        // Todo if my type changes i need to forward these changes to my `children`. Here Type
        // deconstruction
        // works inversely to expression evaluation.
    }

    override fun assignedTypeChanged(assignedTypes: Set<Type>, src: HasType) {
        addAssignedTypes(assignedTypes)
        // Todo if my type changes i need to forward these changes to my `children`. Here Type
        // deconstruction
        // works inversely to expression evaluation.
    }
}
