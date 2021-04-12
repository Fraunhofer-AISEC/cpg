package de.fraunhofer.aisec.cpg.graph.statements.expressions

import de.fraunhofer.aisec.cpg.graph.SubGraph

/**
 * Represents a key / value pair, often found in languages that allow associative arrays or objects,
 * such as Python, Golang or JavaScript.
 *
 * Most often used in combination with an
 * [de.fraunhofer.aisec.cpg.graph.statements.expressions.InitializerListExpression] to represent the
 * creation of an array.
 */
class KeyValueExpression : Expression() {

    /**
     * The key of this pair. It is usually a literal, but some languages even allow references to
     * variables as a key.
     */
    @field:SubGraph("AST")
    var key: Expression? = null

    /** The value of this pair. It can be any expression */
    @field:SubGraph("AST")
    var value: Expression? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (other !is KeyValueExpression) {
            return false
        }

        return super.equals(other) && name == other.name && value == other.value
    }
}
