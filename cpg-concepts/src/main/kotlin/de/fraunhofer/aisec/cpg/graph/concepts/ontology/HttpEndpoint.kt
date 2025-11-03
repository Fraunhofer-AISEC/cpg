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

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String

/**
 * An HTTP endpoint can set the "proxyTarget" property, in case that is routed through a (reverse)
 * proxy, e.g. a load balancer.
 */
public abstract class HttpEndpoint(
    public val handler: String?,
    public val method: String?,
    public val path: String?,
    public val url: String?,
    public val authenticity: Authenticity?,
    public val authorization: Authorization?,
    public val httpRequestContext: HttpRequestContext?,
    public val proxyTarget: HttpEndpoint?,
    public val transportEncryption: TransportEncryption?,
    underlyingNode: Node?,
) : RemoteEntryPoint(underlyingNode) {
    override fun equals(other: Any?): Boolean =
        other is HttpEndpoint &&
            super.equals(other) &&
            other.handler == this.handler &&
            other.method == this.method &&
            other.path == this.path &&
            other.url == this.url &&
            other.authenticity == this.authenticity &&
            other.authorization == this.authorization &&
            other.httpRequestContext == this.httpRequestContext &&
            other.proxyTarget == this.proxyTarget &&
            other.transportEncryption == this.transportEncryption

    override fun hashCode(): Int =
        Objects.hash(
            super.hashCode(),
            handler,
            method,
            path,
            url,
            authenticity,
            authorization,
            httpRequestContext,
            proxyTarget,
            transportEncryption,
        )
}
