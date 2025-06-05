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
import de.fraunhofer.aisec.cpg.assumptions.AssumptionStatus
import de.fraunhofer.aisec.cpg.assumptions.HasAssumptions
import de.fraunhofer.aisec.cpg.evaluation.compareTo
import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.uuid.Uuid

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
    override var assumptions: MutableSet<Assumption> = mutableSetOf(),
) : Comparable<QueryTree<T>>, HasAssumptions {

    /**
     * The purpose of [lazyDecision] is to evaluate the decision after all post-processing
     * information is applied, e.g., after setting the [AssumptionStatus] of the [Assumption]s. This
     * default implementation will simply consider the value of the [QueryTree], if the value is a
     * [Boolean], it will return [Failed] and [Succeeded] respectively. If the value is not a
     * [Boolean], it will return [Succeeded] for now as the value was simply determined.
     *
     * This function should be overridden for [QueryTree]s and certain operations to steer how
     * assumptions affect the decision-making process. As an example, for logic operations "and",
     * "or", and "implies", the decision should first be made for the left and right hand side of
     * the operation because invalidating a subtree has different effects for each operation.
     */
    var lazyDecision = lazy {
        val boolValue: Boolean? = this.value as? Boolean
        val assumptions = collectAssumptions()

        val decisionState =
            when (boolValue) {
                false -> Failed
                else -> Succeeded
            }

        val (newValue, stringInfo) = decisionState.decideWithAssumptions(assumptions)

        QueryTree(
            value = newValue,
            children = mutableListOf(this),
            stringRepresentation =
                "The requirement ${ newValue::class.simpleName } because $stringInfo",
        )
    }

    /**
     * This function changes the decision state based on the [AssumptionStatus] of the provided
     * assumptions. It is the basic logic for assumption based decisions when handling a leaf
     * QueryTree<Boolean>, i.e. the first QueryTree<Boolean> to be converted into a [Decision] as
     * well as the logic used for propagating [Decision]s in the QueryTree hierarchy, while
     * considering assumptions on the intermediate levels.
     */
    fun DecisionState.decideWithAssumptions(
        assumptions: Set<Assumption>
    ): Pair<DecisionState, String> {
        return when {
            this == NotYetEvaluated ->
                NotYetEvaluated to "QueryTree to decide is not a boolean value"
            this == Failed || assumptions.any { it.status == AssumptionStatus.Rejected } ->
                Failed to
                    (if (this == Failed) "the query failed given the accepted assumptions"
                    else
                        "the assumptions ${assumptions.filter { it.status == AssumptionStatus.Rejected }.map { it.id.toHexDashString() }.joinToString(", ") } were rejected")

            this == Undecided || assumptions.any { it.status == AssumptionStatus.Undecided } ->
                Undecided to
                    "the assumptions ${assumptions.filter { it.status == AssumptionStatus.Undecided }.map { it.id.toHexDashString() }.joinToString(", ")} are not yet decided"

            this == Succeeded &&
                assumptions.all {
                    it.status == AssumptionStatus.Ignored || it.status == AssumptionStatus.Accepted
                } ->
                Succeeded to
                    "the query was evaluated to true and all assumptions were accepted or deemed not influencing the result."

            else -> NotYetEvaluated to "Something went wrong"
        }
    }

    /**
     * Returns a unique identifier for this [QueryTree]. The identifier is based on the node's ID,
     * the ID of its children, and the hash of the value.
     *
     * This allows to uniquely identify the [QueryTree] even if it is not associated with a specific
     * node.
     */
    val id: Uuid
        get() {
            val nodePart =
                node?.id?.toLongs { mostSignificantBits, leastSignificantBits ->
                    leastSignificantBits
                }

            val childrenIds =
                children.sumOf {
                    it.id.toLongs { mostSignificantBits, leastSignificantBits ->
                        leastSignificantBits + mostSignificantBits
                    }
                }

            return Uuid.fromLongs(nodePart ?: 0, childrenIds + Objects.hash(value))
        }

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

    /**
     * Adds the [assumptions] attached to the [QueryTree] itself and of all sub [QueryTree]s that
     * were declared as children.
     */
    override fun collectAssumptions(): Set<Assumption> {
        return super.collectAssumptions() + children.flatMap { it.collectAssumptions() }.toSet()
    }
}

