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
package de.fraunhofer.aisec.cpg.graph.concepts.api /// *
// * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// *
// *                    $$$$$$\  $$$$$$$\   $$$$$$\
// *                   $$  __$$\ $$  __$$\ $$  __$$\
// *                   $$ /  \__|$$ |  $$ |$$ /  \__|
// *                   $$ |      $$$$$$$  |$$ |$$$$\
// *                   $$ |      $$  ____/ $$ |\_$$ |
// *                   $$ |  $$\ $$ |      $$ |  $$ |
// *                   \$$$$$   |$$ |      \$$$$$   |
// *                    \______/ \__|       \______/
// *
// */
// package de.fraunhofer.aisec.cpg.graph.concepts.api
//
// import de.fraunhofer.aisec.cpg.graph.MetadataProvider
// import de.fraunhofer.aisec.cpg.graph.Node
// import de.fraunhofer.aisec.cpg.graph.NodeBuilder
// import de.fraunhofer.aisec.cpg.graph.codeAndLocationFrom
// import kotlin.collections.forEach
//
// fun MetadataProvider.newRestApiConcept(
//    underlyingNode: Node,
//    apiUrl: String,
//    role: ApiRole,
// ): RestApiConcept {
//    val restApi = RestApiConcept(underlyingNode = underlyingNode, apiUrl = apiUrl, role = role)
//    restApi.underlyingNode = underlyingNode
//    restApi.codeAndLocationFrom(underlyingNode)
//
//    NodeBuilder.log(restApi)
//    return restApi
// }
//
// fun MetadataProvider.newRestApiOperation(
//    underlyingNode: Node,
//    httpMethod: String,
//    arguments: List<Node>,
//    concept: RestApiConcept,
// ): RestApiOperation {
//    val apiOperation =
//        RestApiOperation(
//            underlyingNode = underlyingNode,
//            concept = concept,
//            httpMethod =
//                when (httpMethod) {
//                    "_get",
//                    "show",
//                    "get" -> HttpMethod.GET
//
//                    "_create",
//                    "create",
//                    "post" -> HttpMethod.POST
//
//                    "_update",
//                    "update",
//                    "put" -> HttpMethod.PUT
//
//                    "_delete",
//                    "delete" -> HttpMethod.DELETE
//
//                    else -> HttpMethod.UNKNOWN
//                },
//            arguments = arguments,
//        )
//    apiOperation.underlyingNode = underlyingNode
//    apiOperation.codeAndLocationFrom(underlyingNode)
//
//    concept.ops += apiOperation
//
//    arguments.forEach { arg -> arg.nextDFG += apiOperation }
//
//    NodeBuilder.log(apiOperation)
//    return apiOperation
// }
