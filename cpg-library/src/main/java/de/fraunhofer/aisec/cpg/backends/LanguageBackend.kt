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
package de.fraunhofer.aisec.cpg.backends

import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.HasType
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.types.Type
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** A language backend is responsible to emit a Code Property Graph into a specific language. */
abstract class LanguageBackend<TypeClass> {
    val log: Logger = LoggerFactory.getLogger(LanguageFrontend::class.java)

    /**
     * The main entry point for a backend. This gets called for each [TranslationUnitDeclaration] in
     * the graph.
     */
    abstract fun generate(tu: TranslationUnitDeclaration)

    /**
     * This function extracts a language-specific [TypeClass] object out of a node, which implements
     * [HasType].
     */
    abstract fun typeOf(node: HasType): TypeClass

    /** This function converts a CPG [Type] into a language specific [TypeClass] object. */
    abstract fun typeFrom(type: Type): TypeClass
}
