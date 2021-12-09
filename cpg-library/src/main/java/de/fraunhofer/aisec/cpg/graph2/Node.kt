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
package de.fraunhofer.aisec.cpg.graph2

import de.fraunhofer.aisec.cpg.graph.HasLocation
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.util.*

// TODO: Convert to interface and create a AbstractNode
/** Our new node class. */
abstract class Node(
    var parent: Node?,
    var name: String,
    override var location: PhysicalLocation? = null
) : HasLocation {
    abstract val children: List<Node>
}

interface Symbol {}

abstract class Statement(parent: Node?, name: String) : Node(parent, name) {}

abstract class Expression(name: String) : Statement(null, name) {
    abstract fun toPseudoCode(): String
}

open class Reference(name: String) : Expression(name), Assignable {
    var refersTo: Declaration? = null

    override fun toPseudoCode(): String {
        return name
    }

    override val children: List<Node>
        get() = listOf()
}

abstract class Declaration(name: String, val type: String) : Expression(name)

class Variable(name: String, type: String) : Declaration(name, type), Assignable {
    override fun toPseudoCode(): String {
        return "$type $name"
    }

    override val children: List<Node>
        get() = listOf()
}

open class Literal<T>(private val value: T) : Expression(value.toString()) {
    override fun toPseudoCode(): String {
        return value.toString()
    }

    override val children: List<Node>
        get() = listOf()
}

open class BinaryOperation(
    open var lhs: Expression,
    private val operator: String,
    var rhs: Expression,
) : Expression(operator) {

    init {
        lhs.parent = this
        rhs.parent = this
    }

    override fun toPseudoCode(): String {
        return "${lhs.toPseudoCode()} $operator ${rhs.toPseudoCode()}"
    }

    override val children: List<Node>
        get() = listOf(lhs, rhs)
}

interface Assignable {
    var parent: Node?

    fun toPseudoCode(): String
}

/**
 * An assignment assigns the expression on the right-hand-side ([rhs]] to the left-hand-side, which
 * needs to be [Assignable].
 */
class Assignment(lhs: Expression /*Assignable*/, rhs: Expression) : BinaryOperation(lhs, "=", rhs) {

    init {
        lhs.parent = this
        rhs.parent = this
    }

    override fun toPseudoCode(): String {
        return "${lhs.toPseudoCode()} = ${rhs.toPseudoCode()}"
    }

    override val children: List<Node>
        get() = listOf(lhs, rhs)
}

interface DeclarationHolder<T : Declaration> {
    val declarations: List<T>

    fun addDeclaration(declaration: T)
}

/** A block is a list of statements */
class Block(var statements: List<Statement> = mutableListOf()) :
    Node(null, ""), DeclarationHolder<Variable> {
    init {
        statements.forEach { it.parent = this }
    }

    private var locals: MutableList<Variable> = mutableListOf()

    override val children: List<Node>
        get() = statements

    override val declarations: List<Variable>
        get() = locals

    override fun addDeclaration(declaration: Variable) {
        this.locals.add(declaration)
    }

    operator fun plusAssign(stmt: Statement) {
        this.statements += stmt
    }
}

class DeclarationStatement(var declarations: List<Declaration>) : Statement(null, "") {
    override val children: List<Node>
        get() = declarations
}

/**
 * This pass creates implicit declaration statements for langauges that do not support explicit
 * variable declarations.
 */
class ImplicitDeclarator() {

    fun doPass(block: Block) {
        var refs = collect<Reference>(block)

        println(refs)
    }
}

inline fun <reified T> collect(start: Node): List<T> {
    val list = mutableListOf<T>()

    // TODO: use a worklist
    val seen = mutableListOf<Node>()
    val work = LinkedList<Node>()

    // initialize the worklist with our node
    work += start

    // loop until the work list is empty
    do {
        // get the next item from the work list
        var next = work.remove()

        // if we have already seen it, continue to avoid loops
        if (seen.contains(next)) {
            continue
        }

        // check, if next is our desired type, if yes, then we store it in the results
        if (next is T) {
            list += next
        }

        // add its children to the work-list
        work.addAll(next.children)

        // and add it to the seen list
        seen += next
    } while (work.isNotEmpty())

    return list
}
