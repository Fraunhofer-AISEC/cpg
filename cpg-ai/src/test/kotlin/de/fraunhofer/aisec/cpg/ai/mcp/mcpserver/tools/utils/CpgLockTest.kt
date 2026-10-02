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
package de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils

import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CpgLockTest {

    private fun <T> withPool(threads: Int, body: (java.util.concurrent.ExecutorService) -> T): T {
        val pool = Executors.newFixedThreadPool(threads)
        try {
            return body(pool)
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun readersRunConcurrently() =
        withPool(2) { pool ->
            val barrier = CyclicBarrier(2)
            // Each reader only gets past the barrier if the other is inside read {} at the same
            // time.
            val inside =
                List(2) {
                    pool.submit<Boolean> {
                        CpgLock.read {
                            try {
                                barrier.await(5, TimeUnit.SECONDS)
                                true
                            } catch (_: TimeoutException) {
                                false
                            }
                        }
                    }
                }
            assertTrue(inside.all { it.get(10, TimeUnit.SECONDS) })
        }

    @Test
    fun writerExcludesReaders() =
        withPool(2) { pool ->
            val writerInside = CountDownLatch(1)
            val releaseWriter = CountDownLatch(1)
            val readerEntered = AtomicBoolean(false)

            val writer =
                pool.submit {
                    CpgLock.write {
                        writerInside.countDown()
                        releaseWriter.await(10, TimeUnit.SECONDS)
                    }
                }
            assertTrue(writerInside.await(5, TimeUnit.SECONDS))
            val reader = pool.submit { CpgLock.read { readerEntered.set(true) } }

            Thread.sleep(300)
            assertFalse(readerEntered.get(), "reader must wait for the running writer")

            releaseWriter.countDown()
            writer.get(5, TimeUnit.SECONDS)
            reader.get(5, TimeUnit.SECONDS)
            assertTrue(readerEntered.get())
        }

    @Test
    fun readerExcludesWriter() =
        withPool(2) { pool ->
            val readerInside = CountDownLatch(1)
            val releaseReader = CountDownLatch(1)
            val writerEntered = AtomicBoolean(false)

            val reader =
                pool.submit {
                    CpgLock.read {
                        readerInside.countDown()
                        releaseReader.await(10, TimeUnit.SECONDS)
                    }
                }
            assertTrue(readerInside.await(5, TimeUnit.SECONDS))
            val writer = pool.submit { CpgLock.write { writerEntered.set(true) } }

            Thread.sleep(300)
            assertFalse(writerEntered.get(), "writer must wait for the running reader")

            releaseReader.countDown()
            reader.get(5, TimeUnit.SECONDS)
            writer.get(5, TimeUnit.SECONDS)
            assertTrue(writerEntered.get())
        }

    @Test
    fun writeIsReentrantAndMayDowngradeToRead() {
        val result = CpgLock.write { CpgLock.write { CpgLock.read { "ok" } } }
        assertEquals("ok", result)
    }

    @Test
    fun upgradingFromReadToWriteFailsInsteadOfDeadlocking() {
        CpgLock.read { assertFailsWith<IllegalStateException> { CpgLock.write {} } }
    }

    @Test
    fun lockIsReleasedWhenTheBlockThrows() =
        withPool(1) { pool ->
            assertFailsWith<IllegalStateException> { CpgLock.write { error("boom") } }
            assertFailsWith<IllegalStateException> { CpgLock.read { error("boom") } }
            // Another thread can take the exclusive lock, so neither failure leaked a hold.
            assertEquals(
                "ok",
                pool.submit<String> { CpgLock.write { "ok" } }.get(5, TimeUnit.SECONDS),
            )
        }
}