operator fun QueryTree<*>?.compareTo(other: Number): Int {
    if (this?.value is Number) {
        return (this.value as Number).compareTo(other)
    }
    throw QueryException("Cannot compare objects of type ${this?.value} and $other")
}

fun <T> T.toQueryTree(): QueryTree<T> {
    if (this is QueryTree<*>) {
        @Suppress("UNCHECKED_CAST")
        return this as QueryTree<T>
    }

    return QueryTree(this, stringRepresentation = this.toString())
}

/**
 * Checks if the value is a member of the type of [other] (or the value of the respective
 * [QueryTree]). creates [QueryTree]s for [this], [other] and the result if necessary.
 */
infix fun <T, S> T.IS(other: S): QueryTree<Boolean> {
    val thisQt = this.toQueryTree()
    val otherQt = other.toQueryTree()

    val result =
        (otherQt.value as? Class<*>)?.isInstance(thisQt.value)
            ?: throw IllegalArgumentException(
                "Cannot check if ${thisQt.value} is of type ${otherQt.value}. The other value must be a Class<*>."
            )
    return QueryTree(result, mutableListOf(thisQt, otherQt), "${thisQt.value} is ${otherQt.value}")
}

/**
 * Checks if the value is contained in the collection [other] (or the value of the respective
 * [QueryTree]). creates [QueryTree]s for [this], [other] and the result if necessary.
 */
infix fun <T, S> T.IN(other: S): QueryTree<Boolean> {
    val thisQt = this.toQueryTree()
    val otherQt = other.toQueryTree()

    val result =
        (otherQt.value as? Collection<*>)?.contains(thisQt.value)
            ?: throw IllegalArgumentException(
                "Cannot check if ${thisQt.value} is of type ${otherQt.value}. The other value must be a Collection<*>."
            )

    return QueryTree(result, mutableListOf(thisQt, otherQt), "${thisQt.value} in ${otherQt.value}")
}

/**
 * Checks for equality of two objects and creates a [QueryTree] with a value `true` if they are
 * equal.
 */
infix fun <T, S> T.eq(other: S): QueryTree<Boolean> {
    val thisQt = this.toQueryTree()
    val otherQt = other.toQueryTree()

    val result = thisQt.value == otherQt.value
    return QueryTree(result, mutableListOf(thisQt, otherQt), "${thisQt.value} == ${otherQt.value}")
}

/**
 * Checks for unequality of two objects and creates a [QueryTree] with a value `true` if they are
 * unequal.
 */
infix fun <T, S> T.ne(other: S): QueryTree<Boolean> {
    val thisQt = this.toQueryTree()
    val otherQt = other.toQueryTree()

    val result = thisQt.value != otherQt.value
    return QueryTree(result, mutableListOf(thisQt, otherQt), "${thisQt.value} != ${otherQt.value}")
}

/**
 * Performs a logical and (&&) operation between the values and creates and returns [QueryTree]s.
 */
infix fun Boolean.and(other: Boolean): QueryTree<Boolean> {
    return this.toQueryTree() and other.toQueryTree()
}

/**
 * Performs a logical and (&&) operation between the values and creates and returns [QueryTree]s.
 */
infix fun Boolean.and(other: QueryTree<Boolean>): QueryTree<Boolean> {
    return this.toQueryTree() and other
}

/**
 * Performs a logical and (&&) operation between the values and creates and returns [QueryTree]s.
 */
infix fun QueryTree<Boolean>.and(other: Boolean): QueryTree<Boolean> {
    return this and other.toQueryTree()
}

/** Performs a logical and (&&) operation between the values of two [QueryTree]s. */
infix fun QueryTree<Boolean>.and(other: QueryTree<Boolean>): QueryTree<Boolean> {
    return QueryTree(
            this.value && other.value,
            mutableListOf(this, other),
            stringRepresentation = "${this.value} && ${other.value}",
        )
        .registerLazyDecision { this.lazyDecision.value and other.lazyDecision.value }
}

