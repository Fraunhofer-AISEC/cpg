/*
 * Copyright (c) 2020, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.declarations

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.scopes.Symbol
import de.fraunhofer.aisec.cpg.persistence.DoNotPersist
import org.neo4j.ogm.annotation.NodeEntity

/**
 * Represents a single declaration or definition, i.e. of a variable ([VariableDeclaration]) or
 * function ([FunctionDeclaration]).
 *
 * Note: We do NOT (currently) distinguish between the definition and the declaration of a function.
 * This means, that if a function is first declared and later defined with a function body, we will
 * currently have two [FunctionDeclaration] nodes. This is very similar to the behaviour of clang,
 * however clang does establish a connection between those nodes, we currently do not.
 */
@NodeEntity
abstract class Declaration : Node() {
    @DoNotPersist
    val symbol: Symbol
        get() {
            return this.name.localName
        }

    override fun getExitNextEOG(): Collection<Node> {
        return setOf()
    }

    override fun getStartingPrevEOG(): Collection<Node> {
        return setOf()
    }
}
