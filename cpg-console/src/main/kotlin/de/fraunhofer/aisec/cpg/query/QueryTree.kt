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
package de.fraunhofer.aisec.cpg.query

import de.fraunhofer.aisec.cpg.graph.compareTo

open class QueryTree<T>(
    open var value: T,
    open val children: MutableList<QueryTree<*>> = mutableListOf(),
    open val stringRepresentation: String = ""
) {
    fun printNicely(depth: Int = 0): String {
        // TODO: Make this output awesome
        var res =
            "  ".repeat(depth) +
                "$stringRepresentation (==> $value)\n" +
                "--------".repeat(depth) +
                "\n"
        children.forEach { res += it.toString() + "\n" + "--------".repeat(depth + 1) + "\n" }
        return res
    }

    infix fun eq(other: QueryTree<T>): QueryTree<Boolean> {
        val result = this.value == other.value
        return QueryTree(result, mutableListOf(this, other), "$this == $other")
    }

    infix fun neq(other: QueryTree<T>): QueryTree<Boolean> {
        val result = this.value != other.value
        return QueryTree(result, mutableListOf(this, other), "$this == $other")
    }

    override fun hashCode(): Int {
        return value?.hashCode() ?: 0
    }

    override fun equals(other: Any?): Boolean {
        if (other is QueryTree<*>) {
            return this.value?.equals(other.value) ?: false
        }

        return super.equals(other)
    }
}

infix fun QueryTree<Boolean>.and(other: QueryTree<Boolean>): QueryTree<Boolean> {
    return QueryTree(
        this.value && other.value,
        mutableListOf(this, other),
        stringRepresentation = "${this.value} && ${other.value}"
    )
}

infix fun QueryTree<Boolean>.or(other: QueryTree<Boolean>): QueryTree<Boolean> {
    return QueryTree(
        this.value || other.value,
        mutableListOf(this, other),
        stringRepresentation = "${this.value} || ${other.value}"
    )
}

infix fun QueryTree<Boolean>.xor(other: QueryTree<Boolean>): QueryTree<Boolean> {
    return QueryTree(
        this.value xor other.value,
        mutableListOf(this, other),
        stringRepresentation = "${this.value} xor ${other.value}"
    )
}

infix fun QueryTree<Boolean>.implies(other: QueryTree<Boolean>): QueryTree<Boolean> {
    return QueryTree(
        !this.value || other.value,
        mutableListOf(this, other),
        stringRepresentation = "${this.value} => ${other.value}"
    )
}

infix fun QueryTree<Number>.gt(other: QueryTree<Number>): QueryTree<Boolean> {
    val result = this.value.compareTo(other.value) > 0
    return QueryTree(result, mutableListOf(this, other), "${this.value} > ${other.value}")
}

infix fun QueryTree<Number>.ge(other: QueryTree<Number>): QueryTree<Boolean> {
    val result = this.value.compareTo(other.value) >= 0
    return QueryTree(result, mutableListOf(this, other), "${this.value} >= ${other.value}")
}

infix fun QueryTree<Number>.lt(other: QueryTree<Number>): QueryTree<Boolean> {
    val result = this.value.compareTo(other.value) < 0
    return QueryTree(result, mutableListOf(this, other), "${this.value} < ${other.value}")
}

infix fun QueryTree<Number>.le(other: QueryTree<Number>): QueryTree<Boolean> {
    val result = this.value.compareTo(other.value) <= 0
    return QueryTree(result, mutableListOf(this, other), "${this.value} <= ${other.value}")
}

/**
 * This is a small wrapper to create a [QueryTreeHolder] containing a constant value, so that it can
 * be used to in comparison with other [QueryTreeHolder] objects.
 */
fun <T> const(n: T): QueryTree<T> {
    return QueryTree(n, stringRepresentation = "$n")
}
