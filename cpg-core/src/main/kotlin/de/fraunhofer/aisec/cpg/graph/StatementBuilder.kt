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
import de.fraunhofer.aisec.cpg.graph.expressions.*

/**
 * Creates a new [Return]. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newReturn(rawNode: Any? = null): Return {
    val node = Return()
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
 * Creates a new [Try]. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newTry(rawNode: Any? = null): Try {
    val node = Try()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [Assert]. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newAssert(rawNode: Any? = null): Assert {
    val node = Assert()
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
 * Creates a new [Synchronized]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newSynchronized(rawNode: Any? = null): Synchronized {
    val node = Synchronized()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [Empty]. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newEmpty(rawNode: Any? = null): Empty {
    val node = Empty()
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
 * Creates a new [IfElse]. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newIfElse(rawNode: Any? = null): IfElse {
    val node = IfElse()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [Label]. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newLabel(rawNode: Any? = null): Label {
    val node = Label()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [Goto]. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newGoto(rawNode: Any? = null): Goto {
    val node = Goto()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [While]. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newWhile(rawNode: Any? = null): While {
    val node = While()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [DoWhile]. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newDoWhile(rawNode: Any? = null): DoWhile {
    val node = DoWhile()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [ForEach]. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newForEach(rawNode: Any? = null): ForEach {
    val node = ForEach()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [For]. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newFor(rawNode: Any? = null): For {
    val node = For()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [Continue]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newContinue(rawNode: Any? = null): Continue {
    val node = Continue()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [Break]. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newBreak(rawNode: Any? = null): Break {
    val node = Break()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [Switch]. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newSwitch(rawNode: Any? = null): Switch {
    val node = Switch()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [Case]. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newCase(rawNode: Any? = null): Case {
    val node = Case()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [Default]. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newDefault(rawNode: Any? = null): Default {
    val node = Default()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [LookupScope]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newLookupScope(
    symbols: List<Symbol>,
    targetScope: Scope?,
    rawNode: Any? = null,
): LookupScope {
    val node = LookupScope()
    node.targetScope = targetScope
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    // Add it to our scope
    for (symbol in symbols) {
        node.scope?.predefinedLookupScopes[symbol] = node
    }

    log(node)
    return node
}
