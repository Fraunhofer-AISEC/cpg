/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.statements

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.newLookupScopeStatement
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.graph.scopes.Symbol
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import java.util.Objects

/**
 * This statement modifies the lookup scope of one or more [Reference] nodes (or more precise it's
 * symbols) within the current [Scope]. The most prominent example of this are the Python `global`
 * and `nonlocal` keywords.
 *
 * This node itself does not implement the actual functionality. It is necessary to add this node
 * (or the information therein) to [Scope.predefinedLookupScopes]. The reason for this is that we
 * want to avoid AST traversals in the scope/identifier lookup.
 *
 * The [newLookupScopeStatement] node builder will add this automatically, so it is STRONGLY
 * encouraged that the node builder is used instead of creating the node itself.
 */
class LookupScopeStatement internal constructor(ctx: TranslationContext) : Statement(ctx) {

    /** The symbols this statement affects. */
    var symbols: List<Symbol> = listOf()

    /** The target scope to which the references are referring to. */
    var targetScope: Scope? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LookupScopeStatement) return false
        return super.equals(other) && symbols == other.symbols && targetScope == other.targetScope
    }

    override fun hashCode() = Objects.hash(super.hashCode(), symbols, targetScope)
}
