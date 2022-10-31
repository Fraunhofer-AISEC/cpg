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
package de.fraunhofer.aisec.cpg.frontends

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import java.io.File

/**
 * Represents a programming language. When creating new languages in the CPG, one must derive custom
 * class from this and override the necessary fields and methods.
 *
 * Furthermore, since this also implements [Node], one node for each programming language used is
 * persisted in the final graph (database) and each node links to its corresponding language using
 * the [Node.language] property.
 */
abstract class Language<T : LanguageFrontend> : Node() {
    /** The file extensions without the dot */
    abstract val fileExtensions: List<String>

    abstract val namespaceDelimiter: String

    abstract val frontend: Class<out T>

    open val primitiveTypes: Set<String>
        get() = setOf("byte", "short", "int", "long", "float", "double", "boolean", "char")

    abstract fun newFrontend(
        config: TranslationConfiguration,
        scopeManager: ScopeManager = ScopeManager()
    ): T

    fun handlesFile(file: File): Boolean {
        return file.extension in fileExtensions
    }

    init {
        this.also { this.language = it }
    }
}
