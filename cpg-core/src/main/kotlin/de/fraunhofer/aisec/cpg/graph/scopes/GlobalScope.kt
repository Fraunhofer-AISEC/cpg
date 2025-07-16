/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.scopes

import de.fraunhofer.aisec.cpg.ScopeManager
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.dNodes

/**
 * This should ideally only be called once. It constructs a new global scope, which is not
 * associated to any AST node. However, depending on the language, a language frontend can
 * explicitly set the ast node using [ScopeManager.resetToGlobal] if the language needs a global
 * scope that is restricted to a translation unit, i.e. C++ while still maintaining a unique list of
 * global variables.
 */
class GlobalScope() : Scope(null) {

    /**
     * Because the way we currently handle parallel parsing in [TranslationManager.parseParallel],
     * we end up with multiple [GlobalScope] objects, one for each [TranslationUnitDeclaration]. In
     * the end, we need to merge all these different scopes into one final global scope. To be
     * somewhat consistent with the behaviour of [TranslationManager.parseSequentially], we assign
     * the *last* translation unit declaration we see to the AST node of the [GlobalScope]. This is
     * not completely ideal, but the best we can do for now.
     */
    fun mergeFrom(others: Collection<GlobalScope>) {
        for (other in others) {
            typedefs.putAll(other.typedefs)

            // Make sure, the child scopes of the global scope point to the new global scope parent
            // (this)
            for (child in other.children) {
                child.parent = this
                children.add(child)
            }

            // Merge symbols lists
            symbols.mergeFrom(other.symbols)
            wildcardImports.addAll(other.wildcardImports)

            for (node in other.astNode?.dNodes ?: listOf()) {
                if (node.scope is GlobalScope) {
                    node.scope = this
                }
            }
        }

        // We set the AST node of the global scope to the last declaration we see (this might not be
        // 100 % deterministic).
        this.astNode = others.lastOrNull()?.astNode
    }
}
