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
 * This service discloses security advisories, e.g. according to the CSAF standard. It has one or
 * more feeds that contain the actual advisories as well as multiple (public) keys that are used to
 * sign the advisory documents.
 */
public abstract class SecurityAdvisoryService(
    public val securityAdvisoryFeeds: MutableList<SecurityAdvisoryFeed?>,
    public val selectorKeies: MutableList<SelectorKey?>,
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
        other is SecurityAdvisoryService &&
            super.equals(other) &&
            other.securityAdvisoryFeeds == this.securityAdvisoryFeeds &&
            other.selectorKeies == this.selectorKeies

    override fun hashCode(): Int =
        Objects.hash(super.hashCode(), securityAdvisoryFeeds, selectorKeies)
}
