/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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

import java.time.Duration
import java.time.Instant
import org.slf4j.LoggerFactory

open class Benchmark
@JvmOverloads
constructor(c: Class<*>, private val message: String, private var debug: Boolean = false) {

    private val caller: String
    private val start: Instant

    var duration: Long
        private set

    fun stop(): Long {
        duration = Duration.between(start, Instant.now()).toMillis()

        val msg = "$caller: $message done in $duration ms"

        if (debug) {
            log.debug(msg)
        } else {
            log.info(msg)
        }

        return duration
    }

    companion object {
        private val log = LoggerFactory.getLogger(Benchmark::class.java)
    }

    init {
        this.duration = -1
        caller = c.simpleName
        start = Instant.now()

        val msg = "$caller: $message"

        if (debug) {
            log.debug(msg)
        } else {
            log.info(msg)
        }
    }
}
