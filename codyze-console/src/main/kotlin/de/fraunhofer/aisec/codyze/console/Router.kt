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

import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.listOverlayClasses
import io.ktor.http.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlin.reflect.KClass

/**
 * This function sets up the API routes for the web application. It defines the endpoints for
 * analyzing code, reanalyzing the last project, getting the translation result, and retrieving
 * components and translation units.
 *
 * The routes are defined using the Ktor routing DSL. Each route handles a specific HTTP method and
 * path, and the corresponding handler function is called when a request matches that route. The
 * functionality itself is then handled in the [ConsoleService].
 *
 * The following endpoints are defined:
 * - POST `/api/analyze`: Analyzes a project based on the provided request.
 * - POST `/api/reanalyze`: Reanalyzes the last project.
 * - GET `/api/result`: Retrieves the translation result.
 * - GET `/api/component/{name}`: Retrieves a component by name.
 * - GET `/api/component/{component_name}/translation-unit/{id}`: Retrieves a translation unit by
 *   ID.
 * - GET `/api/component/{component_name}/translation-unit/{id}/ast-nodes`: Retrieves all AST nodes
 *   for a translation unit.
 * - GET `/api/component/{component_name}/translation-unit/{id}/overlay-nodes`: Retrieves all
 *   overlay nodes for a translation unit.
 * - GET `/api/classes/concepts`: Retrieves a list of all available [Concept] classes (as Java class
 *   names).
 * - POST `/api/concept`: Adds a concept node to the current
 *   [de.fraunhofer.aisec.codyze.AnalysisResult]
 */
fun Routing.apiRoutes(service: ConsoleService) {
    // The API routes are prefixed with /api
    route("/api") {
        // The endpoint to analyze a project
        post("/analyze") {
            val request = call.receive<AnalyzeRequestJSON>()
            try {
                val result = service.analyze(request)
                call.respond(result)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // The endpoint to reanalyze the last project
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

        // The endpoint to get the translation result
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

        get("/project") {
            // This endpoint returns the last project as a JSON object
            val lastProject = service.lastProject
            if (lastProject != null) {
                call.respond(lastProject.toJSON())
            } else {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "No project has been analyzed yet"),
                )
            }
        }

        // The endpoint to get a component by name
        get("/component/{name}") {
            val name = call.parameters["name"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val component = service.getComponent(name)

            if (component != null) {
                call.respond(component)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Component not found"))
            }
        }

        // The endpoint to get a translation unit by ID
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

        // The endpoint to get all AST nodes for a translation unit
        get("/component/{component_name}/translation-unit/{id}/ast-nodes") {
            val componentName =
                call.parameters["component_name"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)

            val nodes = service.getNodesForTranslationUnit(componentName, id, false)
            call.respond(nodes)
        }

        // The endpoint to get all overlay nodes for a translation unit
        get("/component/{component_name}/translation-unit/{id}/overlay-nodes") {
            val componentName =
                call.parameters["component_name"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)

            val nodes = service.getNodesForTranslationUnit(componentName, id, true)
            call.respond(nodes)
        }

        // The endpoint to add a concept node to the current analysis result
        post("/concepts") {
            try {
                val request = call.receive<ConceptRequestJSON>()
                try {
                    call.respond(service.addConcept(request))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Invalid request format: ${e.message}"),
                )
            }
        }

        /**
         * The endpoint to get a list of all available [Concept] classes. Returns a JSON object with
         * an array of concept names (Java class names).
         */
        get("/classes/concepts") {
            val conceptClasses = listOverlayClasses<Concept>()
            call.respond(
                mapOf("info" to conceptClasses.map { it.kotlin.getConstructorArguments() })
            )
        }

        // The endpoint to get a single requirement by ID
        get("/requirement/{requirementId}") {
            val requirementId =
                call.parameters["requirementId"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Missing requirement ID"),
                    )

            val requirement = service.getRequirement(requirementId)
            if (requirement != null) {
                call.respond(requirement)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Requirement not found"))
            }
        }

        /**
         * The endpoint to export a YAML listing of all manually added [Concept]s (via `POST
         * /concept`).
         */
        get("/export-concepts") { call.respond(service.exportPersistedConcepts()) }
    }
}

/**
 * This function sets up the static resources for the web application. It serves the static files
 * from the "static" directory and sets the default resource to "index.html". This serves our SPA
 * frontend.
 *
 * In order to allow deep-links in our frontend to work, we need to serve the index.html file for
 * all paths that do not really exist so that our SPA can handle the routing.
 */
fun Routing.frontendRoutes() {
    staticResources("/", "static", "index.html") { default("index.html") }
}

/**
 * This function retrieves the constructor arguments of a [KClass]. It returns a list of triples,
 * where each triple contains the name, type, and whether the argument is optional.
 *
 * @return A list of triples containing the constructor argument names, types, and optionality. If
 *   multiple constructors exist, returns an empty list.
 */
fun KClass<*>.getConstructorArguments(): ConceptInfo {
    return ConceptInfo(
        this.java.name,
        this.constructors.singleOrNull()?.parameters?.mapNotNull {
            it.name?.let { name -> ConstructorInfo(name, it.type.toString(), it.isOptional) }
        } ?: listOf(),
    )
}
