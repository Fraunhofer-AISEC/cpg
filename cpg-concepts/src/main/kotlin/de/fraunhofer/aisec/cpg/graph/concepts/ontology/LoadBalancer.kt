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

/** A Load Balancer may have multiple access restriction features, e.g. a L3 firewall and a WAF */
public abstract class LoadBalancer(
    public val url: String?,
    public val accessRestriction: Boolean?,
    public val httpEndpoints: MutableList<HttpEndpoint?>,
    public val networkServices: MutableList<NetworkService?>,
    ips: Array<String>?,
    ports: Array<Short>?,
    authenticity: Boolean?,
    computes: MutableList<Compute?>,
    serviceMetadataDocument: ServiceMetadataDocument?,
    transportEncryption: TransportEncryption?,
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
    underlyingNode: Node?,
) :
    NetworkService(
        ips,
        ports,
        authenticity,
        computes,
        serviceMetadataDocument,
        transportEncryption,
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
        other is LoadBalancer &&
            super.equals(other) &&
            other.url == this.url &&
            other.accessRestriction == this.accessRestriction &&
            other.httpEndpoints == this.httpEndpoints &&
            other.networkServices == this.networkServices

    override fun hashCode(): Int =
        Objects.hash(super.hashCode(), url, accessRestriction, httpEndpoints, networkServices)
}
