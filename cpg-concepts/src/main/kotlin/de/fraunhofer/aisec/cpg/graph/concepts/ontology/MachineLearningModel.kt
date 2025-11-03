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
import kotlin.Float
import kotlin.Int
import kotlin.String
import kotlin.collections.MutableList
import kotlin.collections.MutableMap

public abstract class MachineLearningModel(
    public val adv_robustness: Float?,
    public val explainability: Float?,
    public val poisonLevel: Float?,
    public val privacyLabel: Float?,
    public val privacyLevel: Float?,
    public val robustness: Float?,
    public val vulnerabilities: MutableList<Vulnerability?>,
    dataLocation: DataLocation?,
    creation_time: ZonedDateTime?,
    description: String?,
    labels: MutableMap<String, String>?,
    name: String?,
    raw: String?,
    parent: Resource?,
    underlyingNode: Node?,
) :
    MachineLearning(
        dataLocation,
        creation_time,
        description,
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
        other is MachineLearningModel &&
            super.equals(other) &&
            other.adv_robustness == this.adv_robustness &&
            other.explainability == this.explainability &&
            other.poisonLevel == this.poisonLevel &&
            other.privacyLabel == this.privacyLabel &&
            other.privacyLevel == this.privacyLevel &&
            other.robustness == this.robustness &&
            other.vulnerabilities == this.vulnerabilities

    override fun hashCode(): Int =
        Objects.hash(
            super.hashCode(),
            adv_robustness,
            explainability,
            poisonLevel,
            privacyLabel,
            privacyLevel,
            robustness,
            vulnerabilities,
        )
}
