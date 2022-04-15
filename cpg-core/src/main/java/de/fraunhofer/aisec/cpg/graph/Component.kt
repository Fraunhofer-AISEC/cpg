/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration

/**
 * A node which presents some kind of complete piece of software, e.g., an application, a library,
 * microservice, ...
 *
 * This node holds all translation units belonging to this software component as well as (potential)
 * entry points or interactions with other software.
 */
open class Component : Node() {
    /** All translation units belonging to this application. */
    @field:SubGraph("AST")
    val translationUnits: MutableList<TranslationUnitDeclaration> = mutableListOf()

    /**
     * All points where unknown data may enter this application, e.g., the main method, or other
     * targets such as listeners to external events such as HTTP requests. This also includes the
     * list of possible entry points.
     */
    val incomingInteractions: MutableList<Node> = mutableListOf()

    /** All outgoing interactions such as sending data to the network or some kind of IPC. */
    val outgoingInteractions: MutableList<Node> = mutableListOf()
}
