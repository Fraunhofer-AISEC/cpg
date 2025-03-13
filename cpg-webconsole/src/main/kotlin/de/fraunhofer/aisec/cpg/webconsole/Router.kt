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
package de.fraunhofer.aisec.cpg.webconsole

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.defaultResource
import io.ktor.server.http.content.resources
import io.ktor.server.http.content.static
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable data class GenerateCPGRequest(val sourceDir: String)

private val cpgService = CPGService()

fun Routing.cpgRoutes() {
    route("/api") {
        post("/generate") {
            val request = call.receive<GenerateCPGRequest>()
            try {
                val result = cpgService.generateCPG(request.sourceDir)
                call.respond(result)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        get("/result") {
            val result = cpgService.getTranslationResult()
            if (result != null) {
                call.respond(result)
            } else {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "No CPG has been generated yet"),
                )
            }
        }

        get("/component/{name}") {
            val name = call.parameters["name"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val component = cpgService.getComponent(name)

            if (component != null) {
                call.respond(component)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Component not found"))
            }
        }

        get("/translationUnit") {
            val componentName =
                call.request.queryParameters["component"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
            val path =
                call.request.queryParameters["path"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest)

            val tu = cpgService.getTranslationUnit(componentName, path)
            if (tu != null) {
                call.respond(tu)
            } else {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Translation unit not found"),
                )
            }
        }

        get("/nodes") {
            val componentName =
                call.request.queryParameters["component"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
            val path =
                call.request.queryParameters["path"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest)

            val nodes = cpgService.getNodesForTranslationUnit(componentName, path)
            call.respond(nodes)
        }
    }
}

fun Routing.staticResources() {
    static("/") {
        resources("static")
        defaultResource("static/index.html")
    }
}
