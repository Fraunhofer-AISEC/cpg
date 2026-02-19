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
package de.fraunhofer.aisec.cpg.concepts

import de.fraunhofer.aisec.cpg.concepts.PwTag.Registry.httprequests
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage
import de.fraunhofer.aisec.cpg.graph.Forward
import de.fraunhofer.aisec.cpg.graph.GraphToFollow
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.codeAndLocationFrom
import de.fraunhofer.aisec.cpg.graph.conceptNodes
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.http.HttpClient
import de.fraunhofer.aisec.cpg.graph.concepts.http.HttpEndpoint
import de.fraunhofer.aisec.cpg.graph.concepts.http.HttpMethod
import de.fraunhofer.aisec.cpg.graph.concepts.http.HttpRequest
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.evaluate
import de.fraunhofer.aisec.cpg.graph.refs
import de.fraunhofer.aisec.cpg.graph.statements.expressions.AssignExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.passes.concepts.TagOverlaysPass
import de.fraunhofer.aisec.cpg.passes.concepts.TaggingContext
import de.fraunhofer.aisec.cpg.passes.concepts.each
import de.fraunhofer.aisec.cpg.passes.concepts.tag
import de.fraunhofer.aisec.cpg.passes.concepts.with
import de.fraunhofer.aisec.cpg.query.allExtended
import de.fraunhofer.aisec.cpg.query.dataFlow
import de.fraunhofer.aisec.cpg.query.value
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertNotNull

class PwTag {

    object Registry {
        var httprequests = mutableListOf<HttpRequest>()
    }

