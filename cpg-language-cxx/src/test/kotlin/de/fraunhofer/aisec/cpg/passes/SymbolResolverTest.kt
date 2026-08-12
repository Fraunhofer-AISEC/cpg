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

import de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage
import de.fraunhofer.aisec.cpg.frontends.cxx.CPPLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.expressions.UnaryOperator
import de.fraunhofer.aisec.cpg.graph.types.BooleanType
import de.fraunhofer.aisec.cpg.test.analyze
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SymbolResolverTest {

    @Test
    fun testExternVariables() {
        val file = File("src/test/resources/two_files/file.c")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), usePasses = true) {
                it.registerLanguage<CLanguage>()
                it.configurePass<SymbolResolver>(
                    SymbolResolver.Configuration(experimentalEOGWorklist = true)
                )
                it.disableTypeObserver()
            }
        assertEquals(1, result.variables.filter { it.name.localName == "x" }.size)
    }

    @Test
    fun testExternVariablesReversedOrder() {
        val file = File("src/test/resources/cxx/extern/reversed_order.c")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), usePasses = true) {
                it.registerLanguage<CLanguage>()
                it.configurePass<SymbolResolver>(
                    SymbolResolver.Configuration(experimentalEOGWorklist = true)
                )
                it.disableTypeObserver()
            }
        val variables = result.variables.filter { it.name.localName == "x" }
        assertEquals(1, variables.size)
        assertNotNull(variables.single().initializer)
    }

    @Test
    fun testExternVariablesTentativeDefinition() {
        val file = File("src/test/resources/cxx/extern/tentative_definition.c")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), usePasses = true) {
                it.registerLanguage<CLanguage>()
                it.configurePass<SymbolResolver>(
                    SymbolResolver.Configuration(experimentalEOGWorklist = true)
                )
                it.disableTypeObserver()
            }
        val variables = result.variables.filter { it.name.localName == "x" }
        assertEquals(1, variables.size)
        assertNotNull(variables.single().initializer)
        result.refs { it.name.localName == "x" }.forEach { assertNotNull(it.refersTo) }
    }

    @Test
    fun testExternVariablesWithInitializer() {
        val file = File("src/test/resources/cxx/extern/extern_with_initializer.c")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), usePasses = true) {
                it.registerLanguage<CLanguage>()
                it.configurePass<SymbolResolver>(
                    SymbolResolver.Configuration(experimentalEOGWorklist = true)
                )
                it.disableTypeObserver()
            }
        val variables = result.variables.filter { it.name.localName == "x" }
        assertEquals(1, variables.size)
        assertNotNull(variables.single().initializer)
    }

    @Test
    fun testExternVariablesCrossTranslationUnit() {
        val topLevel = File("src/test/resources/cxx/extern/cross_tu")
        val files = listOf(File(topLevel, "decl_only.c"), File(topLevel, "def.c"))
        val result =
            analyze(files, topLevel.toPath(), usePasses = true) {
                it.registerLanguage<CLanguage>()
                it.configurePass<SymbolResolver>(
                    SymbolResolver.Configuration(experimentalEOGWorklist = true)
                )
                it.disableTypeObserver()
            }
        val ref = result.refs.singleOrNull { it.name.localName == "x" }
        assertNotNull(ref, "expected exactly one reference to 'x' in decl_only.c")
        val resolved = ref.refersTo
        assertNotNull(
            resolved,
            "reference to 'x' declared extern in one TU and defined in " + "another should resolve",
        )
        assertNotNull((resolved as? Variable)?.initializer)
    }

    @Test
    fun testOnlyVariables() {
        val file = File("src/test/resources/cxx/symbols/only_variables.cpp")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), usePasses = true) {
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

        val unaryOp = result.allChildren<UnaryOperator>().firstOrNull()
        assertNotNull(unaryOp)
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
