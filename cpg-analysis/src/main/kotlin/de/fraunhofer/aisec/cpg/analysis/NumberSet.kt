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
package de.fraunhofer.aisec.cpg.analysis

abstract class NumberSet {
    abstract fun min(): Long
    abstract fun max(): Long
    abstract fun addValue(value: Long)
    abstract fun clear()
}

class Interval : NumberSet() {
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
    override fun clear() {
        min = Long.MAX_VALUE
        max = Long.MIN_VALUE
    }
}

class ConcreteNumberSet(var values: MutableSet<Long> = mutableSetOf()) : NumberSet() {
    override fun addValue(value: Long) {
        values.add(value)
    }
    override fun min(): Long {
        return values.minOrNull()!!
    }
    override fun max(): Long {
        return values.maxOrNull()!!
    }
    override fun clear() {
        values.clear()
    }
}