    @Test
    fun testPwServer() {
        val topLevel = Path.of("..", "cpg-language-python", "src", "test", "resources", "python")
        val tu =
            analyze(listOf(topLevel.resolve("Passwordmanager").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>().matchCommentsToNodes(true)
                it.registerPass<TagOverlaysPass>()
                it.configurePass<TagOverlaysPass>(
                    TagOverlaysPass.Configuration(
                        tag =
                            tag {
                                tagUser()
                                tagTaint()
                                tagHTTPRequests()
                                tagDBExecute()
                                tagHttp()
                                tagPassword()
                                tagFernetencrypt()
                                tagPersonaldata()
                            }
                    )
                )
            }

        assertNotNull(tu)
        tu.conceptNodes.filterIsInstance<HttpEndpoint>().forEach { endpoint: HttpEndpoint ->
            endpoint.matchingRequest()
        }

        val users = tu.refs.filter { it.overlays.any { o -> o is User } }
        val existsPath =
            users.any { u ->
                dataFlow(
                        startNode = u,
                        direction = Forward(GraphToFollow.DFG),
                        predicate = { n ->
                            n.overlays.filterIsInstance<PostRequest>().any { pr ->
                                pr.url.contains("/register")
                            }
                        },
                    )
                    .value
            }
        println(existsPath)

        val query =
            tu.allExtended<User>(
                mustSatisfy = { User ->
                    dataFlow(
                        startNode = User.underlyingNode!!,
                        direction = Forward(GraphToFollow.DFG),
                        predicate = {
                            val postReq = it.overlays.filterIsInstance<PostRequest>().firstOrNull()
                            postReq != null && postReq.url.contains("/register")
                        },
                    )
                }
            )

        val querytaint =
            tu.allExtended<User>(
                mustSatisfy = { input ->
                    dataFlow(
                        startNode = input.underlyingNode!!,
                        direction = Forward(GraphToFollow.DFG),
                        predicate = {
                            it.astParent?.overlays?.filterIsInstance<HttpRequest>()?.isNotEmpty() ==
                                true
                        },
                    )
                }
            )

        val querytaintpost =
            tu.allExtended<HttpEndpoint>(
                mustSatisfy = { input ->
                    dataFlow(
                        startNode = input.underlyingNode!!,
                        direction = Forward(GraphToFollow.DFG),
                        predicate = { it.overlays.filterIsInstance<HttpEndpoint>().isNotEmpty() },
                    )
                }
            )

        val queryHttpRequests =
            tu.allExtended<User>(
                mustSatisfy = { input ->
                    dataFlow(
                        startNode = input.underlyingNode!!,
                        direction = Forward(GraphToFollow.DFG),
                        predicate = {
                            it.astParent?.overlays?.filterIsInstance<HttpRequest>()?.isNotEmpty() ==
                                true
                        },
                    )
                }
            )

        val queryPersonaldatainHTTPRequestffffail =
            tu.allExtended<Personaldata>(
                mustSatisfy = { data ->
                    dataFlow(
                        startNode = data.underlyingNode!!,
                        direction = Forward(GraphToFollow.DFG),
                        predicate = { it.overlays.filterIsInstance<HttpRequest>().isNotEmpty() },
                    )
                }
            )

        val queryPersonaldatainHTTPRequest =
            tu.allExtended<Personaldata>(
                mustSatisfy = { data ->
                    dataFlow(
                        startNode = (data.underlyingNode as? AssignExpression)!!.lhs.first(),
                        direction = Forward(GraphToFollow.DFG),
                        predicate = {
                            // it.overlays.filterIsInstance<DBExe>().isNotEmpty()
                            it.astParent?.overlays?.filterIsInstance<HttpRequest>()?.isNotEmpty() ==
                                true
                        },
                    )
                }
            )

        // ----------------------------------------------------------------------
        val queryPersonaldatainDBExe =
            tu.allExtended<Personaldata>(
                mustSatisfy = { data ->
                    dataFlow(
                        startNode = (data.underlyingNode as? AssignExpression)!!.lhs.first(),
                        direction = Forward(GraphToFollow.DFG),
                        predicate = {
                            val req =
                                it.astParent
                                    ?.overlays
                                    ?.filterIsInstance<HttpRequest>()
                                    ?.firstOrNull()

                            if (req != null) {
                                httprequests += req
                                true
                            } else {
                                false
                            }
                        },
                    )
                }
            )
        val pdEndpoints: Set<HttpEndpoint> =
            httprequests.flatMap { it.to }.filterIsInstance<HttpEndpoint>().toSet()

        val queryPdEndpointsToDB =
            tu.allExtended<HttpEndpoint>(
                mustSatisfy = { ep ->
                    dataFlow(
                        startNode = ep.underlyingNode!!,
                        direction = Forward(GraphToFollow.EOG),
                        predicate = { n ->
                            (ep in pdEndpoints) &&
                                (n.astParent?.overlays?.filterIsInstance<DBExe>()?.isNotEmpty() ==
                                    true)
                        },
                    )
                }
            )

        val pdEndpointsToDB: List<HttpEndpoint> =
            pdEndpoints.filter { ep ->
                dataFlow(
                        startNode = ep.underlyingNode!!,
                        direction = Forward(GraphToFollow.EOG),
                        predicate = { n ->
                            n.overlays.filterIsInstance<DBExe>().isNotEmpty() ||
                                n.astParent?.overlays?.filterIsInstance<DBExe>()?.isNotEmpty() ==
                                    true
                        },
                    )
                    .value
            }

        val pdEndpointsToDBexists: Boolean =
            pdEndpoints.any { ep ->
                dataFlow(
                        startNode = ep.underlyingNode!!,
                        direction = Forward(GraphToFollow.EOG),
                        predicate = { n ->
                            n.overlays.filterIsInstance<DBExe>().isNotEmpty() ||
                                n.astParent?.overlays?.filterIsInstance<DBExe>()?.isNotEmpty() ==
                                    true
                        },
                    )
                    .value
            }

        val queryPdEndpointsToPOST =
            tu.allExtended<HttpEndpoint>(
                mustSatisfy = { ep ->
                    dataFlow(
                        startNode = ep.underlyingNode!!,
                        direction = Forward(GraphToFollow.EOG),
                        predicate = { n ->
                            (ep in pdEndpoints) &&
                                (n.overlays.filterIsInstance<HttpRequest>().isNotEmpty() ||
                                    n.astParent
                                        ?.overlays
                                        ?.filterIsInstance<HttpRequest>()
                                        ?.isNotEmpty() == true)
                        },
                    )
                }
            )

        // ----------------------------------------------------------------------

        val queryfernet =
            tu.allExtended<Password>(
                mustSatisfy = { input ->
                    dataFlow(
                        startNode = input.underlyingNode!!,
                        direction = Forward(GraphToFollow.DFG),
                        predicate = { it.overlays.filterIsInstance<Fernet>().isNotEmpty() },
                    )
                }
            )
    }

    fun HttpEndpoint.matchingRequest() {
        createdHttpRequest.forEach { httpRequest ->
            val isMatch =
                httpRequest.url.contains(this.path) && httpRequest.httpMethod == this.httpMethod
            if (isMatch) {
                httpRequest.to += this
            }
        }
    }

    class User(underlyingNode: Node) : Concept(underlyingNode = underlyingNode)

    class Password(underlyingNode: Node) : Concept(underlyingNode = underlyingNode)

    class Fernet(underlyingNode: Node) : Concept(underlyingNode = underlyingNode)

    class Personaldata(underlyingNode: Node) : Concept(underlyingNode = underlyingNode)

    class PostRequest(underlyingNode: Node, val url: String) :
        Concept(underlyingNode = underlyingNode)

    class GetRequest(underlyingNode: Node) : Concept(underlyingNode = underlyingNode)

    class Userinput(underlyingNode: Node) : Concept(underlyingNode = underlyingNode)

    class DBExe(underlyingNode: Node) : Concept(underlyingNode = underlyingNode)

    fun TaggingContext.tagFernetencrypt() {
        each<MemberCallExpression>(predicate = { it.name.localName.contains("encrypt") }).with {
            Fernet(underlyingNode = node).apply {
                this.codeAndLocationFrom(node)
                this.name = Name(node.name.localName)
            }
        }
    }

    fun TaggingContext.tagPersonaldata() {
        each<AssignExpression>(predicate = { it.comment.toString().contains("@PersonalData") })
            .with {
                val ref = node.lhs.first()
                Personaldata(underlyingNode = ref).apply {
                    this.codeAndLocationFrom(ref)
                    this.name = Name(ref.name.localName)
                }
            }
    }

    fun TaggingContext.tagPassword() {
        each<Reference>(
                predicate = {
                    it.name.localName.contains("password") &&
                        it.location.toString().contains("client")
                }
            )
            .with {
                Password(underlyingNode = node).apply {
                    this.codeAndLocationFrom(node)
                    this.name = Name(node.name.localName)
                }
            }
    }

    fun TaggingContext.tagUser() {
        each<Reference>(predicate = { it.name.localName == "user" }).with {
            User(underlyingNode = node).apply {
                this.codeAndLocationFrom(node)
                this.name = Name(node.name.localName)
            }
        }
    }

    fun TaggingContext.tagHttp() {
        val httpMethods = setOf("get", "post", "delete")
        each<FunctionDeclaration>(
                predicate = {
                    it.annotations.any { annotation -> annotation.name.localName in httpMethods }
                }
            )
            .with {
                val path = node.annotations.first().members[0].value?.evaluate() as? String ?: ""
                val httpmethods = node.annotations.first().name.localName.asHttpMethod()
                val httpEndpoint =
                    HttpEndpoint(
                            underlyingNode = node,
                            httpMethod = httpmethods,
                            path = path,
                            arguments = node.parameters,
                            authentication = null,
                            authorization = null,
                            requestContext = null,
                        )
                        .apply {
                            this.codeAndLocationFrom(node)
                            this.name = Name(node.name.localName)
                        }
                httpEndpoint
            }
    }

    val createdHttpRequest = mutableListOf<HttpRequest>()

    fun TaggingContext.tagHTTPRequests() {
        val httpMethods = setOf("get", "post", "delete")

        each<CallExpression>(
                predicate = {
                    httpMethods.any { m -> it.name.localName.contains(m) } &&
                        it.name.contains("requests.") ||
                        it.name.parent.toString().contains("requests.")
                }
            )
            .with {
                val urlpath =
                    when (val url = this.node.arguments.firstOrNull()) {
                        is Literal<*> -> url.value
                        is Reference -> url.evaluate()
                        is BinaryOperator -> url.lhs.value.value
                        else -> {
                            null
                        }
                    }
                val httpmethods = node.name.localName.asHttpMethod()
                val httpRequest =
                    HttpRequest(
                            underlyingNode = node,
                            url = urlpath.toString(),
                            arguments = node.arguments,
                            httpMethod = httpmethods,
                            concept = HttpClient(underlyingNode = node, authentication = null),
                        )
                        .apply {
                            this.codeAndLocationFrom(node)
                            this.name = Name(node.name.localName)
                        }
                createdHttpRequest += httpRequest
                httpRequest
            }
    }

    fun String.asHttpMethod(): HttpMethod =
        when (this.lowercase()) {
            "get" -> HttpMethod.GET
            "post" -> HttpMethod.POST
            else -> HttpMethod.DELETE
        }

    fun TaggingContext.tagTaint() {
        each<Reference>(predicate = { it.name.localName == "input" }).with {
            Userinput(underlyingNode = node).apply {
                this.codeAndLocationFrom(node)
                this.name = Name(node.name.localName)
            }
        }
    }

    fun TaggingContext.tagDBExecute() {
        each<MemberCallExpression>(predicate = { it.name.localName == "execute" }).with {
            DBExe(underlyingNode = node).apply {
                this.codeAndLocationFrom(node)
                this.name = Name(node.name.localName)
            }
        }
    }
}
