package de.fraunhofer.aisec.cpg.graph.statements.expressions

import de.fraunhofer.aisec.cpg.graph.SubGraph

/**
 * Represents a key / value pair, often found in languages that allow associative arrays or objects, such as Python, Golang or JavaScript.
 *
 * Most often used in combination with an [de.fraunhofer.aisec.cpg.graph.statements.expressions.InitializerListExpression] to represent the creation of an array.
 */
class KeyValueExpression : Expression() {

    /**
     * The name of this pair. Intentionally a literal, to support strings and numbers, but not complex expressions.
     */
    @SubGraph("AST")
    val name: Literal<*>? = null

    /**
    * The value of this pair. It can be any expression
     */
    val value: Expression? = null

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