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

import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.UnknownLanguage
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.helpers.CommonPath
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteBefore
import de.fraunhofer.aisec.cpg.passes.configuration.RequiredFrontend
import org.slf4j.LoggerFactory

@DependsOn(TypeHierarchyResolver::class)
@ExecuteBefore(JavaImportResolver::class)
@RequiredFrontend(JavaLanguageFrontend::class)
class JavaExternalTypeHierarchyResolver(ctx: TranslationContext) : ComponentPass(ctx) {
    override fun accept(component: Component) {
        val provider =
            object : ContextProvider, LanguageProvider, ScopeProvider {
                override val language: Language<*>
                    get() = ctx.availableLanguage<JavaLanguage>() ?: UnknownLanguage

                override val ctx: TranslationContext = this@JavaExternalTypeHierarchyResolver.ctx
                override val scope: Scope?
                    get() = scopeManager.globalScope
            }
        val resolver = CombinedTypeSolver()

        resolver.add(ReflectionTypeSolver())
        var root = ctx.currentComponent?.topLevel()
        if (root == null && config.softwareComponents.size == 1) {
            root =
                config.softwareComponents[config.softwareComponents.keys.first()]?.let {
                    CommonPath.commonPath(it)
                }
        }
        if (root == null) {
            log.warn("Could not determine source root for {}", config.softwareComponents)
        } else {
            log.info("Source file root used for type solver: {}", root)
            resolver.add(JavaParserTypeSolver(root))
        }

        // Iterate over all known types and add their (direct) supertypes.
        var types = typeManager.resolvedTypes.toList()
        for (t in types) {
            val symbol = resolver.tryToSolveType(t.typeName)
            if (symbol.isSolved) {
                val resolvedSuperTypes = symbol.correspondingDeclaration.getAncestors(true)
                for (anc in resolvedSuperTypes) {
                    // We need to try to resolve the type first in order to create weirdly
                    // scoped types
                    var superType = typeManager.lookupResolvedType(anc.qualifiedName)

                    // Otherwise, we can create this in the global scope
                    if (superType == null) {
                        superType = provider.objectType(anc.qualifiedName)
                        superType.typeOrigin = Type.Origin.RESOLVED
                    }

                    // Add all resolved supertypes to the type.
                    t.superTypes.add(superType)
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
