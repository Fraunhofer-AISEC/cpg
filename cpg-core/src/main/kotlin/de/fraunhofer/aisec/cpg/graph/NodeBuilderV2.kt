/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
@file:OptIn(ExperimentalContracts::class)

package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.types.Type
import kotlin.contracts.ExperimentalContracts

context(ContextProvider)
fun TranslationUnitDeclaration.globalScope(
    init: TranslationUnitDeclaration.() -> Unit
): TranslationUnitDeclaration {
    val ctx = ctx ?: throw TranslationException("context not available")

    ctx.scopeManager.resetToGlobal(this)
    init(this)

    return this
}

context(ContextProvider)
fun <T : Node> T.withScope(init: T.() -> Unit): T {
    val ctx = ctx ?: throw TranslationException("context not available")

    ctx.scopeManager.enterScope(this)
    init(this)
    ctx.scopeManager.leaveScope(this)

    return this
}

context(ContextProvider)
operator fun DeclarationHolder.plusAssign(declaration: Declaration) {
    addDeclaration(declaration)
    ctx?.scopeManager?.declare(declaration)
}

/** Creates a new [TranslationUnitDeclaration] with the given [name]. */
fun <T> RawNodeTypeProvider<T>.translationUnitDeclaration(
    name: CharSequence,
    rawNode: T? = null,
    init: (TranslationUnitDeclaration.() -> Unit)? = null
): TranslationUnitDeclaration {
    val node = TranslationUnitDeclaration()
    node.applyMetadata(this, name = name, rawNode = rawNode)

    init?.invoke(node)

    return node
}

/** Creates a new [FunctionDeclaration] with the given [name]. */
fun <T> RawNodeTypeProvider<T>.functionDeclaration(
    name: CharSequence,
    rawNode: T? = null,
    init: (FunctionDeclaration.() -> Unit)? = null
): FunctionDeclaration {
    val node = FunctionDeclaration()
    node.applyMetadata(this, name = name, rawNode = rawNode)

    init?.invoke(node)

    return node
}

/** Creates a new [ParameterDeclaration] with the given [name]. */
fun <T> RawNodeTypeProvider<T>.parameterDeclaration(
    name: CharSequence,
    type: Type,
    rawNode: T? = null,
    init: (ParameterDeclaration.() -> Unit)? = null
): ParameterDeclaration {
    val node = ParameterDeclaration()
    node.applyMetadata(this, name = name, rawNode = rawNode, localNameOnly = true)
    node.type = type

    init?.invoke(node)

    return node
}
