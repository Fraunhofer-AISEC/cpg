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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.declarations.Method
import de.fraunhofer.aisec.cpg.graph.declarations.Operator
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation

/** A simple interface that a node has [language]. */
interface HasLanguage {

    var language: Language<*>
}

/** A simple interface that a node has [name] and [location]. */
interface HasNameAndLocation : HasLanguage {

    val name: Name

    /** Location of the finding in source code. */
    val location: PhysicalLocation?
}

/** A simple interface that a node has [scope]. */
interface HasScope : HasNameAndLocation {

    /** The scope this node lives in. */
    val scope: Scope?
}

/** A simple interface to denote that the implementing class has some kind of [operatorCode]. */
interface HasOperatorCode : HasScope {

    /** The operator code, identifying an operation executed on one or more [Expression]s */
    val operatorCode: String?
}

/** Specifies that a certain node has a base on which it executes an operation. */
interface HasBase : HasOperatorCode {

    /** The base. If there is no actual base, it can be null. */
    val base: Expression?

    /**
     * The operator that is used to access the [base]. Usually either `.` or `->`, but some
     * languages offer additional operator codes. If the [base] is null, the [operatorCode] should
     * also be null.
     */
    override val operatorCode: String?
}

/**
 * Interface that allows us to mark nodes that contain a default value
 *
 * @param <T> type of the default node </T>
 */
interface HasDefault<T : Node?> : HasScope {
    var default: T
}

/**
 * Specifies that a certain node has an initializer. It is a special case of [ArgumentHolder], in
 * which the initializer is treated as the first (and only) argument.
 */
interface HasInitializer : HasScope, HasType, ArgumentHolder, AssignmentHolder {

    var initializer: Expression?

    override fun addArgument(expression: Expression) {
        this.initializer = expression
    }

    override fun removeArgument(expression: Expression): Boolean {
        return if (this.initializer == expression) {
            this.initializer = null
            true
        } else {
            false
        }
    }

    override fun replaceArgument(old: Expression, new: Expression): Boolean {
        this.initializer = new
        return true
    }

    override fun hasArgument(expression: Expression): Boolean {
        return initializer == expression
    }

    override val assignments: List<Assignment>
        get() {
            return initializer?.let { listOf(Assignment(it, this, this)) } ?: listOf()
        }
}

/**
 * Some nodes have aliases, i.e., it potentially references another variable. This means that
 * writing to this node, also writes to its [aliases] and vice versa.
 */
interface HasAliases : HasScope {
    /** The aliases which this node has. */
    var aliases: MutableSet<HasAliases>
}

/**
 * Specifies that this node (e.g. a [BinaryOperator]) contains an operation that can be overloaded
 * by an [Operator].
 */
interface HasOverloadedOperation : HasOperatorCode {

    /**
     * Arguments forwarded to the operator. These might not necessarily be all regular "arguments",
     * since often, the first argument is part of the [operatorBase].
     */
    val operatorArguments: List<Expression>

    /**
     * The base expression this operator works on. The [Type] of this is also the source where the
     * [SymbolResolver] is looking for an overloaded [Operator].
     */
    val operatorBase: Expression
}

/**
 * Specifies that a node has modifiers, e.g., determining its visibility (typically `public` or
 * `private) or other modifiers like `mut`. The modifiers are represented as a set of strings and
 * are specific to the language.
 *
 * Careful distinction between modifiers and types is necessary and might be different for different
 * languages, for example in C++ `int` is a type, but `const` or `volatile` are most likely
 * modifiers.
 */
interface HasModifiers {

    /**
     * The modifiers of this node. The actual modifiers are language-specific and can be any string,
     * but typical examples include `public`, `private`, `protected` for [Method]s and `mut` or
     * `volatile` for [Variable]s.
     */
    var modifiers: Set<String>
}