/**
 * This function is used to add a lambda as lazy decision to a QueryTree<Boolean> for functions on
 * those trees that need to change the default behavior of what assumptions need to be considered
 * when deciding on nested [QueryTree]s. See [QueryTree.lazyDecision] for more information.
 */
fun QueryTree<Boolean>.registerLazyDecision(
    decision: () -> QueryTree<DecisionState>
): QueryTree<Boolean> {
    val assumptions = this.assumptions
    this.lazyDecision = lazy {
        decision().also {
            val decisionVal = it.value.decideWithAssumptions(assumptions).first
            if (decisionVal != it.value) {
                it.stringRepresentation =
                    "$it.stringRepresentation changed to $decisionVal due to assumptions"
            }
            it.value = decisionVal
        }
    }
    return this
}

/** Performs a logical or (||) operation between the values and creates and returns [QueryTree]s. */
infix fun Boolean.or(other: Boolean): QueryTree<Boolean> {
    return this.toQueryTree() or other.toQueryTree()
}

/** Performs a logical or (||) operation between the values and creates and returns [QueryTree]s. */
infix fun Boolean.or(other: QueryTree<Boolean>): QueryTree<Boolean> {
    return this.toQueryTree() or other
}

/** Performs a logical or (||) operation between the values and creates and returns [QueryTree]s. */
infix fun QueryTree<Boolean>.or(other: Boolean): QueryTree<Boolean> {
    return this or other.toQueryTree()
}

/** Performs a logical or (||) operation between the values of two [QueryTree]s. */
infix fun QueryTree<Boolean>.or(other: QueryTree<Boolean>): QueryTree<Boolean> {
    return QueryTree(
            this.value || other.value,
            mutableListOf(this, other),
            stringRepresentation = "${this.value} || ${other.value}",
        )
        .registerLazyDecision { this.lazyDecision.value or other.lazyDecision.value }
}

/** Performs a logical xor operation between the values and creates and returns [QueryTree]s. */
infix fun Boolean.xor(other: Boolean): QueryTree<Boolean> {
    return this.toQueryTree() xor other.toQueryTree()
}

/** Performs a logical xor operation between the values and creates and returns [QueryTree]s. */
infix fun Boolean.xor(other: QueryTree<Boolean>): QueryTree<Boolean> {
    return this.toQueryTree() xor other
}

/** Performs a logical xor operation between the values and creates and returns [QueryTree]s. */
infix fun QueryTree<Boolean>.xor(other: Boolean): QueryTree<Boolean> {
    return this xor other.toQueryTree()
}

/** Performs a logical xor operation between the values of two [QueryTree]s. */
infix fun QueryTree<Boolean>.xor(other: QueryTree<Boolean>): QueryTree<Boolean> {
    return QueryTree(
            this.value xor other.value,
            mutableListOf(this, other),
            stringRepresentation = "${this.value} xor ${other.value}",
        )
        .registerLazyDecision { this.lazyDecision.value xor other.lazyDecision.value }
}

/**
 * Performs a logical implication (->) operation between the values and creates and returns
 * [QueryTree]s.
 */
infix fun Boolean.implies(other: Boolean): QueryTree<Boolean> {
    return this.toQueryTree() implies other.toQueryTree()
}

/**
 * Performs a logical implication (->) operation between the values and creates and returns
 * [QueryTree]s.
 */
infix fun Boolean.implies(other: QueryTree<Boolean>): QueryTree<Boolean> {
    return this.toQueryTree() implies other
}

/**
 * Performs a logical implication (->) operation between the values and creates and returns
 * [QueryTree]s.
 */
infix fun QueryTree<Boolean>.implies(other: Boolean): QueryTree<Boolean> {
    return this implies other.toQueryTree()
}

/** Evaluates a logical implication (->) operation between the values of two [QueryTree]s. */
infix fun QueryTree<Boolean>.implies(other: QueryTree<Boolean>): QueryTree<Boolean> {
    return QueryTree(
            !this.value || other.value,
            mutableListOf(this, other),
            stringRepresentation = "${this.value} => ${other.value}",
        )
        .registerLazyDecision { this.lazyDecision.value.implies(other.lazyDecision.value) }
}

