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
import de.fraunhofer.aisec.cpg.graph.statements.*

/**
 * Creates a new [ReturnStatement]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newReturnStatement(
    code: String? = null,
    rawNode: Any? = null
): ReturnStatement {
    val node = ReturnStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code)

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
fun MetadataProvider.newCatchClause(code: String? = null, rawNode: Any? = null): CatchClause {
    val node = CatchClause()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code)

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
fun MetadataProvider.newTryStatement(code: String? = null, rawNode: Any? = null): TryStatement {
    val node = TryStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code)

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
fun MetadataProvider.newAssertStatement(
    code: String? = null,
    rawNode: Any? = null
): AssertStatement {
    val node = AssertStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code)

    log(node)
    return node
}

/**
 * Creates a new [ASMDeclarationStatement]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newASMDeclarationStatement(
    code: String? = null,
    rawNode: Any? = null
): ASMDeclarationStatement {
    val node = ASMDeclarationStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code)

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
fun MetadataProvider.newSynchronizedStatement(
    code: String? = null,
    rawNode: Any? = null
): SynchronizedStatement {
    val node = SynchronizedStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code)

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
fun MetadataProvider.newEmptyStatement(code: String? = null, rawNode: Any? = null): EmptyStatement {
    val node = EmptyStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code)

    log(node)
    return node
}

/**
 * Creates a new [CompoundStatement]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newCompoundStatement(
    code: String? = null,
    rawNode: Any? = null
): CompoundStatement {
    val node = CompoundStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code)

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
fun MetadataProvider.newDeclarationStatement(
    code: String? = null,
    rawNode: Any? = null
): DeclarationStatement {
    val node = DeclarationStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code)

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
fun MetadataProvider.newIfStatement(code: String? = null, rawNode: Any? = null): IfStatement {
    val node = IfStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code)

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
fun MetadataProvider.newLabelStatement(code: String? = null, rawNode: Any? = null): LabelStatement {
    val node = LabelStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code)

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
fun MetadataProvider.newGotoStatement(code: String? = null, rawNode: Any? = null): GotoStatement {
    val node = GotoStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code)

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
fun MetadataProvider.newWhileStatement(code: String? = null, rawNode: Any? = null): WhileStatement {
    val node = WhileStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code)

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
fun MetadataProvider.newDoStatement(code: String? = null, rawNode: Any? = null): DoStatement {
    val node = DoStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code)

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
fun MetadataProvider.newForEachStatement(
    code: String? = null,
    rawNode: Any? = null
): ForEachStatement {
    val node = ForEachStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code)

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
fun MetadataProvider.newForStatement(code: String? = null, rawNode: Any? = null): ForStatement {
    val node = ForStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code)

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
fun MetadataProvider.newContinueStatement(
    code: String? = null,
    rawNode: Any? = null
): ContinueStatement {
    val node = ContinueStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code)

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
fun MetadataProvider.newBreakStatement(code: String? = null, rawNode: Any? = null): BreakStatement {
    val node = BreakStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code)

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
fun MetadataProvider.newSwitchStatement(
    code: String? = null,
    rawNode: Any? = null
): SwitchStatement {
    val node = SwitchStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code)

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
fun MetadataProvider.newCaseStatement(code: String? = null, rawNode: Any? = null): CaseStatement {
    val node = CaseStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code)

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
fun MetadataProvider.newDefaultStatement(
    code: String? = null,
    rawNode: Any? = null
): DefaultStatement {
    val node = DefaultStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code)

    log(node)
    return node
}
