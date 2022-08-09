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

/**
 * Holds the [value] to which the statements have been evaluated. The [children] define previous
 * steps of the evaluation, thus building a tree of all steps of the evaluation recursively until we
 * reach the nodes of the CPG.
 *
 * Numerous methods allow to evaluate the queries while keeping track of all the steps. Currently,
 * the following operations are supported:
 * - **eq**: Equality of two values.
 * - **ne**: Inequality of two values.
 * - **IN**: Checks if a value is contained in a [Collection]
 * - **IS**: Checks if a value implements a type ([Class]).
 *
 * Additionally, some functions are available only for certain types of values.
 *
 * For boolean values:
 * - **and**: Logical and operation (&&)
 * - **or**: Logical or operation (||)
 * - **xor**: Logical exclusive or operation (xor)
 * - **implies**: Logical implication
 *
 * For numeric values:
 * - **gt**: Grater than (>)
 * - **ge**: Grater than or equal (>=)
 * - **lt**: Less than (<)
 * - **le**: Less than or equal (<=)
 */
open class QueryTree<T>(
    open var value: T,
    open val children: MutableList<QueryTree<*>> = mutableListOf(),
    open var stringRepresentation: String = ""
) {
    fun printNicely(depth: Int = 0): String {
        var res =
            "  ".repeat(depth) +
                "$stringRepresentation (==> $value)\n" +
                "--------".repeat(depth + 1)
        if (children.isNotEmpty()) {
            res += "\n"
            children.forEach { c ->
                val next = c.printNicely(depth + 2)
                if (next.isNotEmpty()) res += next + "\n" + "--------".repeat(depth + 1) + "\n"
            }
        }
        return res
    }

    /** Checks for equality of two [QueryTree]s. */
    infix fun eq(other: QueryTree<T>): ComparableQueryTree<Boolean> {
        val result = this.value == other.value
        return ComparableQueryTree(
            result,
            mutableListOf(this, other),
            "${this.value} == ${other.value}"
        )
    }

    /**
     * Checks for equality of a [QueryTree] with a value of the same type (e.g. useful to check for
     * constants).
     */
    infix fun eq(other: T): ComparableQueryTree<Boolean> {
        val result = this.value == other
        return ComparableQueryTree(
            result,
            mutableListOf(this, QueryTree(other)),
            "${this.value} == $value"
        )
    }

    /** Checks for inequality of two [QueryTree]s. */
    infix fun ne(other: QueryTree<T>): ComparableQueryTree<Boolean> {
        val result = this.value != other.value
        return ComparableQueryTree(
            result,
            mutableListOf(this, other),
            "${this.value} != ${other.value}"
        )
    }

    /**
     * Checks for inequality of a [QueryTree] with a value of the same type (e.g. useful to check
     * for constants).
     */
    infix fun ne(other: T): ComparableQueryTree<Boolean> {
        val result = this.value != other
        return ComparableQueryTree(
            result,
            mutableListOf(this, QueryTree(other)),
            "${this.value} != $value"
        )
    }

    /** Checks if the value is contained in the collection of the other [QueryTree]. */
    infix fun IN(other: QueryTree<Collection<*>>): ComparableQueryTree<Boolean> {
        val result = other.value.contains(this.value)
        return ComparableQueryTree(
            result,
            mutableListOf(this, other),
            "${this.value} in ${other.value}"
        )
    }

    /** Checks if the value is contained in the collection [other]. */
    infix fun IN(other: Collection<*>): ComparableQueryTree<Boolean> {
        val result = other.contains(this.value)
        return ComparableQueryTree(
            result,
            mutableListOf(this, QueryTree(other)),
            "${this.value} in $other"
        )
    }

    /** Checks if the value is a member of the type of the other [QueryTree]. */
    infix fun IS(other: QueryTree<Class<*>>): ComparableQueryTree<Boolean> {
        val result = other.value.isInstance(this.value)
        return ComparableQueryTree(
            result,
            mutableListOf(this, other),
            "${this.value} is ${other.value}"
        )
    }

    /** Checks if the value is a member of the type of [oter]. */
    infix fun IS(other: Class<*>): ComparableQueryTree<Boolean> {
        val result = other.isInstance(this.value)
        return ComparableQueryTree(
            result,
            mutableListOf(this, QueryTree(other)),
            "${this.value} is $other"
        )
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

open class ComparableQueryTree<T : Comparable<T>>(
    override var value: T,
    override val children: MutableList<QueryTree<*>> = mutableListOf(),
    override var stringRepresentation: String = ""
) : QueryTree<T>(value, children, stringRepresentation), Comparable<ComparableQueryTree<T>> {
    override fun compareTo(other: ComparableQueryTree<T>): Int {
        return this.value.compareTo(other.value)
    }
}

/** Performs a logical and (&&) operation between the values of two [QueryTree]s. */
infix fun QueryTree<Boolean>.and(other: QueryTree<Boolean>): ComparableQueryTree<Boolean> {
    return ComparableQueryTree(
        this.value && other.value,
        mutableListOf(this, other),
        stringRepresentation = "${this.value} && ${other.value}"
    )
}

/** Performs a logical or (||) operation between the values of two [QueryTree]s. */
infix fun QueryTree<Boolean>.or(other: QueryTree<Boolean>): ComparableQueryTree<Boolean> {
    return ComparableQueryTree(
        this.value || other.value,
        mutableListOf(this, other),
        stringRepresentation = "${this.value} || ${other.value}"
    )
}

/** Performs a logical xor operation between the values of two [QueryTree]s. */
infix fun QueryTree<Boolean>.xor(other: QueryTree<Boolean>): ComparableQueryTree<Boolean> {
    return ComparableQueryTree(
        this.value xor other.value,
        mutableListOf(this, other),
        stringRepresentation = "${this.value} xor ${other.value}"
    )
}

/** Evaluates a logical implication (->) operation between the values of two [QueryTree]s. */
infix fun QueryTree<Boolean>.implies(other: QueryTree<Boolean>): ComparableQueryTree<Boolean> {
    return ComparableQueryTree(
        !this.value || other.value,
        mutableListOf(this, other),
        stringRepresentation = "${this.value} => ${other.value}"
    )
}

/** Compares the numeric values of two [QueryTree]s for this being "greater than" (>) [other]. */
infix fun <T : Number, S : Number> QueryTree<T>.gt(
    other: QueryTree<S>
): ComparableQueryTree<Boolean> {
    val result = this.value.compareTo(other.value) > 0
    return ComparableQueryTree(result, mutableListOf(this, other), "${this.value} > ${other.value}")
}

/**
 * Compares the numeric values of a [QueryTree] and another number for this being "greater than" (>)
 * [other].
 */
infix fun <T : Number, S : Number> QueryTree<T>.gt(other: S): ComparableQueryTree<Boolean> {
    val result = this.value.compareTo(other) > 0
    return ComparableQueryTree(
        result,
        mutableListOf(this, QueryTree(other)),
        "${this.value} > $other"
    )
}

/**
 * Compares the numeric values of two [QueryTree]s for this being "greater than or equal" (>=)
 * [other].
 */
infix fun <T : Number, S : Number> QueryTree<T>.ge(
    other: QueryTree<S>
): ComparableQueryTree<Boolean> {
    val result = this.value.compareTo(other.value) >= 0
    return ComparableQueryTree(
        result,
        mutableListOf(this, other),
        "${this.value} >= ${other.value}"
    )
}

/**
 * Compares the numeric values of a [QueryTree] and another number for this being "greater than or
 * equal" (>=) [other].
 */
infix fun <T : Number, S : Number> QueryTree<T>.ge(other: S): ComparableQueryTree<Boolean> {
    val result = this.value.compareTo(other) >= 0
    return ComparableQueryTree(
        result,
        mutableListOf(this, QueryTree(other)),
        "${this.value} >= $other"
    )
}

/** Compares the numeric values of two [QueryTree]s for this being "less than" (<) [other]. */
infix fun <T : Number, S : Number> QueryTree<T>.lt(
    other: QueryTree<S>
): ComparableQueryTree<Boolean> {
    val result = this.value.compareTo(other.value) < 0
    return ComparableQueryTree(result, mutableListOf(this, other), "${this.value} < ${other.value}")
}

/**
 * Compares the numeric values of a [QueryTree] and another number for this being "less than" (<)
 * [other].
 */
infix fun <T : Number, S : Number> QueryTree<T>.lt(other: S): ComparableQueryTree<Boolean> {
    val result = this.value.compareTo(other) < 0
    return ComparableQueryTree(
        result,
        mutableListOf(this, QueryTree(other)),
        "${this.value} < $other"
    )
}

/**
 * Compares the numeric values of two [QueryTree]s for this being "less than or equal" (=) [other].
 */
infix fun <T : Number, S : Number> QueryTree<T>.le(
    other: QueryTree<S>
): ComparableQueryTree<Boolean> {
    val result = this.value.compareTo(other.value) <= 0
    return ComparableQueryTree(
        result,
        mutableListOf(this, other),
        "${this.value} <= ${other.value}"
    )
}

/**
 * Compares the numeric values of a [QueryTree] and another number for this being "less than or
 * equal" (<=) [other].
 */
infix fun <T : Number, S : Number> QueryTree<T>.le(other: S): ComparableQueryTree<Boolean> {
    val result = this.value.compareTo(other) <= 0
    return ComparableQueryTree(
        result,
        mutableListOf(this, QueryTree(other)),
        "${this.value} <= $other"
    )
}

/** Negates the value of [arg] and returns the resulting [QueryTree]. */
fun not(arg: QueryTree<Boolean>): ComparableQueryTree<Boolean> {
    val result = !arg.value
    return ComparableQueryTree(result, mutableListOf(arg), "! ${arg.value}")
}

/**
 * This is a small wrapper to create a [QueryTree] containing a constant value, so that it can be
 * used to in comparison with other [QueryTree] objects.
 */
fun <T : Comparable<T>> const(n: T): ComparableQueryTree<T> {
    return ComparableQueryTree(n, stringRepresentation = "$n")
}

/**
 * This is a small wrapper to create a [QueryTree] containing a constant value, so that it can be
 * used to in comparison with other [QueryTree] objects.
 */
fun <T> const(n: T): QueryTree<T> {
    return QueryTree(n, stringRepresentation = "$n")
}
