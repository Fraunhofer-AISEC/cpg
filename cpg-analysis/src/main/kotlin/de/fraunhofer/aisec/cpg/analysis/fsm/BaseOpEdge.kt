/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.analysis.fsm

/**
 * Represents an edge of the automaton. The edge label consists of an operation (typically a method
 * name) and a base which allows us to differentiate between multiple objects.
 */
class BaseOpEdge(val op: String, val base: String?, val nextState: State) {

    fun matches(base: String?, op: String): Boolean {
        return this.base == base && this.op == op
    }

    override fun toString(): String {
        return if (base != null) "-- $base.$op --> $nextState" else "-- $op --> $nextState"
    }

    override fun equals(other: Any?): Boolean {
        if (other !is BaseOpEdge) return false
        return this.op == other.op && this.base == other.base && this.nextState == other.nextState
    }

    fun toDotLabel(): String {
        return if (base != null) "$base.$op" else op
    }

    override fun hashCode(): Int {
        var result = op.hashCode()
        result = 31 * result + (base?.hashCode() ?: 0)
        result = 31 * result + nextState.hashCode()
        return result
    }
}
