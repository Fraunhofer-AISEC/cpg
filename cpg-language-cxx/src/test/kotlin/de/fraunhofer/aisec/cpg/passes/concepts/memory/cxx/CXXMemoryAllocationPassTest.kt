/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.passes.concepts.memory.cxx

import de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.concepts.memory.Allocate
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CXXMemoryAllocationPassTest {
    private lateinit var tu: TranslationUnit

    @BeforeTest
    fun setUp() {
        val file = File("src/test/resources/allocations/allocations.c")
        tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
            }
    }

    /**
     * Returns the [Allocate] operation attached to the first [Call] to [funcName] inside the
     * function [containerName]. Asserts presence.
     */
    private fun allocateIn(containerName: String, funcName: String): Allocate {
        val container = tu.functions[containerName]
        assertNotNull(container, "function '$containerName' must be present in the test TU")
        val call =
            container.calls.firstOrNull { it.name.toString() == funcName }
                ?: error("no call to '$funcName' inside '$containerName'")
        val allocate = call.overlays.filterIsInstance<Allocate>().singleOrNull()
        assertNotNull(allocate, "no Allocate overlay on $funcName call in $containerName")
        return allocate
    }

    /** `char *p = malloc(64)` → size is the literal 64, `what` is the variable. */
    @Test
    fun testMallocConstant() {
        val allocate = allocateIn("malloc_constant", "malloc")
        val sizeLit = assertIs<Literal<*>>(allocate.size)
        assertEquals(64L, (sizeLit.value as Number).toLong())
        assertIs<Variable>(allocate.what)
        assertEquals("p", (allocate.what as Variable).name.localName)
    }

    /** `p = malloc(128)` after a prior declaration → `what` resolves through the LHS Reference. */
    @Test
    fun testMallocViaAssign() {
        val allocate = allocateIn("malloc_via_assign", "malloc")
        val sizeLit = assertIs<Literal<*>>(allocate.size)
        assertEquals(128L, (sizeLit.value as Number).toLong())
        assertIs<Variable>(allocate.what)
        assertEquals("p", (allocate.what as Variable).name.localName)
    }

    /**
     * `calloc(8, sizeof(int))` → size is the synthesised `count * elemSize` [BinaryOperator]; the
     * raw count and elemSize are still the original Call arguments.
     */
    @Test
    fun testCallocSynthesisesProduct() {
        val allocate = allocateIn("calloc_constant", "calloc")
        val sizeBin = assertIs<BinaryOperator>(allocate.size)
        assertEquals("*", sizeBin.operatorCode)
        val countLit = assertIs<Literal<*>>(sizeBin.lhs)
        assertEquals(8L, (countLit.value as Number).toLong())
        // rhs is `sizeof(int)`; we don't constant-evaluate types here, just check the structure.
        assertNotNull(sizeBin.rhs)
        assertIs<Variable>(allocate.what)
        assertEquals("p", (allocate.what as Variable).name.localName)
    }

    /** `realloc(p, 32)` → size is argument 1 (the new size), not the old buffer. */
    @Test
    fun testReallocSize() {
        val allocate = allocateIn("realloc_constant", "realloc")
        val sizeLit = assertIs<Literal<*>>(allocate.size)
        assertEquals(32L, (sizeLit.value as Number).toLong())
        assertIs<Variable>(allocate.what)
        assertEquals("p", (allocate.what as Variable).name.localName)
    }

    /**
     * `malloc(n)` with a non-constant size → Allocate.size is the Reference to `n`. Downstream
     * consumers can choose to constant-evaluate, do interval analysis, or trace DFG from there.
     */
    @Test
    fun testMallocVariableSize() {
        val allocate = allocateIn("malloc_unknown_size", "malloc")
        val sizeRef = assertIs<Reference>(allocate.size)
        assertEquals("n", sizeRef.name.localName)
        assertIs<Variable>(allocate.what)
        assertEquals("p", (allocate.what as Variable).name.localName)
    }

    /** A non-allocator call (e.g. `free`) should not get an Allocate overlay. */
    @Test
    fun testNoOverlayForUnrelatedCall() {
        val unrelated =
            tu.calls.filter { it.name.toString() !in CXXMemoryAllocationPass.RECOGNIZED_ALLOCATORS }
        assertTrue(unrelated.isNotEmpty(), "fixture must contain at least one non-allocator call")
        unrelated.forEach { call ->
            assertNull(call.overlays.filterIsInstance<Allocate>().firstOrNull())
        }
    }
}
