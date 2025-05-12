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

import de.fraunhofer.aisec.cpg.assumptions.Assumption
import de.fraunhofer.aisec.cpg.assumptions.HasAssumptions
import de.fraunhofer.aisec.cpg.evaluation.compareTo
import de.fraunhofer.aisec.cpg.graph.Node

/**
 * Holds the [value] to which the statements have been evaluated. The [children] define previous
 * steps of the evaluation, thus building a tree of all steps of the evaluation recursively until we
 * reach the nodes of the CPG. This is necessary if we want to store all steps which are performed
 * when evaluating a query. It helps to make the reasoning of the query more understandable to the
 * user and gives an analyst the maximum of information available.
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
    open var stringRepresentation: String = "",

    /**
     * The node, to which this current element of the query tree is associated with. This is useful
     * to access detailed information about the node that is otherwise only contained in string form
     * in [stringRepresentation].
     */
    open var node: Node? = null,

    /**
     * Assumptions can be created in the QueryTree object with the [assume] function ore by adding
     * an assumption manually.
     */
    override var assumptions: MutableSet<Assumption> = mutableSetOf(),
) : Comparable<QueryTree<T>>, HasAssumptions {
    fun printNicely(depth: Int = 0): String {
        var res =
            "  ".repeat(depth) +
                "$stringRepresentation (==> ${if (value is List<*>) (value as List<*>).joinToString("\n","[", "]") else value.toString()})\n" +
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

    override fun toString(): String {
        return stringRepresentation
    }

    /** Checks for equality of two [QueryTree]s. */
    infix fun eq(other: QueryTree<T>): QueryTree<Boolean> {
        val result = this.value == other.value
        return QueryTree(result, mutableListOf(this, other), "${this.value} == ${other.value}")
    }

    /**
     * Checks for equality of a [QueryTree] with a value of the same type (e.g. useful to check for
     * constants).
     */
    infix fun eq(other: T): QueryTree<Boolean> {
        val result = this.value == other
        return QueryTree(
            result,
            mutableListOf(this, QueryTree(other)),
            "${this.value} == $value",
            this.node,
        )
    }

    /** Checks for inequality of two [QueryTree]s. */
    infix fun ne(other: QueryTree<T>): QueryTree<Boolean> {
        val result = this.value != other.value
        return QueryTree(result, mutableListOf(this, other), "${this.value} != ${other.value}")
    }

    /**
     * Checks for inequality of a [QueryTree] with a value of the same type (e.g. useful to check
     * for constants).
     */
    infix fun ne(other: T): QueryTree<Boolean> {
        val result = this.value != other
        return QueryTree(result, mutableListOf(this, QueryTree(other)), "${this.value} != $value")
    }

    /** Checks if the value is contained in the collection of the other [QueryTree]. */
    infix fun IN(other: QueryTree<Collection<*>>): QueryTree<Boolean> {
        val result = other.value.contains(this.value)
        return QueryTree(result, mutableListOf(this, other), "${this.value} in ${other.value}")
    }

    /** Checks if the value is contained in the collection [other]. */
    infix fun IN(other: Collection<*>): QueryTree<Boolean> {
        val result = other.contains(this.value)
        return QueryTree(result, mutableListOf(this, QueryTree(other)), "${this.value} in $other")
    }

    /** Checks if the value is a member of the type of the other [QueryTree]. */
    infix fun IS(other: QueryTree<Class<*>>): QueryTree<Boolean> {
        val result = other.value.isInstance(this.value)
        return QueryTree(result, mutableListOf(this, other), "${this.value} is ${other.value}")
    }

    /** Checks if the value is a member of the type of [other]. */
    infix fun IS(other: Class<*>): QueryTree<Boolean> {
        val result = other.isInstance(this.value)
        return QueryTree(result, mutableListOf(this, QueryTree(other)), "${this.value} is $other")
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

    override fun compareTo(other: QueryTree<T>): Int {
        if (this.value is Number && other.value is Number) {
            return (this.value as Number).compareTo(other.value as Number)
        } else if (this.value is Comparable<*> && other.value is Comparable<*>) {
            @Suppress("UNCHECKED_CAST")
            return (this.value as Comparable<Any>).compareTo(other.value as Any)
        }
        throw QueryException("Cannot compare objects of type ${this.value} and ${other.value}")
    }

    operator fun compareTo(other: Number): Int {
        if (this.value is Number) {
            return (this.value as Number).compareTo(other)
        }
        throw QueryException("Cannot compare objects of type ${this.value} and $other")
    }

    override fun collectAssumptions(): Set<Assumption> {
        return super.collectAssumptions() + children.flatMap { it.collectAssumptions() }.toSet()
    }
}

/** Performs a logical and (&&) operation between the values of two [QueryTree]s. */
infix fun QueryTree<Boolean>.and(other: QueryTree<Boolean>): QueryTree<Boolean> {
    return QueryTree(
        this.value && other.value,
        mutableListOf(this, other),
        stringRepresentation = "${this.value} && ${other.value}",
    )
}

/** Performs a logical or (||) operation between the values of two [QueryTree]s. */
infix fun QueryTree<Boolean>.or(other: QueryTree<Boolean>): QueryTree<Boolean> {
    return QueryTree(
        this.value || other.value,
        mutableListOf(this, other),
        stringRepresentation = "${this.value} || ${other.value}",
    )
}

/** Performs a logical xor operation between the values of two [QueryTree]s. */
infix fun QueryTree<Boolean>.xor(other: QueryTree<Boolean>): QueryTree<Boolean> {
    return QueryTree(
        this.value xor other.value,
        mutableListOf(this, other),
        stringRepresentation = "${this.value} xor ${other.value}",
    )
}

/** Evaluates a logical implication (->) operation between the values of two [QueryTree]s. */
infix fun QueryTree<Boolean>.implies(other: QueryTree<Boolean>): QueryTree<Boolean> {
    return QueryTree(
        !this.value || other.value,
        mutableListOf(this, other),
        stringRepresentation = "${this.value} => ${other.value}",
    )
}

/** Evaluates a logical implication (->) operation between the values of two [QueryTree]s. */
infix fun QueryTree<Boolean>.implies(other: Lazy<QueryTree<Boolean>>): QueryTree<Boolean> {
    return QueryTree(
        !this.value || other.value.value,
        if (!this.value) mutableListOf(this) else mutableListOf(this, other.value),
        stringRepresentation =
            if (!this.value) "false => XYZ" else "${this.value} => ${other.value}",
    )
}

/** Compares the numeric values of two [QueryTree]s for this being "greater than" (>) [other]. */
infix fun <T : Number, S : Number> QueryTree<T>.gt(other: QueryTree<S>): QueryTree<Boolean> {
    val result = this.value.compareTo(other.value) > 0
    return QueryTree(result, mutableListOf(this, other), "${this.value} > ${other.value}")
}

/**
 * Compares the numeric values of a [QueryTree] and another number for this being "greater than" (>)
 * [other].
 */
infix fun <T : Number, S : Number> QueryTree<T>.gt(other: S): QueryTree<Boolean> {
    val result = this.value.compareTo(other) > 0
    return QueryTree(result, mutableListOf(this, QueryTree(other)), "${this.value} > $other")
}

/**
 * Compares the numeric values of two [QueryTree]s for this being "greater than or equal" (>=)
 * [other].
 */
infix fun <T : Number, S : Number> QueryTree<T>.ge(other: QueryTree<S>): QueryTree<Boolean> {
    val result = this.value.compareTo(other.value) >= 0
    return QueryTree(result, mutableListOf(this, other), "${this.value} >= ${other.value}")
}

/**
 * Compares the numeric values of a [QueryTree] and another number for this being "greater than or
 * equal" (>=) [other].
 */
infix fun <T : Number, S : Number> QueryTree<T>.ge(other: S): QueryTree<Boolean> {
    val result = this.value.compareTo(other) >= 0
    return QueryTree(result, mutableListOf(this, QueryTree(other)), "${this.value} >= $other")
}

/** Compares the numeric values of two [QueryTree]s for this being "less than" (<) [other]. */
infix fun <T : Number, S : Number> QueryTree<T>.lt(other: QueryTree<S>): QueryTree<Boolean> {
    val result = this.value.compareTo(other.value) < 0
    return QueryTree(result, mutableListOf(this, other), "${this.value} < ${other.value}")
}

/**
 * Compares the numeric values of a [QueryTree] and another number for this being "less than" (<)
 * [other].
 */
infix fun <T : Number, S : Number> QueryTree<T>.lt(other: S): QueryTree<Boolean> {
    val result = this.value.compareTo(other) < 0
    return QueryTree(result, mutableListOf(this, QueryTree(other)), "${this.value} < $other")
}

/**
 * Compares the numeric values of two [QueryTree]s for this being "less than or equal" (=) [other].
 */
infix fun <T : Number, S : Number> QueryTree<T>.le(other: QueryTree<S>): QueryTree<Boolean> {
    val result = this.value.compareTo(other.value) <= 0
    return QueryTree(result, mutableListOf(this, other), "${this.value} <= ${other.value}")
}

/**
 * Compares the numeric values of a [QueryTree] and another number for this being "less than or
 * equal" (<=) [other].
 */
infix fun <T : Number, S : Number> QueryTree<T>.le(other: S): QueryTree<Boolean> {
    val result = this.value.compareTo(other) <= 0
    return QueryTree(result, mutableListOf(this, QueryTree(other)), "${this.value} <= $other")
}

/** Negates the value of [arg] and returns the resulting [QueryTree]. */
fun not(arg: QueryTree<Boolean>): QueryTree<Boolean> {
    val result = !arg.value
    return QueryTree(result, mutableListOf(arg), "! ${arg.value}")
}

/** Negates the value of [arg] and returns the resulting [QueryTree]. */
fun not(arg: Boolean): QueryTree<Boolean> {
    val result = !arg
    return QueryTree(result, mutableListOf(QueryTree(arg)), "! $arg")
}

/**
 * This is a small wrapper to create a [QueryTree] containing a constant value, so that it can be
 * used to in comparison with other [QueryTree] objects.
 */
fun <T : Comparable<T>> const(n: T): QueryTree<T> {
    return QueryTree(n, stringRepresentation = "$n")
}

/**
 * This is a small wrapper to create a [QueryTree] containing a constant value, so that it can be
 * used to in comparison with other [QueryTree] objects.
 */
fun <T> const(n: T): QueryTree<T> {
    return QueryTree(n, stringRepresentation = "$n")
}

/**
 * This is a helper function to extract all the final nodes visited on successful [dataFlow]
 * traversals. The helper filters for successful traversals only and maps all those paths to the
 * last node (i.e. the node that made the traversal stop).
 *
 * Use-case to find the terminating nodes (i.e. the `SpecialNodeType` nodes) of the [dataFlow] call
 * below:
 * ```
 * val specialNodes = dataFlow(foo) { it is SpecialNodeType }.successfulLastNodes()
 * ```
 *
 * @return A list of all terminating nodes for successful queries.
 */
// TODO: Use the SinglePathResult instead?
fun QueryTree<*>.successfulLastNodes(): List<Node> {
    val successfulPaths = this.children.filter { it.value == true }
    val innerPath = successfulPaths.flatMap { it.children }
    val finallyTheEntirePaths = innerPath.map { it.value }
    return finallyTheEntirePaths.mapNotNull { (it as? List<*>)?.last() }.filterIsInstance<Node>()
}

sealed interface TerminationReason {
    val endNode: Node
}

data class Success(override val endNode: Node) : TerminationReason

data class PathEnded(override val endNode: Node) : TerminationReason

data class StepsExceeded(override val endNode: Node) : TerminationReason

data class HitEarlyTermination(override val endNode: Node) : TerminationReason

class SinglePathResult(
    override var value: Boolean,
    override val children: MutableList<QueryTree<*>> = mutableListOf(),
    override var stringRepresentation: String = "",
    override var node: Node? = null,
    val terminationReason: TerminationReason,
) : QueryTree<Boolean>(value, children, stringRepresentation, node)

class QueryException(override val message: String) : Exception(message)
