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
package de.fraunhofer.aisec.cpg.graph.scopes

import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.LookupScopeStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import kotlin.test.Test
import kotlin.test.assertEquals

class ScopeTest {
    @Test
    fun testLookup() {
        // some mock variable declarations, global and local
        var globalA = VariableDeclaration()
        globalA.name = Name("a")
        var localA = VariableDeclaration()
        localA.name = Name("a")

        // two scopes, global and local
        val globalScope = GlobalScope()
        globalScope.addSymbol("a", globalA)
        val scope = BlockScope(Block())
        scope.parent = globalScope
        scope.addSymbol("a", localA)

        // if we try to resolve "a" now, this should point to the local A since we start there and
        // move upwards
        var result = scope.lookupSymbol("a")
        assertEquals(listOf(localA), result)

        // now, we pretend to have a lookup scope modifier for a symbol, e.g. through "global" in
        // Python
        var stmt = LookupScopeStatement()
        stmt.targetScope = globalScope
        stmt.symbols = listOf("a")
        scope.predefinedLookupScopes["a"] = stmt

        // let's try the lookup again, this time it should point to the global A
        result = scope.lookupSymbol("a")
        assertEquals(listOf(globalA), result)
    }
}