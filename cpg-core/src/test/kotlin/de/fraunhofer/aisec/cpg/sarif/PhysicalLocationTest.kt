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
package de.fraunhofer.aisec.cpg.sarif

import java.net.URI
import java.util.Objects
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

class PhysicalLocationTest {
    @Test
    fun testRegionRoundTrip() {
        val location =
            PhysicalLocation(
                URI("file:///a.c"),
                Region(startLine = 1, startColumn = 2, endLine = 3, endColumn = 4),
            )

        assertEquals(Region(1, 2, 3, 4), location.region)
    }

    @Test
    fun testRegionGetterReturnsFreshInstanceEachTime() {
        // The region is materialized on demand rather than stored as a dedicated object, so
        // repeated reads are equal but not the same instance.
        val location = PhysicalLocation(URI("file:///a.c"), Region(1, 2, 3, 4))

        assertNotSame(location.region, location.region)
        assertEquals(location.region, location.region)
    }

    @Test
    fun testRegionSetterReplacesWholesale() {
        val location = PhysicalLocation(URI("file:///a.c"), Region(1, 2, 3, 4))

        location.region = Region(5, 6, 7, 8)

        assertEquals(Region(5, 6, 7, 8), location.region)
    }

    @Test
    fun testEqualsAndHashCode() {
        val a = PhysicalLocation(URI("file:///a.c"), Region(1, 2, 3, 4))
        val b = PhysicalLocation(URI("file:///a.c"), Region(1, 2, 3, 4))
        val differentRegion = PhysicalLocation(URI("file:///a.c"), Region(1, 2, 3, 5))
        val differentFile = PhysicalLocation(URI("file:///b.c"), Region(1, 2, 3, 4))

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assert(a != differentRegion)
        assert(a != differentFile)
    }

    @Test
    fun testHashCodeMatchesPreFlattenedFormula() {
        // hashCode must stay byte-for-byte identical to the pre-flattening
        // Objects.hash(artifactLocation, region) formula, since Node.id (used for persistence) is
        // derived from it transitively.
        val location = PhysicalLocation(URI("file:///a.c"), Region(1, 2, 3, 4))

        val expected = Objects.hash(location.artifactLocation, Region(1, 2, 3, 4))

        assertEquals(expected, location.hashCode())
    }

    @Test
    fun testToString() {
        val location = PhysicalLocation(URI("file:///a.c"), Region(1, 2, 3, 4))

        assertEquals("a.c(1:2-3:4)", location.toString())
    }
}
