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
import kotlin.test.assertFalse
import kotlin.test.assertNull
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
