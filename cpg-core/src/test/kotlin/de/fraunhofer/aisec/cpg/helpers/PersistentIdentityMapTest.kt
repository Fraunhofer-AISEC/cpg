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
package de.fraunhofer.aisec.cpg.helpers

import de.fraunhofer.aisec.cpg.helpers.functional.PersistentIdentityMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PersistentIdentityMapTest {

    /** A key which is `equals` to every other key with the same [name], but not identical to it. */
    private data class Key(val name: String)

    @Test
    fun testKeysAreComparedByIdentity() {
        val first = Key("a")
        val second = Key("a")
        assertEquals(first, second, "The test only makes sense if the two keys are equal")

        val map = PersistentIdentityMap<Key, Int>()
        map.put(first, 1)
        map.put(second, 2)

        assertEquals(2, map.size, "Two distinct objects have to occupy two entries")
        assertEquals(1, map[first])
        assertEquals(2, map[second])
        assertTrue(map.containsKey(first))
        assertFalse(map.containsKey(Key("a")), "A third, unrelated object is not in the map")
    }

    @Test
    fun testPutAndRemoveReturnThePreviousValue() {
        val key = Key("a")
        val map = PersistentIdentityMap<Key, Int>()

        assertNull(map.put(key, 1), "There was no previous value")
        assertEquals(1, map.put(key, 2), "put has to return the value it replaced")
        assertEquals(2, map.remove(key), "remove has to return the value it removed")
        assertNull(map.remove(key), "There is nothing left to remove")
        assertTrue(map.isEmpty())
    }

    @Test
    fun testComputeAndComputeIfAbsent() {
        val key = Key("a")
        val map = PersistentIdentityMap<Key, Int>()

        assertEquals(1, map.computeIfAbsent(key) { 1 })
        assertEquals(1, map.computeIfAbsent(key) { 2 }, "The entry is already there")

        assertEquals(3, map.compute(key) { _, value -> (value ?: 0) + 2 })
        assertEquals(3, map[key])

        assertNull(map.compute(key) { _, _ -> null }, "Returning null removes the entry")
        assertFalse(map.containsKey(key))
    }

    @Test
    fun testRemoveKeyIf() {
        val keep = Key("keep")
        val drop = Key("drop")
        val map = PersistentIdentityMap<Key, Int>()
        map.putAll(listOf(keep to 1, drop to 2))

        assertTrue(map.removeKeyIf { it.name == "drop" })
        assertEquals(1, map.size)
        assertEquals(1, map[keep])
        assertFalse(map.removeKeyIf { it.name == "drop" }, "There is nothing left to remove")
    }

    @Test
    fun testViewsSeeTheCurrentContents() {
        val first = Key("a")
        val second = Key("b")
        val map = PersistentIdentityMap<Key, Int>()
        map.put(first, 1)

        val keys = map.keys
        val entries = map.entries
        map.put(second, 2)

        assertEquals(setOf(first, second), keys.toSet())
        assertEquals(setOf(first to 1, second to 2), entries.map { it.key to it.value }.toSet())

        @Suppress("UNCHECKED_CAST") val iterator = (keys as MutableSet<Key>).iterator()
        iterator.next()
        iterator.remove()
        assertEquals(1, map.size, "Removing through the key iterator has to reach the map")
    }

    @Test
    fun testHashCodeAndEqualsAreContentBased() {
        val keyA = Key("a")
        val keyB = Key("b")

        val map1 = PersistentIdentityMap<Key, Int>()
        map1.put(keyA, 1)
        map1.put(keyB, 2)

        val map2 = PersistentIdentityMap<Key, Int>()
        // Insert in a different order, to make sure that alone doesn't affect the result. Reuses
        // keyA/keyB rather than fresh, merely-equal Key instances: this map compares keys by
        // reference (testKeysAreComparedByIdentity), so two maps can only be equal if their keys
        // are references to the very same objects - exactly like ConcurrentMapLattice.Element
        // comparing two states that both refer to the same graph nodes.
        map2.put(keyB, 2)
        map2.put(keyA, 1)

        // This is the actual regression this test guards against: PersistentIdentityMap used to
        // inherit identity-based hashCode()/equals() from Any, which two separately-constructed but
        // content-equal maps would fail - breaking the Map contract and anything that hashes a map
        // (e.g. putting it in a HashSet, or a wrapper whose own hashCode() delegates to it, like
        // ConcurrentMapLattice.Element does).
        assertEquals(map1, map2)
        assertEquals(map1.hashCode(), map2.hashCode())

        map2.put(Key("c"), 3)
        assertFalse(map1 == map2, "A map with an extra entry must not be equal")
    }

    /**
     * A value which is `equals` to every other value with the same [content], but never the same
     * instance.
     */
    private data class Box(val content: String)

    @Test
    fun testPutStoresAnEqualButNotIdenticalValue() {
        // This pins an assumption the copy-on-write mechanism in ConcurrentMapLattice.Element
        // relies on (see its getForUpdate): put()/compute() must not short-circuit and keep the old
        // value just because the new one is `==` to it, or a caller which just replaced a value
        // with a fresh, private, but content-equal copy would find the old (possibly still shared)
        // instance still installed.
        val key = Key("a")
        val map = PersistentIdentityMap<Key, Box>()
        val original = Box("x")
        map.put(key, original)

        val equalButNotSame = Box("x")
        assertEquals(original, equalButNotSame)
        assertNotSame(original, equalButNotSame)

        map.put(key, equalButNotSame)
        assertSame(equalButNotSame, map[key])
    }

    @Test
    fun testKeyIteratorRemoveRejectsCallsWithoutAPrecedingNext() {
        val map = PersistentIdentityMap<Key, Int>()
        map.put(Key("a"), 1)

        @Suppress("UNCHECKED_CAST") val iterator = (map.keys as MutableSet<Key>).iterator()
        assertFailsWith<IllegalStateException> { iterator.remove() }

        iterator.next()
        iterator.remove()
        // A second remove() for the same element - without an intervening next() - must also be
        // rejected, not silently remove whatever key now happens to occupy the same slot.
        assertFailsWith<IllegalStateException> { iterator.remove() }
    }

    @Test
    fun testClearEmptiesTheMap() {
        val map = PersistentIdentityMap<Key, Int>()
        map.putAll(listOf(Key("a") to 1, Key("b") to 2))

        map.clear()

        assertTrue(map.isEmpty())
        assertEquals(0, map.size)
    }

    /**
     * The map is written from several threads at once, which is exactly what the compare-and-set
     * loops in [PersistentIdentityMap] are there for. None of the writes may get lost.
     */
    @Test
    fun testConcurrentWritesDoNotGetLost() {
        val threads = 8
        val perThread = 500
        val keys = List(threads * perThread) { Key("k$it") }
        val counter = Key("counter")
        val map = PersistentIdentityMap<Key, Int>()

        val pool = Executors.newFixedThreadPool(threads)
        try {
            val start = CountDownLatch(1)
            val done = CountDownLatch(threads)
            repeat(threads) { thread ->
                pool.submit {
                    start.await()
                    for (i in 0 until perThread) {
                        val index = thread * perThread + i
                        map.put(keys[index], index)
                    }
                    // Every thread also counts up the very same entry, so these updates have to be
                    // serialized by the retry loop instead of overwriting each other.
                    repeat(perThread) { map.compute(counter) { _, value -> (value ?: 0) + 1 } }
                    done.countDown()
                }
            }
            start.countDown()
            assertTrue(done.await(60, TimeUnit.SECONDS), "The writers did not finish in time")
        } finally {
            pool.shutdownNow()
        }

        assertEquals(keys.size + 1, map.size, "Every key has to be in the map exactly once")
        keys.forEachIndexed { index, key ->
            assertEquals(index, map[key], "Lost the write for $key")
        }
        assertEquals(
            threads * perThread,
            map[counter],
            "Every increment of the contended entry has to be visible",
        )
    }
}
