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

import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation

/**
 * Represents a policy that can be applied to a resource or a set of resources. Policies can contain
 * rules and operations that define how access to the resource is controlled.
 */
class Policy() : Concept() {}

/**
 * Represents a rule that is part of a policy. This can be used to define conditions that must be
 * met for the policy to be enforced.
 */
abstract class PolicyRule(val policy: Policy) : Concept()

/**
 * Represents an operation that is part of a policy. This can be used to define rules or checks that
 * need to be performed to enforce the policy.
 */
abstract class PolicyOperation(val policy: Policy) : Operation(concept = policy)

class AndRule(policy: Policy) : PolicyRule(policy)

/**
 * Represents a principal that is allowed to access a resource. This can for example be a (structure
 * representing) a user or a group of users.
 */
class Principal() : Concept()

/** Represents an operation that checks whether two principals are equal. */
class EqualityCheck(policy: Policy, val left: Principal, val right: Principal) :
    PolicyOperation(policy)

/** Represents an operation that checks whether a principal is in a specific group. */
class IsInCheck(policy: Policy, val principal: Principal, val group: Principal)
