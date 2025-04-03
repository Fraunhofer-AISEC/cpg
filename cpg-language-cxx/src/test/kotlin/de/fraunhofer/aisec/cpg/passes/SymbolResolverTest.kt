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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.frontends.cxx.CPPLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.types.BooleanType
import de.fraunhofer.aisec.cpg.test.analyze
import java.io.File
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SymbolResolverTest {
    @Test
    fun testOnlyVariables() {
        val file = File("src/test/resources/cxx/symbols/only_variables.cpp")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), usePasses = false) {
                it.registerLanguage<CPPLanguage>()
                it.configurePass<SymbolResolver>(
                    SymbolResolver.Configuration(experimentalEOGWorklist = true)
                )
                it.disableTypeObserver()
            }
        assertNotNull(result)
        result.refs.forEach { assertNotNull(it.refersTo, "$it should not have an empty refersTo") }

        val ifCondition = result.ifs.firstOrNull()?.condition
        assertNotNull(ifCondition)
        assertIs<BooleanType>(ifCondition.type, "Type of if condition should be BooleanType")
    }

    @Test
    fun testMemberCalls() {
        val file = File("src/test/resources/cxx/symbols/member_calls.cpp")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), usePasses = true) {
                it.registerLanguage<CPPLanguage>()
                it.configurePass<SymbolResolver>(
                    SymbolResolver.Configuration(experimentalEOGWorklist = true)
                )
                it.disableTypeObserver()
            }
        assertNotNull(result)
        result.refs.forEach { assertNotNull(it.refersTo) }
        result.mcalls.forEach { assertTrue(it.invokes.isNotEmpty()) }
    }

    @Test
    fun testSimpleCalls() {
        val file = File("src/test/resources/cxx/symbols/simple_calls.cpp")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), usePasses = true) {
                it.registerLanguage<CPPLanguage>()
                it.configurePass<SymbolResolver>(
                    SymbolResolver.Configuration(experimentalEOGWorklist = true)
                )
                it.disableTypeObserver()
            }
        assertNotNull(result)
        result.refs.forEach { assertNotNull(it.refersTo) }
        result.calls.forEach { assertTrue(it.invokes.isNotEmpty()) }
    }
}
