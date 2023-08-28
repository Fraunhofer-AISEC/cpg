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
 * Creates a new [ReturnStmt]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newReturnStmt(code: String? = null, rawNode: Any? = null): ReturnStmt {
    val node = ReturnStmt()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

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
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    log(node)
    return node
}

/**
 * Creates a new [TryStmt]. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newTryStmt(code: String? = null, rawNode: Any? = null): TryStmt {
    val node = TryStmt()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    log(node)
    return node
}

/**
 * Creates a new [AssertStmt]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newAssertStmt(code: String? = null, rawNode: Any? = null): AssertStmt {
    val node = AssertStmt()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    log(node)
    return node
}

/**
 * Creates a new [ASMDeclStmt]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newASMDeclStmt(code: String? = null, rawNode: Any? = null): ASMDeclStmt {
    val node = ASMDeclStmt()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    log(node)
    return node
}

/**
 * Creates a new [SynchronizedStmt]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newSynchronizedStmt(
    code: String? = null,
    rawNode: Any? = null
): SynchronizedStmt {
    val node = SynchronizedStmt()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    log(node)
    return node
}

/**
 * Creates a new [EmptyStmt]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newEmptyStmt(code: String? = null, rawNode: Any? = null): EmptyStmt {
    val node = EmptyStmt()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    log(node)
    return node
}

/**
 * Creates a new [CompoundStmt]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newCompoundStmt(code: String? = null, rawNode: Any? = null): CompoundStmt {
    val node = CompoundStmt()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    log(node)
    return node
}

/**
 * Creates a new [DeclarationStmt]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newDeclarationStmt(
    code: String? = null,
    rawNode: Any? = null
): DeclarationStmt {
    val node = DeclarationStmt()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    log(node)
    return node
}

/**
 * Creates a new [IfStmt]. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newIfStmt(code: String? = null, rawNode: Any? = null): IfStmt {
    val node = IfStmt()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    log(node)
    return node
}

/**
 * Creates a new [LabelStmt]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newLabelStmt(code: String? = null, rawNode: Any? = null): LabelStmt {
    val node = LabelStmt()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    log(node)
    return node
}

/**
 * Creates a new [GotoStmt]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newGotoStmt(code: String? = null, rawNode: Any? = null): GotoStmt {
    val node = GotoStmt()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    log(node)
    return node
}

/**
 * Creates a new [WhileStmt]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newWhileStmt(code: String? = null, rawNode: Any? = null): WhileStmt {
    val node = WhileStmt()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    log(node)
    return node
}

/**
 * Creates a new [DoStmt]. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newDoStmt(code: String? = null, rawNode: Any? = null): DoStmt {
    val node = DoStmt()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    log(node)
    return node
}

/**
 * Creates a new [ForEachStmt]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newForEachStmt(code: String? = null, rawNode: Any? = null): ForEachStmt {
    val node = ForEachStmt()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    log(node)
    return node
}

/**
 * Creates a new [ForStmt]. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newForStmt(code: String? = null, rawNode: Any? = null): ForStmt {
    val node = ForStmt()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    log(node)
    return node
}

/**
 * Creates a new [ContinueStmt]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newContinueStmt(code: String? = null, rawNode: Any? = null): ContinueStmt {
    val node = ContinueStmt()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    log(node)
    return node
}

/**
 * Creates a new [BreakStmt]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newBreakStmt(code: String? = null, rawNode: Any? = null): BreakStmt {
    val node = BreakStmt()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    log(node)
    return node
}

/**
 * Creates a new [SwitchStmt]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newSwitchStmt(code: String? = null, rawNode: Any? = null): SwitchStmt {
    val node = SwitchStmt()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    log(node)
    return node
}

/**
 * Creates a new [CaseStmt]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newCaseStmt(code: String? = null, rawNode: Any? = null): CaseStmt {
    val node = CaseStmt()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    log(node)
    return node
}

/**
 * Creates a new [DefaultStmt]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newDefaultStmt(code: String? = null, rawNode: Any? = null): DefaultStmt {
    val node = DefaultStmt()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    log(node)
    return node
}
