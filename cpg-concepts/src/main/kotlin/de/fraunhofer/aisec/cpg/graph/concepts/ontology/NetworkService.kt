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
import kotlin.Array
import kotlin.Boolean
import kotlin.Int
import kotlin.Short
import kotlin.String
import kotlin.collections.MutableList
import kotlin.collections.MutableMap

/**
 * A NetworkService is an application (on the network layer) running on a Compute resource. It
 * provides access to a resource
 */
public open class NetworkService(
    public val ips: Array<String>?,
    public val ports: Array<Short>?,
    public val authenticity: Boolean?,
    public val computes: MutableList<Compute?>,
    public val serviceMetadataDocument: ServiceMetadataDocument?,
    public val transportEncryption: TransportEncryption?,
    internetAccessibleEndpoint: Boolean?,
    geoLocation: GeoLocation?,
    loggings: MutableList<Logging?>,
    redundancies: Redundancy?,
    usageStatistics: UsageStatistics?,
    creation_time: ZonedDateTime?,
    description: String?,
    labels: MutableMap<String, String>?,
    name: String?,
    raw: String?,
    parent: Resource?,
    underlyingNode: Node? = null,
) :
    Networking(
        internetAccessibleEndpoint,
        geoLocation,
        loggings,
        redundancies,
        usageStatistics,
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
        other is NetworkService &&
            super.equals(other) &&
            other.ips == this.ips &&
            other.ports == this.ports &&
            other.authenticity == this.authenticity &&
            other.computes == this.computes &&
            other.serviceMetadataDocument == this.serviceMetadataDocument &&
            other.transportEncryption == this.transportEncryption

    override fun hashCode(): Int =
        Objects.hash(
            super.hashCode(),
            ips,
            ports,
            authenticity,
            computes,
            serviceMetadataDocument,
            transportEncryption,
        )
}