/** Evaluates a logical implication (->) operation between the values of two [QueryTree]s. */
infix fun QueryTree<Boolean>.implies(other: Lazy<QueryTree<Boolean>>): QueryTree<Boolean> {
    return QueryTree(
            !this.value || other.value.value,
            if (!this.value) mutableListOf(this) else mutableListOf(this, other.value),
            stringRepresentation =
                if (!this.value) "false => XYZ" else "${this.value} => ${other.value}",
        )
        .registerLazyDecision { this.lazyDecision.value.implies(other.value.lazyDecision.value) }
}

/**
 * Creates and compares the numeric values of two [QueryTree]s for this being "greater than" (>)
 * [other].
 */
infix fun <T : Number?, S : Number?> S.gt(other: T): QueryTree<Boolean> {
    return this.toQueryTree() gt other.toQueryTree()
}

/**
 * Creates and compares the numeric values of two [QueryTree]s for this being "greater than" (>)
 * [other].
 */
infix fun <T : Number?, S : Number?> QueryTree<S>?.gt(other: T): QueryTree<Boolean> {
    return this gt other.toQueryTree()
}

/**
 * Creates and compares the numeric values of two [QueryTree]s for this being "greater than" (>)
 * [other].
 */
infix fun <T : Number?, S : Number?> S.gt(other: QueryTree<T>?): QueryTree<Boolean> {
    return this.toQueryTree() gt other
}

/** Compares the numeric values of two [QueryTree]s for this being "greater than" (>) [other]. */
infix fun <T : Number?, S : Number?> QueryTree<T>?.gt(other: QueryTree<S>?): QueryTree<Boolean> {
    val result =
        this?.value?.let { thisV -> other?.value?.let { otherV -> thisV.compareTo(otherV) > 0 } }
            ?: false
    return QueryTree(
        result,
        listOfNotNull(this, other).toMutableList(),
        "${this?.value} > ${other?.value}",
    )
}

/**
 * Creates and compares the numeric values of two [QueryTree]s for this being "greater than or
 * equal" (>=) [other].
 */
infix fun <T : Number?, S : Number?> S.ge(other: T): QueryTree<Boolean> {
    return this.toQueryTree() ge other.toQueryTree()
}

/**
 * Creates and compares the numeric values of two [QueryTree]s for this being "greater than or
 * equal" (>=) [other].
 */
infix fun <T : Number?, S : Number?> QueryTree<S>?.ge(other: T): QueryTree<Boolean> {
    return this ge other.toQueryTree()
}

/**
 * Creates and compares the numeric values of two [QueryTree]s for this being "greater than or
 * equal" (>=) [other].
 */
infix fun <T : Number?, S : Number?> S.ge(other: QueryTree<T>?): QueryTree<Boolean> {
    return this.toQueryTree() ge other
}

/**
 * Compares the numeric values of two [QueryTree]s for this being "greater than or equal" (>=)
 * [other].
 */
infix fun <T : Number?, S : Number?> QueryTree<T>?.ge(other: QueryTree<S>?): QueryTree<Boolean> {
    val result =
        this?.value?.let { thisV -> other?.value?.let { otherV -> thisV.compareTo(otherV) >= 0 } }
            ?: false
    return QueryTree(
        result,
        listOfNotNull(this, other).toMutableList(),
        "${this?.value} >= ${other?.value}",
    )
}

/**
 * Creates and compares the numeric values of two [QueryTree]s for this being "less than" (<)
 * [other].
 */
infix fun <T : Number?, S : Number?> S.lt(other: T): QueryTree<Boolean> {
    return this.toQueryTree() lt other.toQueryTree()
}

/**
 * Creates and compares the numeric values of two [QueryTree]s for this being "less than" (<)
 * [other].
 */
infix fun <T : Number?, S : Number?> QueryTree<S>?.lt(other: T): QueryTree<Boolean> {
    return this lt other.toQueryTree()
}

/**
 * Creates and compares the numeric values of two [QueryTree]s for this being "less than" (<)
 * [other].
 */
infix fun <T : Number?, S : Number?> S.lt(other: QueryTree<T>?): QueryTree<Boolean> {
    return this.toQueryTree() lt other
}

