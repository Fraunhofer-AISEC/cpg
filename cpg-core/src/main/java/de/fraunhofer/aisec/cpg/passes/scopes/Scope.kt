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
package de.fraunhofer.aisec.cpg.passes.scopes

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.statements.LabelStatement

/**
 * Represent semantic scopes in the language. Depending on the language scopes can have visibility
 * restriction and can act as namespaces to avoid name collisions.
 */
abstract class Scope(open var astNode: Node?) {

    // FQN Name currently valid
    var scopedName: String? = null

    /* Scopes are nested and therefore have a parent child relationship, this two members will help
    navigate through the scopes,e.g. when looking up variables */
    var parent: Scope? = null
    var children = mutableListOf<Scope>()
    var labelStatements = mutableMapOf<String, LabelStatement>()

    fun addLabelStatement(labelStatement: LabelStatement) {
        labelStatements[labelStatement.label] = labelStatement
    }

    fun isBreakable(): Boolean {
        return this is LoopScope || this is SwitchScope
    }

    fun isContinuable(): Boolean {
        return this is LoopScope
    }

    /** Returns the [GlobalScope] of this scope by traversing its parents upwards. */
    val globalScope: Scope?
        get() {
            var scope: Scope? = this
            while (scope !is GlobalScope) {
                if (scope == null) {
                    return null
                }

                scope = scope.parent
            }

            return scope
        }
}
