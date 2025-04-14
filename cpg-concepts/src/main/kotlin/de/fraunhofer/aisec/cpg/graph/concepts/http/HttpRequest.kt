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
package de.fraunhofer.aisec.cpg.graph.concepts.http

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects

/** Represents an [HttpRequest] from the [HttpClient]. */
class HttpRequest(
    underlyingNode: Node? = null,
    val url: String,
    val arguments: List<Node>,
    val httpMethod: HttpMethod,
    override val concept: HttpClient,
) : HttpClientOperation(underlyingNode = underlyingNode, concept = concept) {
    val to = mutableListOf<HttpEndpoint>()

    override fun equals(other: Any?): Boolean {
        return other is HttpRequest &&
            super.equals(other) &&
            other.url == this.url &&
            other.arguments == this.arguments &&
            other.httpMethod == this.httpMethod &&
            other.to == this.to
    }

    override fun hashCode() = Objects.hash(super.hashCode(), url, arguments, httpMethod, to)
}
