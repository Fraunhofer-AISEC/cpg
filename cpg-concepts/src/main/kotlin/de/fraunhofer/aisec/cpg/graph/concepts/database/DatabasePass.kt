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
package de.fraunhofer.aisec.cpg.graph.concepts.database

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.calls
import de.fraunhofer.aisec.cpg.graph.followPrevDFGEdgesUntilHit
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.passes.ComponentPass
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteLate

@ExecuteLate
class DatabasePass(ctx: TranslationContext) : ComponentPass(ctx) {
    override fun cleanup() {
        // nop
    }

    override fun accept(comp: Component) {
        comp.calls.filter { it.name.lastPartsMatch("SQLAlchemy") }.forEach { handleSQLAlchemy(it) }

        comp.calls.filter { it.name.lastPartsMatch("add") }.forEach { handleAdd(it) }
    }

    private fun handleAdd(addCall: CallExpression) {
        when (addCall) {
            is MemberCallExpression -> {
                val base = addCall.base
                // TODO localName == session
                when (base) {
                    is MemberExpression -> {
                        val baseBase = base.base
                        val db =
                            baseBase
                                .followPrevDFGEdgesUntilHit { // walk DFg backwards until you find a
                                    // node that has on overlay of type
                                    // [Database]
                                    it.overlays.filterIsInstance<Database>().isNotEmpty()
                                }
                                .fulfilled // we are only interested in successful DFG paths
                                .singleOrNull() // TODO: handle multiple paths
                                ?.last() // the last node in the path -> the node connected to the
                                // [Database] node
                                ?.overlays // it's overlay nodes
                                ?.filterIsInstance<Database>()
                                ?.singleOrNull() // TODO: handle multiple overlays
                        db?.let {
                            newDatabaseAdd(
                                addCall,
                                it,
                                what =
                                    addCall.arguments
                                        .firstOrNull(), // TODO handle multiple arguments
                            )
                        }
                    }
                }
            }
        }
    }

    private fun handleSQLAlchemy(db: CallExpression) {
        newDatabase(underlyingNode = db)
    }
}
