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

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.UnknownLanguage
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteBefore
import de.fraunhofer.aisec.cpg.passes.configuration.RequiresLanguage
import org.slf4j.LoggerFactory

@DependsOn(TypeHierarchyResolver::class)
@ExecuteBefore(JavaImportResolver::class)
@RequiresLanguage(JavaLanguage::class)
@Description(
    "Adds some java types and their hierarchy information that are not part of the analyzed code (e.g., from the standard library) to the CPG's type hierarchy."
)
class JavaExternalTypeHierarchyResolver(ctx: TranslationContext) : TranslationResultPass(ctx) {
    override fun accept(result: TranslationResult) {
        val provider =
            object : ContextProvider, LanguageProvider, ScopeProvider {
                override val language: Language<*>
                    get() = ctx.availableLanguage<JavaLanguage>() ?: UnknownLanguage

                override val ctx: TranslationContext = this@JavaExternalTypeHierarchyResolver.ctx
                override val scope: Scope?
                    get() = scopeManager.globalScope
            }
        val language = ctx.availableLanguage<JavaLanguage>() ?: return

        // The types are global, so we only need to resolve them once. However, components with
        // different source roots have different type solvers, so we try all of them.
        val resolvers = result.components.map { language.typeSolverFor(ctx, it) }.distinct()

        // Index the resolved types by their name, so that we do not need to look up each super
        // type in all resolved types
        val typesByName =
            typeManager.resolvedTypes
                .filter {
                    it.typeOrigin == Type.Origin.RESOLVED || it.typeOrigin == Type.Origin.GUESSED
                }
                .groupBy { it.root.name.toString() }
                .mapValuesTo(HashMap()) { it.value.first() }

        // The same type name can occur multiple times (e.g., with different generics), so we cache
        // the names of its ancestors
        val ancestors = HashMap<String, List<String>>()

        // Iterate over all known types and add their (direct) supertypes.
        val types = typeManager.resolvedTypes.toList()
        for (t in types) {
            val ancestorNames =
                ancestors.getOrPut(t.typeName) {
                    val symbol =
                        resolvers.firstNotNullOfOrNull { resolver ->
                            resolver.tryToSolveType(t.typeName).takeIf { it.isSolved }
                        }
                    symbol?.correspondingDeclaration?.getAncestors(true)?.map { it.qualifiedName }
                        ?: listOf()
                }

            for (name in ancestorNames) {
                // We need to try to resolve the type first in order to create weirdly scoped
                // types. Otherwise, we can create this in the global scope
                val superType =
                    typesByName.getOrPut(name) {
                        provider.objectType(name).also { it.typeOrigin = Type.Origin.RESOLVED }
                    }

                // Add all resolved supertypes to the type.
                t.superTypes.add(superType)
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
