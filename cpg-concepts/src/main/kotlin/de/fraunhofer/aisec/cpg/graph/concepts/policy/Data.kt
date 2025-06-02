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
import de.fraunhofer.aisec.cpg.graph.scopes.Scope

/**
 * Represents an asset that is protected by a policy. This can be an in-memory data structure, a
 * file, a database, or any other resource that requires access control.
 */
class ProtectedAsset(scope: Scope?) : Concept()

/**
 * Base class for operations that involve protected assets. These operations can include checks for
 * access permissions, modifications, or any other actions that require validation against a policy.
 */
abstract class ProtectedAssetOperation(asset: ProtectedAsset) : Operation(concept = asset)

/** Represents an operation that checks whether a user or principal has access to a protected */
class CheckAccess(asset: ProtectedAsset, predicate: Predicate) : ProtectedAssetOperation(asset)

/**
 * Base class for predicates used in an [CheckAccess] operation. Predicates define the conditions
 * that must be met for access to be granted.
 */
open class Predicate()

/**
 * Represents a logical AND operation between two nodes. This can be used to combine multiple
 * conditions that must all be true for access to be granted.
 */
class Equals(var left: Node, var right: Node) : Predicate()

/**
 * Represents a check to see if an element is part of a group. This can be used to verify if a
 * principal belongs to a specific group or role within the policy.
 */
class IsIn(var element: Node, var group: Node) : Predicate()
