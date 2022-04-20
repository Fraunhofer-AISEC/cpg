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
package de.fraunhofer.aisec.cpg.passes

import com.github.javaparser.resolution.UnsolvedSymbolException
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import org.slf4j.LoggerFactory

class JavaExternalTypeHierarchyResolver : Pass() {
    override fun accept(translationResult: TranslationResult) {
        // Run only for Java.
        if (lang is JavaLanguageFrontend) {
            val resolver = (lang as JavaLanguageFrontend).nativeTypeResolver

            // Iterate over all known types and add their (direct) supertypes.
            for (t in TypeManager.getInstance().firstOrderTypes.toSet()) {
                val symbol = resolver.tryToSolveType(t.typeName)
                if (symbol.isSolved) {
                    try {
                        val resolvedSuperTypes = symbol.correspondingDeclaration.getAncestors(true)
                        for (anc in resolvedSuperTypes) {
                            // Add all resolved supertypes to the type.
                            val superType = TypeParser.createFrom(anc.qualifiedName, false)
                            superType.typeOrigin = Type.Origin.RESOLVED
                            t.superTypes.add(superType)
                        }
                    } catch (e: UnsolvedSymbolException) {
                        // Even if the symbol itself is resolved, "getAncestors()" may throw an
                        // exception.
                        LOGGER.warn(
                            "Could not resolve supertypes of {}",
                            symbol.correspondingDeclaration
                        )
                    }
                }
            }
        }
    }

    override fun cleanup() {
        // nothing to do here.
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(JavaExternalTypeHierarchyResolver::class.java)
    }
}
