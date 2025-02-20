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
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.graph.scopes.Symbol
import de.fraunhofer.aisec.cpg.graph.statements.*

/**
 * Creates a new [ReturnStatement]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newReturnStatement(rawNode: Any? = null): ReturnStatement {
    val node = ReturnStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [CatchClause]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newCatchClause(rawNode: Any? = null): CatchClause {
    val node = CatchClause()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [TryStatement]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newTryStatement(rawNode: Any? = null): TryStatement {
    val node = TryStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [AssertStatement]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newAssertStatement(rawNode: Any? = null): AssertStatement {
    val node = AssertStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [DistinctLanguageBlock]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newDistinctLanguageBlock(rawNode: Any? = null): DistinctLanguageBlock {
    val node = DistinctLanguageBlock()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [SynchronizedStatement]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newSynchronizedStatement(rawNode: Any? = null): SynchronizedStatement {
    val node = SynchronizedStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [EmptyStatement]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newEmptyStatement(rawNode: Any? = null): EmptyStatement {
    val node = EmptyStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [DeclarationStatement]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newDeclarationStatement(rawNode: Any? = null): DeclarationStatement {
    val node = DeclarationStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [IfStatement]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newIfStatement(rawNode: Any? = null): IfStatement {
    val node = IfStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [LabelStatement]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newLabelStatement(rawNode: Any? = null): LabelStatement {
    val node = LabelStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [GotoStatement]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newGotoStatement(rawNode: Any? = null): GotoStatement {
    val node = GotoStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [WhileStatement]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newWhileStatement(rawNode: Any? = null): WhileStatement {
    val node = WhileStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [DoStatement]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newDoStatement(rawNode: Any? = null): DoStatement {
    val node = DoStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [ForEachStatement]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newForEachStatement(rawNode: Any? = null): ForEachStatement {
    val node = ForEachStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [ForStatement]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newForStatement(rawNode: Any? = null): ForStatement {
    val node = ForStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [ContinueStatement]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newContinueStatement(rawNode: Any? = null): ContinueStatement {
    val node = ContinueStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [BreakStatement]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newBreakStatement(rawNode: Any? = null): BreakStatement {
    val node = BreakStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [SwitchStatement]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newSwitchStatement(rawNode: Any? = null): SwitchStatement {
    val node = SwitchStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [CaseStatement]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newCaseStatement(rawNode: Any? = null): CaseStatement {
    val node = CaseStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [DefaultStatement]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newDefaultStatement(rawNode: Any? = null): DefaultStatement {
    val node = DefaultStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [LookupScopeStatement]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
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
