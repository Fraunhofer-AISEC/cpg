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
import java.util.*
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
    value: T,
    var children: List<QueryTree<*>> = emptyList(),
    var stringRepresentation: String = "",

    /**
     * The node, to which this current element of the query tree is associated with. This is useful
     * to access detailed information about the node that is otherwise only contained in string form
     * in [stringRepresentation].
     */
    node: Node? = null,
    override var assumptions: MutableSet<Assumption> = mutableSetOf(),

    /**
     * Indicates whether this [QueryTree] is suppressed by the user. The query tree itself will
     * still hold the original value, but it is wrapped in a new [QueryTree] with the suppressed
     * value as the only child.
     *
     * See [checkForSuppression] for more information.
     */
    var suppressed: Boolean = false,
    val operator: QueryOperators,
) : Comparable<QueryTree<T>>, HasAssumptions {
    /**
     * Determines if the [QueryTree.value] is acceptable after evaluating the [assumptions] which
     * affect the result.
     */
    open val confidence: AcceptanceStatus
        get() {
            val assumptionsToUse = this.assumptions
            return when (operator) {
                QueryOperators.SUPPRESS -> {
                    AcceptedResult
                }
                QueryOperators.EVALUATE -> {
                    AcceptanceStatus.fromAssumptionsAndStatus(
                        this.children.map { it.confidence },
                        assumptionsToUse,
                    )
                }

                // These operators require everything to be Accepted and all assumptions are
                // accepted/ignored
                QueryOperators.ALL -> {
                    AcceptanceStatus.fromAssumptionsAndStatus(
                        children.map { it.confidence },
                        assumptionsToUse,
                    )
                }
                QueryOperators.AND,
                QueryOperators.EQ,
                QueryOperators.NE,
                QueryOperators.GT,
                QueryOperators.GE,
                QueryOperators.LT,
                QueryOperators.LE,
                QueryOperators.IS,
                QueryOperators.XOR -> {
                    AcceptanceStatus.fromAssumptionsAndStatus(
                        children.minOf { it.confidence },
                        assumptionsToUse,
                    )
                }

                // These operators require only one "true" result to be Accepted. We also want all
                // assumptions related to this requirement to be accepted/ignored.
                QueryOperators.OR,
                QueryOperators.ANY -> {
                    val trueChildren = children.filter { it.value == true }
                    val trueConfidence = trueChildren.map { it.confidence }
                    val falseChildren = children.filter { it.value == false }
                    val falseConfidence = falseChildren.map { it.confidence }
                    val resultingConfidence =
                        if (trueConfidence.isNotEmpty() && trueConfidence.max() is AcceptedResult) {
                            AcceptedResult
                        } else if (
                            trueChildren.isEmpty() && falseConfidence.all { it is AcceptedResult }
                        ) {
                            AcceptedResult
                        } else if (
                            trueConfidence.isEmpty() &&
                                falseChildren.isNotEmpty() &&
                                falseConfidence.max() is RejectedResult
                        ) {
                            RejectedResult
                        } else {
                            UndecidedResult
                        }

                    AcceptanceStatus.fromAssumptionsAndStatus(resultingConfidence, assumptionsToUse)
                }
                QueryOperators.IMPLIES -> {
                    // TODO: This is a tricky one
                    if (children.size != 2) {
                        throw QueryException(
                            "The implies operator requires exactly two children, but got ${children.size}."
                        )
                    }
                    val lhs = children.first()
                    val rhs = children.last()
                    return when {
                        lhs.value == false && lhs.confidence is AcceptedResult -> {
                            // If the lhs is false and accepted, we can say that the implication is
                            // accepted.
                            AcceptedResult
                        }
                        rhs.value == true && rhs.confidence is AcceptedResult -> {
                            // If the rhs is true and accepted, we can say that the implication is
                            // accepted.
                            AcceptedResult
                        }
                        else -> {
                            // We do not know if the implication is accepted or not
                            UndecidedResult
                        }
                    }
                }
                QueryOperators.IN -> {
                    if (value == true) {
                        // TODO: The lhs must be accepted and the element matching lhs in the rhs
                        // must be accepted too. We do not care about the rest.
                        // Qick-fix until we have this: Everything must be accepted.
                        AcceptanceStatus.fromAssumptionsAndStatus(
                            setOf(children[0].confidence, children[1].confidence),
                            assumptionsToUse,
                        )
                    } else {
                        // If the value is false and not everything is of confidence accepted, we
                        // say it's undecided.
                        if (minOf(children[0].confidence, children[1].confidence) != AcceptedResult)
                            UndecidedResult
                        else
                            AcceptanceStatus.fromAssumptionsAndStatus(
                                minOf(children[0].confidence, children[1].confidence),
                                assumptionsToUse,
                            )
                    }
                }
                QueryOperators.NOT -> {
                    AcceptanceStatus.fromAssumptionsAndStatus(
                        children[0].confidence,
                        assumptionsToUse,
                    )
                }
            }
        }

    /** The value of the [QueryTree] is the result of the query evaluation. */
    var value = value
        set(fieldValue) {
            field = fieldValue

            // Update the ID whenever the value changes
            id = computeId()
        }

    /**
     * The associated [Node]. This can be, for example, the node where the query was executed from.
     */
    var node = node
        set(fieldValue) {
            field = fieldValue

            // Update the ID whenever the node changes
            id = computeId()
        }

    /**
     * Returns a unique identifier for this [QueryTree]. The identifier is based on the node's ID,
     * the ID of its children, and the hash of the value.
     *
     * This allows uniquely identifying the [QueryTree] even if it is not associated with a specific
     * node.
     */
    var id: Uuid

    init {
        id = computeId()
        checkForSuppression()
    }

    /**
     * This functions checks if the [QueryTree] is suppressed by the user. If it is, it sets the
     * value to `true` and creates a new [QueryTree] with the suppressed value as the only child.
     *
     * This is useful for cases where the analysis has an obvious error and the user wants to
     * manually correct this.
     */
    fun checkForSuppression() {
        if (suppressed) {
            return
        }

        // Check for a suppression value for this node
        val key = suppressions.keys.firstOrNull { it(this) }
        if (key != null) {
            val suppressionValue = suppressions[key]

            // Create a copy of the original node with the suppressed value
            val copyQ =
                QueryTree(
                    value = value,
                    children = children.toList(),
                    stringRepresentation = stringRepresentation,
                    node = node,
                    assumptions = assumptions.toMutableSet(),
                    suppressed = true,
                    operator = QueryOperators.SUPPRESS,
                )
            @Suppress("UNCHECKED_CAST")

            // Set the value to true, update the string representation and set the original
            // QueryTree as the only child
            value = suppressionValue as T
            stringRepresentation = "The query tree was set to $value by suppression"
            children = listOf(copyQ)
        }
    }

    fun computeId(): Uuid {
        val nodePart =
            node?.id?.toLongs { mostSignificantBits, leastSignificantBits -> leastSignificantBits }

        val childrenIds =
            children.sumOf { child ->
                child.id.toLongs { mostSignificantBits, leastSignificantBits ->
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

    companion object {
        val suppressions = mutableMapOf<(QueryTree<*>) -> Boolean, Any>()
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

    return QueryTree(
        value = this,
        stringRepresentation = this.toString(),
        node = this as? Node,
        operator = QueryOperators.EVALUATE,
    )
}

enum class QueryOperators {
    ALL,
    ANY,
    AND,
    OR,
    XOR,
    IMPLIES,
    EQ,
    NE,
    GT,
    GE,
    LT,
    LE,
    IS,
    IN,
    NOT,
    EVALUATE,
    SUPPRESS,
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
    return QueryTree(
        value = result,
        children = mutableListOf(thisQt, otherQt),
        stringRepresentation = "${thisQt.value} is ${otherQt.value}",
        operator = QueryOperators.IS,
    )
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

    return QueryTree(
        value = result,
        children = mutableListOf(thisQt, otherQt),
        stringRepresentation = "${thisQt.value} in ${otherQt.value}",
        operator = QueryOperators.IN,
    )
}

/**
 * Checks for equality of two objects and creates a [QueryTree] with a value `true` if they are
 * equal.
 */
infix fun <T, S> T.eq(other: S): QueryTree<Boolean> {
    val thisQt = this.toQueryTree()
    val otherQt = other.toQueryTree()

    val result = thisQt.value == otherQt.value
    return QueryTree(
        value = result,
        children = mutableListOf(thisQt, otherQt),
        stringRepresentation = "${thisQt.value} == ${otherQt.value}",
        operator = QueryOperators.EQ,
    )
}

/**
 * Checks for unequality of two objects and creates a [QueryTree] with a value `true` if they are
 * unequal.
 */
infix fun <T, S> T.ne(other: S): QueryTree<Boolean> {
    val thisQt = this.toQueryTree()
    val otherQt = other.toQueryTree()

    val result = thisQt.value != otherQt.value
    return QueryTree(
        value = result,
        children = mutableListOf(thisQt, otherQt),
        stringRepresentation = "${thisQt.value} != ${otherQt.value}",
        operator = QueryOperators.NE,
    )
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
        value = this.value && other.value,
        children = mutableListOf(this, other),
        stringRepresentation = "${this.value} && ${other.value}",
        operator = QueryOperators.AND,
    )
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
        value = this.value || other.value,
        children = mutableListOf(this, other),
        stringRepresentation = "${this.value} || ${other.value}",
        operator = QueryOperators.OR,
    )
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
        operator = QueryOperators.XOR,
    )
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
        operator = QueryOperators.IMPLIES,
    )
}

