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
package de.fraunhofer.aisec.cpg.analysis.string

import de.fraunhofer.aisec.cpg.helpers.functional.Order
import kotlin.test.*
import kotlinx.coroutines.runBlocking

class StringLatticeTest {
    private val lattice = StringLattice()

    private val terms =
        listOf(
            StringPattern.Bottom,
            const("foo"),
            const("bar"),
            concat(const("foo"), const("bar")),
            union(const("foo"), const("bar")),
            StringPattern.Unknown(
                charSet = charsOf('a', 'b', 'c'),
                reason = StringPattern.Reason.UNSUPPORTED,
            ),
            StringPattern.Unknown(reason = StringPattern.Reason.PARAMETER),
        )

    @Test
    fun testLubCommutative() = runBlocking {
        for (a in terms) {
            for (b in terms) {
                assertEquals(
                    lattice.lub(a, b),
                    lattice.lub(b, a),
                    "lub($a, $b) should be commutative",
                )
            }
        }
    }

    @Test
    fun testLubIdempotent() = runBlocking {
        for (a in terms) {
            assertEquals(a, lattice.lub(a, a), "lub($a, $a) should be idempotent")
        }
    }

    @Test
    fun testLubAbsorbsBottom() = runBlocking {
        for (a in terms) {
            assertEquals(a, lattice.lub(StringPattern.Bottom, a))
            assertEquals(a, lattice.lub(a, StringPattern.Bottom))
        }
    }

    @Test
    fun testGlbAbsorbsBottom() = runBlocking {
        for (a in terms) {
            assertEquals(StringPattern.Bottom, lattice.glb(StringPattern.Bottom, a))
            assertEquals(StringPattern.Bottom, lattice.glb(a, StringPattern.Bottom))
        }
    }

    @Test
    fun testGlbIdempotent() = runBlocking {
        for (a in terms) {
            assertEquals(a, lattice.glb(a, a))
        }
    }

    @Test
    fun testGlbOfDifferentConstsIsBottom() = runBlocking {
        assertEquals(StringPattern.Bottom, lattice.glb(const("foo"), const("bar")))
    }

    @Test
    fun testGlbOfConstAndAdmittingUnknownIsConst() = runBlocking {
        val unknown =
            StringPattern.Unknown(charSet = CharSet.Any, reason = StringPattern.Reason.UNSUPPORTED)
        assertEquals(const("foo"), lattice.glb(const("foo"), unknown))
        assertEquals(const("foo"), lattice.glb(unknown, const("foo")))
    }

    @Test
    fun testCompareConsistentWithLubAndGlb() = runBlocking {
        val a = const("foo")
        val b = union(const("foo"), const("bar"))
        // a is more specific than b (b's language is a superset), so b should be GREATER.
        assertEquals(Order.LESSER, lattice.compare(a, b))
        assertEquals(Order.GREATER, lattice.compare(b, a))
        assertEquals(b, lattice.lub(a, b))
        assertEquals(a, lattice.glb(a, b))
    }

    @Test
    fun testCompareEqualForStructurallyEqualTerms() {
        assertEquals(Order.EQUAL, lattice.compare(const("foo"), const("foo")))
        assertEquals(
            Order.EQUAL,
            lattice.compare(union(const("a"), const("b")), union(const("b"), const("a"))),
        )
    }

    @Test
    fun testCompareUnequalForIncomparableTerms() {
        assertEquals(Order.UNEQUAL, lattice.compare(const("foo"), const("bar")))
    }

    @Test
    fun testDuplicateIsIdentity() {
        val a = union(const("foo"), const("bar"))
        assertSame(a, lattice.duplicate(a))
    }
}
