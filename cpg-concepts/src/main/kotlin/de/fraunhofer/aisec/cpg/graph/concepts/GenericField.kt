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
package de.fraunhofer.aisec.cpg.graph.concepts

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.persistence.DoNotPersist

/**
 * A single value held by a [GenericProperties] map. A property is either a plain scalar
 * ([StringValue]) or, when the LLM-defined property's type is `"NodeReference"`, an actual
 * reference to another [Node] in the graph ([NodeReferenceValue]) instead of a stringified id.
 */
sealed class GenericPropertyValue {
    data class StringValue(val value: String) : GenericPropertyValue()

    data class NodeReferenceValue(val node: Node) : GenericPropertyValue()
}

/**
 * Represents a generic set of properties for a concept or operation. This can be used to store
 * arbitrary "fields". The key represents the name of the property, and the value is either a plain
 * string or a reference to another node in the graph.
 *
 * TODO: neo4j persistence
 */
@DoNotPersist data class GenericProperties(val properties: Map<String, GenericPropertyValue>)
