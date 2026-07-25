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
package de.fraunhofer.aisec.cpg.frontends.php

import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.IntStream
import org.antlr.v4.runtime.misc.Interval

/**
 * A CharStream wrapper that exposes lower-cased characters through LA for case-insensitive lexing.
 */
class CaseChangingCharStream(private val stream: CharStream) : CharStream {
    override fun consume() = stream.consume()

    override fun LA(i: Int): Int {
        val c = stream.LA(i)
        if (c == 0 || c == IntStream.EOF) {
            return c
        }
        return c.toChar().lowercaseChar().code
    }

    override fun mark(): Int = stream.mark()

    override fun release(marker: Int) = stream.release(marker)

    override fun index(): Int = stream.index()

    override fun seek(index: Int) = stream.seek(index)

    override fun size(): Int = stream.size()

    override fun getSourceName(): String = stream.sourceName

    override fun getText(interval: Interval): String = stream.getText(interval)
}
