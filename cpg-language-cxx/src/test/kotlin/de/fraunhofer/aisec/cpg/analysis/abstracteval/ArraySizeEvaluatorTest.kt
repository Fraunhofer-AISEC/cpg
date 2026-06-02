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
package de.fraunhofer.aisec.cpg.analysis.abstracteval

import de.fraunhofer.aisec.cpg.analysis.abstracteval.value.ArraySizeEvaluator
import de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArraySizeEvaluatorTest {
    private lateinit var tu: TranslationUnit

    @BeforeTest
    fun setUp() {
        val file = File("src/test/resources/arraysize/arraysize.c")
        tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
            }
    }

    /** `char buf[8]` — size carried by the [ArrayConstruction] dimension. */
    @Test
    fun testFixedBuffer() {
        val buf = tu.functions["fixed_buffer"]?.variables?.get("buf")
        assertNotNull(buf)
        assertEquals(LatticeInterval.Bounded(8, 8), ArraySizeEvaluator().evaluate(buf))
    }

    /** `char *p = malloc(64)` — size derived from the constant `malloc` argument. */
    @Test
    fun testHeapAlloc() {
        val p = tu.functions["heap_alloc"]?.variables?.get("p")
        assertNotNull(p)
        assertEquals(LatticeInterval.Bounded(64, 64), ArraySizeEvaluator().evaluate(p))
    }

    /** `char *s = "hello"` — size derived from the string literal initializer's length. */
    @Test
    fun testStringLiteralInitializer() {
        val s = tu.functions["string_literal"]?.variables?.get("s")
        assertNotNull(s)
        assertEquals(LatticeInterval.Bounded(5, 5), ArraySizeEvaluator().evaluate(s))
    }

    /**
     * Branch-merged allocation: the lattice join of the two malloc sizes is the only correct
     * answer. The shape-shortcut can't see this — only the full interval analysis can.
     */
    @Test
    fun testBoundedAllocRange() {
        val func = tu.functions["bounded_alloc"]
        assertNotNull(func)
        val buf = func.variables["buf"]
        assertNotNull(buf)
        assertEquals(LatticeInterval.Bounded(16, 64), ArraySizeEvaluator().evaluate(buf))
    }
}
