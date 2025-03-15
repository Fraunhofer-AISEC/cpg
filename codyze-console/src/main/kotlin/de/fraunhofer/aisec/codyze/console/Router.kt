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
package de.fraunhofer.aisec.codyze.console

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.defaultResource
import io.ktor.server.http.content.resources
import io.ktor.server.http.content.static
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

enum class AnalysisMode {
    TRANSLATION_ONLY,
    ANALYZE_WITH_GOALS,
}

@Serializable
data class AnalyzeRequest(
    val sourceDir: String,
    val includeDir: String? = null,
    val topLevel: String? = null,
    val analysisModel: AnalysisMode = AnalysisMode.TRANSLATION_ONLY,
)

fun Routing.cpgRoutes(service: ConsoleService) {
    route("/api") {
        post("/analyze") {
            val request = call.receive<AnalyzeRequest>()
            try {
                val result = service.analyze(request)
                call.respond(result)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        post("/reanalyze") {
            val lastProject = service.lastProject
            if (lastProject != null) {
                try {
                    val result = service.analyzeProject(lastProject)
                    call.respond(result)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                }
            } else {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "No previous configuration to regenerate from"),
                )
            }
        }

        get("/result") {
            val result = service.getTranslationResult()
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
            val component = service.getComponent(name)

            if (component != null) {
                call.respond(component)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Component not found"))
            }
        }

        get("/component/{component_name}/translation-unit/{id}") {
            val componentName =
                call.parameters["component_name"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)

            val tu = service.getTranslationUnit(componentName, id)
            if (tu != null) {
                call.respond(tu)
            } else {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Translation unit not found"),
                )
            }
        }

        get("/component/{component_name}/translation-unit/{id}/ast-nodes") {
            val componentName =
                call.parameters["component_name"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)

            val nodes = service.getNodesForTranslationUnit(componentName, id, false)
            call.respond(nodes)
        }

        get("/component/{component_name}/translation-unit/{id}/overlay-nodes") {
            val componentName =
                call.parameters["component_name"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)

            val nodes = service.getNodesForTranslationUnit(componentName, id, true)
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