/** Evaluates a logical implication (->) operation between the values of two [QueryTree]s. */
infix fun QueryTree<Boolean>.implies(other: Lazy<QueryTree<Boolean>>): QueryTree<Boolean> {
    return QueryTree(
        !this.value || other.value.value,
        if (!this.value) mutableListOf(this) else mutableListOf(this, other.value),
        stringRepresentation =
            if (!this.value) "false => XYZ" else "${this.value} => ${other.value}",
        operator = QueryOperators.IMPLIES,
    )
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
        operator = QueryOperators.GT,
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
        operator = QueryOperators.GE,
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
        operator = QueryOperators.LT,
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
        operator = QueryOperators.LE,
    )
}

/** Negates the value of [arg] and returns the resulting [QueryTree]. */
fun not(arg: QueryTree<Boolean>): QueryTree<Boolean> {
    val result = !arg.value
    return QueryTree(
        value = result,
        children = mutableListOf(arg),
        stringRepresentation = "! ${arg.value}",
        operator = QueryOperators.NOT,
    )
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

/**
 * Represents a single path result of a query evaluation. It contains the [value] of the path, the
 * [children] that were evaluated to reach this path, the [stringRepresentation] of the path, the
 * [node] that was evaluated, and the [terminationReason] that explains why this path was
 * terminated.
 */
class SinglePathResult(
    value: Boolean,
    children: List<QueryTree<*>> = emptyList(),
    stringRepresentation: String = "",
    node: Node? = null,
    val terminationReason: TerminationReason,
    operator: QueryOperators,
) :
    QueryTree<Boolean>(
        value = value,
        children = children,
        stringRepresentation = stringRepresentation,
        node = node,
        operator = operator,
    )

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
        operator = QueryOperators.ALL,
    )
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
        operator = QueryOperators.ANY,
    )
}
