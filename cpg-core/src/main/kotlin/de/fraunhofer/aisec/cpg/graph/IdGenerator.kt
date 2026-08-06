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
package de.fraunhofer.aisec.cpg.graph

import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

/** A simple ID generator based on atomic longs. */
@OptIn(ExperimentalAtomicApi::class)
sealed class IdGenerator {

    /**
     * The current ID. This is an atomic long to ensure thread safety when generating IDs in
     * parallel. It starts at 0 and is incremented by 1 for each new ID. The [next] function
     */
    val current: AtomicLong = AtomicLong(0)

    /** Returns the next ID. */
    fun next(): Long {
        return current.incrementAndFetch()
    }
}

/**
 * An [IdGenerator] for [Node]s. The IDs are generated as [Long]s starting at 0 and incremented by 1
 * for each new ID.
 */
object NodeIdGenerator : IdGenerator()
