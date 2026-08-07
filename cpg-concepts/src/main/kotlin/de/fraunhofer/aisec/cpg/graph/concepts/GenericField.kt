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

import de.fraunhofer.aisec.cpg.persistence.DoNotPersist

/**
 * A single property's value, as attached to a [GenericLLMConcept]/[GenericLLMOperation].
 *
 * @param value The property's value (as a string representation).
 * @param description A description of the property, e.g. what it represents, or - for a property
 *   corresponding to one of the tagged function's actual parameters - which parameter that is
 *   (name/position).
 *
 * TODO: also capture the property's declared `type` here once we decide how to represent/query it.
 */
@DoNotPersist data class GenericPropertyValue(val value: String, val description: String? = null)

/**
 * Represents a generic set of properties for a concept or operation. This can be used to store
 * arbitrary "fields". The key represents the name of the property.
 *
 * TODO: neo4j persistence
 */
@DoNotPersist data class GenericProperties(val properties: Map<String, GenericPropertyValue>)