/** Compares the numeric values of two [QueryTree]s for this being "less than" (<) [other]. */
infix fun <T : Number?, S : Number?> QueryTree<T>?.lt(other: QueryTree<S>?): QueryTree<Boolean> {
    val result =
        this?.value?.let { thisV -> other?.value?.let { otherV -> thisV.compareTo(otherV) < 0 } }
            ?: false
    return QueryTree(
        result,
        listOfNotNull(this, other).toMutableList(),
        "${this?.value} < ${other?.value}",
    )
}

/**
 * Creates and compares the numeric values of two [QueryTree]s for this being "less than or equal"
 * (<=) [other].
 */
infix fun <T : Number?, S : Number?> S.le(other: T): QueryTree<Boolean> {
    return this.toQueryTree() le other.toQueryTree()
}

/**
 * Creates and compares the numeric values of two [QueryTree]s for this being "less than or equal"
 * (<=) [other].
 */
infix fun <T : Number?, S : Number?> QueryTree<S>?.le(other: T): QueryTree<Boolean> {
    return this le other.toQueryTree()
}

/**
 * Creates and compares the numeric values of two [QueryTree]s for this being "less than or equal"
 * (<=) [other].
 */
infix fun <T : Number?, S : Number?> S.le(other: QueryTree<T>?): QueryTree<Boolean> {
    return this.toQueryTree() le other
}

/**
 * Compares the numeric values of two [QueryTree]s for this being "less than or equal" (=) [other].
 */
infix fun <T : Number?, S : Number?> QueryTree<T>?.le(other: QueryTree<S>?): QueryTree<Boolean> {
    val result =
        this?.value?.let { thisV -> other?.value?.let { otherV -> thisV.compareTo(otherV) <= 0 } }
            ?: false
    return QueryTree(
        result,
        listOfNotNull(this, other).toMutableList(),
        "${this?.value} <= ${other?.value}",
    )
}

/** Negates the value of [arg] and returns the resulting [QueryTree]. */
fun not(arg: QueryTree<Boolean>): QueryTree<Boolean> {
    val result = !arg.value
    return QueryTree(result, mutableListOf(arg), "! ${arg.value}").registerLazyDecision {
        not(arg.lazyDecision.value)
    }
}

/** Negates the value of [arg] and returns the resulting [QueryTree]. */
fun not(arg: Boolean): QueryTree<Boolean> {
    return not(arg.toQueryTree())
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

/**
 * Merges a `List<QueryTree<Boolean>>` into a single `QueryTree<Boolean>`. The [QueryTree.value] is
 * `true` if all elements have value `true`.
 */
fun List<QueryTree<Boolean>>.mergeWithAll(
    node: Node? = null,
    assumptions: MutableSet<Assumption> = mutableSetOf(),
): QueryTree<Boolean> {
    val value = this.all { it.value }
    return QueryTree(
            value = value,
            children = this.toMutableList(),
            stringRepresentation =
                if (value) {
                    "All elements has value true"
                } else {
                    "At least one of the elements has false"
                },
            node = node,
            assumptions = assumptions,
        )
        .registerLazyDecision {
            // Performs an `AND` operation on the lazy decisions of all children
            this.fold(true.toQueryTree().lazyDecision.value) { currentResult, subquery ->
                currentResult and subquery.lazyDecision.value
            }
        }
}

/**
 * Merges a `List<QueryTree<Boolean>>` into a single `QueryTree<Boolean>`. The [QueryTree.value] is
 * `true` if at least one element has value `true`.
 */
fun List<QueryTree<Boolean>>.mergeWithAny(
    node: Node? = null,
    assumptions: MutableSet<Assumption> = mutableSetOf(),
): QueryTree<Boolean> {
    val value = this.any { it.value }
    return QueryTree(
            value = value,
            children = this.toMutableList(),
            stringRepresentation =
                if (value) {
                    "At least one of the elements has value true"
                } else {
                    "All elements have value false"
                },
            node = node,
            assumptions = assumptions,
        )
        .registerLazyDecision {
            // Performs an `OR` operation on the lazy decisions of all children
            this.fold(false.toQueryTree().lazyDecision.value) { currentResult, subquery ->
                currentResult or subquery.lazyDecision.value
            }
        }
}
