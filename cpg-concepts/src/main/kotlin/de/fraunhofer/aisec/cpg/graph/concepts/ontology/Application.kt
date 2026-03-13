/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import java.time.ZonedDateTime
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.collections.MutableList
import kotlin.collections.MutableMap

/** This encapsulates the whole (source) code of an application. */
public open class Application(
    public val programmingLanguage: String?,
    public val programmingVersion: String?,
    public val translationUnits: List<String>?,
    public val automaticUpdates: AutomaticUpdates?,
    public val compute: Compute?,
    libraries: MutableList<Library?>,
    codeModules: MutableList<CodeModule?>,
    codeRepository: CodeRepository?,
    functionalities: MutableList<Functionality?>,
    creation_time: ZonedDateTime?,
    description: String?,
    resourceId: String?,
    labels: MutableMap<String, String>?,
    name: String?,
    raw: String?,
    parent: Resource?,
    underlyingNode: Node? = null,
) :
    Component(
        libraries,
        codeModules,
        codeRepository,
        functionalities,
        creation_time,
        description,
        resourceId,
        labels,
        name,
        raw,
        parent,
        underlyingNode,
    ) {
    init {
        name?.let { this.name = Name(localName = it) }
    }

    override fun equals(other: Any?): Boolean =
        other is Application &&
            super.equals(other) &&
            other.programmingLanguage == this.programmingLanguage &&
            other.programmingVersion == this.programmingVersion &&
            other.translationUnits == this.translationUnits &&
            other.automaticUpdates == this.automaticUpdates &&
            other.compute == this.compute

    override fun hashCode(): Int =
        Objects.hash(
            super.hashCode(),
            programmingLanguage,
            programmingVersion,
            translationUnits,
            automaticUpdates,
            compute,
        )
}
