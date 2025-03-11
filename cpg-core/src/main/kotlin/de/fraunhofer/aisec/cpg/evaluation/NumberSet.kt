/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.evaluation

import de.fraunhofer.aisec.cpg.graph.Node
import org.apache.commons.lang3.builder.ToStringBuilder

interface NumberSet {
    fun min(): Long

    fun max(): Long

    fun addValue(value: Long)

    fun maybe(value: Long): Boolean

    fun clear()
}

class Interval : NumberSet {
    private var min: Long = Long.MAX_VALUE
    private var max: Long = Long.MIN_VALUE

    override fun addValue(value: Long) {
        if (value < min) {
            min = value
        }
        if (value > max) {
            max = value
        }
    }

    override fun min(): Long {
        return min
    }

    override fun max(): Long {
        return max
    }

    override fun maybe(value: Long): Boolean {
        return value in min..max
    }

    override fun clear() {
        min = Long.MAX_VALUE
        max = Long.MIN_VALUE
    }
}

class ConcreteNumberSet(var values: MutableSet<Long> = mutableSetOf()) : NumberSet {
    override fun addValue(value: Long) {
        values.add(value)
    }

    override fun min(): Long {
        return values.minOrNull() ?: Long.MAX_VALUE
    }

    override fun max(): Long {
        return values.maxOrNull() ?: Long.MIN_VALUE
    }

    override fun maybe(value: Long): Boolean {
        return value in values
    }

    override fun clear() {
        values.clear()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConcreteNumberSet

        return values == other.values
    }

    override fun hashCode(): Int {
        return values.hashCode()
    }

    override fun toString(): String {
        return ToStringBuilder(this, Node.TO_STRING_STYLE).append(values).toString()
    }
}
