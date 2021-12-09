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

open class Node(val name: String) {}

interface Symbol {}

interface Statement {}

abstract class Expression(name: String) : Node(name) {
    abstract fun toPseudoCode(): String
}

open class Reference(name: String) : Expression(name), Assignable {
    override fun toPseudoCode(): String {
        return name
    }
}

abstract class Declaration(name: String, val type: String) : Expression(name)

class VariableDeclaration(name: String, type: String) : Declaration(name, type), Assignable {
    override fun toPseudoCode(): String {
        return "$type $name"
    }
}

open class Literal<T>(private val value: T) : Expression(value.toString()) {
    override fun toPseudoCode(): String {
        return value.toString()
    }
}

open class BinaryOperation(
    var lhs: Expression,
    private val operator: String,
    var rhs: Expression,
) : Expression(operator) {

    override fun toPseudoCode(): String {
        return "${lhs.toPseudoCode()} ${operator} ${rhs.toPseudoCode()}"
    }
}

interface Assignable {
    fun toPseudoCode(): String
}

/**
 * An assignment assigns the expression on the right-hand-side ([rhs]] to the left-hand-side, which
 * needs to be [Assignable].
 */
class Assignment(lhs: Assignable, rhs: Expression) : BinaryOperation(lhs as Expression, "=", rhs) {

    override fun toPseudoCode(): String {
        return "${lhs.toPseudoCode()} = ${rhs.toPseudoCode()}"
    }
}
