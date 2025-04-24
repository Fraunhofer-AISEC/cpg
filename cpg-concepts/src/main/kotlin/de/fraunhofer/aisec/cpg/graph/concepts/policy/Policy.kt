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
package de.fraunhofer.aisec.cpg.graph.concepts.policy

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation

class Policy(underlyingNode: Node?) : Concept(underlyingNode) {}

abstract class PolicyRule(underlyingNode: Node?, val policy: Policy) : Concept(underlyingNode)

abstract class PolicyOperation(underlyingNode: Node?, val policy: Policy): Operation(underlyingNode, policy)

class AndRule(underlyingNode: Node?, policy: Policy) : PolicyRule(underlyingNode, policy)

/**
 * Represents a principal that is allowed to access a resource. This can for example be a (structure
 * representing) a user or a group of users.
 */
class Principal(underlyingNode: Node?) : Concept(underlyingNode)

class EqualityCheck(
    underlyingNode: Node?,
    policy: Policy,
    val left: Principal,
    val right: Principal,
) : PolicyOperation(underlyingNode,policy)

class IsInCheck(
    underlyingNode: Node?,
    policy: Policy,
    val principal: Principal,
    val group: Principal,
)
