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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.Node.Companion.EMPTY_NAME
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.log
import de.fraunhofer.aisec.cpg.graph.ast.statements.AssertStatement
import de.fraunhofer.aisec.cpg.graph.ast.statements.BreakStatement
import de.fraunhofer.aisec.cpg.graph.ast.statements.CaseStatement
import de.fraunhofer.aisec.cpg.graph.ast.statements.CatchClause
import de.fraunhofer.aisec.cpg.graph.ast.statements.ContinueStatement
import de.fraunhofer.aisec.cpg.graph.ast.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.ast.statements.DefaultStatement
import de.fraunhofer.aisec.cpg.graph.ast.statements.DistinctLanguageBlock
import de.fraunhofer.aisec.cpg.graph.ast.statements.DoStatement
import de.fraunhofer.aisec.cpg.graph.ast.statements.EmptyStatement
import de.fraunhofer.aisec.cpg.graph.ast.statements.ForEachStatement
import de.fraunhofer.aisec.cpg.graph.ast.statements.ForStatement
import de.fraunhofer.aisec.cpg.graph.ast.statements.GotoStatement
import de.fraunhofer.aisec.cpg.graph.ast.statements.IfStatement
import de.fraunhofer.aisec.cpg.graph.ast.statements.LabelStatement
import de.fraunhofer.aisec.cpg.graph.ast.statements.LookupScopeStatement
import de.fraunhofer.aisec.cpg.graph.ast.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.ast.statements.SwitchStatement
import de.fraunhofer.aisec.cpg.graph.ast.statements.SynchronizedStatement
import de.fraunhofer.aisec.cpg.graph.ast.statements.TryStatement
import de.fraunhofer.aisec.cpg.graph.ast.statements.WhileStatement
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.graph.scopes.Symbol

/**
 * Creates a new [ast.statements.ReturnStatement]. The [MetadataProvider] receiver will be used to
 * fill different meta-data using [Node.applyMetadata]. Calling this extension function outside of
 * Kotlin requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newReturnStatement(rawNode: Any? = null): ReturnStatement {
    val node = ReturnStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [ast.statements.CatchClause]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newCatchClause(rawNode: Any? = null): CatchClause {
    val node = CatchClause()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [ast.statements.TryStatement]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newTryStatement(rawNode: Any? = null): TryStatement {
    val node = TryStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [ast.statements.AssertStatement]. The [MetadataProvider] receiver will be used to
 * fill different meta-data using [Node.applyMetadata]. Calling this extension function outside of
 * Kotlin requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newAssertStatement(rawNode: Any? = null): AssertStatement {
    val node = AssertStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [ast.statements.DistinctLanguageBlock]. The [MetadataProvider] receiver will be
 * used to fill different meta-data using [Node.applyMetadata]. Calling this extension function
 * outside of Kotlin requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an
 * additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newDistinctLanguageBlock(rawNode: Any? = null): DistinctLanguageBlock {
    val node = DistinctLanguageBlock()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [ast.statements.SynchronizedStatement]. The [MetadataProvider] receiver will be
 * used to fill different meta-data using [Node.applyMetadata]. Calling this extension function
 * outside of Kotlin requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an
 * additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newSynchronizedStatement(rawNode: Any? = null): SynchronizedStatement {
    val node = SynchronizedStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [ast.statements.EmptyStatement]. The [MetadataProvider] receiver will be used to
 * fill different meta-data using [Node.applyMetadata]. Calling this extension function outside of
 * Kotlin requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newEmptyStatement(rawNode: Any? = null): EmptyStatement {
    val node = EmptyStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [ast.statements.DeclarationStatement]. The [MetadataProvider] receiver will be used
 * to fill different meta-data using [Node.applyMetadata]. Calling this extension function outside
 * of Kotlin requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an
 * additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newDeclarationStatement(rawNode: Any? = null): DeclarationStatement {
    val node = DeclarationStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [ast.statements.IfStatement]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newIfStatement(rawNode: Any? = null): IfStatement {
    val node = IfStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [ast.statements.LabelStatement]. The [MetadataProvider] receiver will be used to
 * fill different meta-data using [Node.applyMetadata]. Calling this extension function outside of
 * Kotlin requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newLabelStatement(rawNode: Any? = null): LabelStatement {
    val node = LabelStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [ast.statements.GotoStatement]. The [MetadataProvider] receiver will be used to
 * fill different meta-data using [Node.applyMetadata]. Calling this extension function outside of
 * Kotlin requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newGotoStatement(rawNode: Any? = null): GotoStatement {
    val node = GotoStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [ast.statements.WhileStatement]. The [MetadataProvider] receiver will be used to
 * fill different meta-data using [Node.applyMetadata]. Calling this extension function outside of
 * Kotlin requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newWhileStatement(rawNode: Any? = null): WhileStatement {
    val node = WhileStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [ast.statements.DoStatement]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newDoStatement(rawNode: Any? = null): DoStatement {
    val node = DoStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [ast.statements.ForEachStatement]. The [MetadataProvider] receiver will be used to
 * fill different meta-data using [Node.applyMetadata]. Calling this extension function outside of
 * Kotlin requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newForEachStatement(rawNode: Any? = null): ForEachStatement {
    val node = ForEachStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [ast.statements.ForStatement]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newForStatement(rawNode: Any? = null): ForStatement {
    val node = ForStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [ast.statements.ContinueStatement]. The [MetadataProvider] receiver will be used to
 * fill different meta-data using [Node.applyMetadata]. Calling this extension function outside of
 * Kotlin requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newContinueStatement(rawNode: Any? = null): ContinueStatement {
    val node = ContinueStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [ast.statements.BreakStatement]. The [MetadataProvider] receiver will be used to
 * fill different meta-data using [Node.applyMetadata]. Calling this extension function outside of
 * Kotlin requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newBreakStatement(rawNode: Any? = null): BreakStatement {
    val node = BreakStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [ast.statements.SwitchStatement]. The [MetadataProvider] receiver will be used to
 * fill different meta-data using [Node.applyMetadata]. Calling this extension function outside of
 * Kotlin requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newSwitchStatement(rawNode: Any? = null): SwitchStatement {
    val node = SwitchStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [ast.statements.CaseStatement]. The [MetadataProvider] receiver will be used to
 * fill different meta-data using [Node.applyMetadata]. Calling this extension function outside of
 * Kotlin requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newCaseStatement(rawNode: Any? = null): CaseStatement {
    val node = CaseStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [ast.statements.DefaultStatement]. The [MetadataProvider] receiver will be used to
 * fill different meta-data using [Node.applyMetadata]. Calling this extension function outside of
 * Kotlin requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newDefaultStatement(rawNode: Any? = null): DefaultStatement {
    val node = DefaultStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [ast.statements.LookupScopeStatement]. The [MetadataProvider] receiver will be used
 * to fill different meta-data using [Node.applyMetadata]. Calling this extension function outside
 * of Kotlin requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an
 * additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newLookupScopeStatement(
    symbols: List<Symbol>,
    targetScope: Scope?,
    rawNode: Any? = null,
): LookupScopeStatement {
    val node = LookupScopeStatement()
    node.targetScope = targetScope
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    // Add it to our scope
    for (symbol in symbols) {
        node.scope?.predefinedLookupScopes[symbol] = node
    }

    log(node)
    return node
}
