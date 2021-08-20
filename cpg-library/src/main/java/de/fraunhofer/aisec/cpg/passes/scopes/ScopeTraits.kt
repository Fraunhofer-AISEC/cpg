/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.graph.statements.BreakStatement
import de.fraunhofer.aisec.cpg.graph.statements.ContinueStatement

interface ScopeTraits

/** Represents scopes that can be interrupted by a [BreakStatement]. */
interface Breakable : ScopeTraits {
    fun addBreakStatement(breakStatement: BreakStatement?)
    val breakStatements: List<BreakStatement?>?
}

/** Represents scopes that can be interrupted by a [ContinueStatement]. */
interface Continuable : ScopeTraits {
    fun addContinueStatement(continueStatement: ContinueStatement)

    val continueStatements: List<ContinueStatement>
}

fun Scope.isBreakable(): Boolean {
    return this is LoopScope || this is SwitchScope
}

fun Scope.isContinuable(): Boolean {
    return this is LoopScope
}
