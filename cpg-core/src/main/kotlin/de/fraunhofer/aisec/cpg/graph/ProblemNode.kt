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

/**
 * Interface for a node representing some kind of failure during the translation or while parsing.
 */
interface ProblemNode {

    /** Type of problem which occurred during the translation. */
    enum class ProblemType {
        /** The CPG cannot handle the statement */
        TRANSLATION,
        /**
         * The library failed to parse the statement (probably a problem of the code under analysis)
         */
        PARSING,
    }

    /** A short description of the issue. */
    var problem: String
    /**
     * The type of the problem: Either the statement could not be parsed or the kind of statement is
     * not handled by the CPG yet. See [ProblemType]
     */
    var problemType: ProblemType
}
