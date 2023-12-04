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
package de.fraunhofer.aisec.cpg.frontends.openqasm

import de.fraunhofer.aisec.cpg.frontends.HasShortCircuitOperators
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.types.*
import kotlin.reflect.KClass
import org.neo4j.ogm.annotation.Transient

/** The OpenQASM language. */
class OpenQasmLanguage : Language<OpenQasmLanguageFrontend>(), HasShortCircuitOperators {
    override val fileExtensions = listOf("qasm")
    override val namespaceDelimiter = "."
    @Transient
    override val frontend: KClass<out OpenQasmLanguageFrontend> = OpenQasmLanguageFrontend::class
    override val conjunctiveOperators = listOf("and")
    override val disjunctiveOperators = listOf("or")

    /** TODO */
    @Transient override val builtInTypes = emptyMap<String, Type>()
    override val compoundAssignmentOperators: Set<String>
        get() = emptySet() // TODO
}
