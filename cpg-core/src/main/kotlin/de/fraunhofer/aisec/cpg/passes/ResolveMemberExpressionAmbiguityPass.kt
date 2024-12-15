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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.HasCallExpressionAmbiguity
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.codeAndLocationFrom
import de.fraunhofer.aisec.cpg.graph.declarations.NamespaceDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.fqn
import de.fraunhofer.aisec.cpg.graph.imports
import de.fraunhofer.aisec.cpg.graph.newReference
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.translationUnit
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.helpers.replace
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteBefore
import de.fraunhofer.aisec.cpg.passes.configuration.RequiresLanguageTrait

/**
 * A translation unit pass that resolves ambiguities in member expressions within a translation
 * unit. This pass checks whether the base or member name in a member expression refers to an import
 * and, if so, replaces the member expression with a reference using the fully qualified name.
 *
 * This pass is dependent on the [ImportResolver] pass and requires the language trait
 * [HasCallExpressionAmbiguity]. It is executed before the [EvaluationOrderGraphPass].
 *
 * @constructor Initializes the pass with the provided translation context.
 */
@ExecuteBefore(EvaluationOrderGraphPass::class)
@DependsOn(ImportResolver::class)
@RequiresLanguageTrait(HasCallExpressionAmbiguity::class)
class ResolveMemberExpressionAmbiguityPass(ctx: TranslationContext) : TranslationUnitPass(ctx) {

    lateinit var walker: SubgraphWalker.ScopedWalker

    override fun accept(tu: TranslationUnitDeclaration) {
        walker = SubgraphWalker.ScopedWalker(ctx.scopeManager)
        walker.registerHandler { _, _, node ->
            when (node) {
                is MemberExpression -> resolveAmbiguity(node)
            }
        }

        walker.iterate(tu)
    }

    /**
     * Resolves ambiguities in a given member expression. Checks whether the base or member name of
     * the member expression refers to an import, and if so, replaces the member expression with a
     * reference that uses the fully qualified name.
     *
     * @param me The member expression to disambiguate and potentially replace.
     */
    private fun resolveAmbiguity(me: MemberExpression) {
        // We need to check, if our "base" (or our expression) is really a name that refers to an
        // import, because in this case we do not have a member expression, but a reference with a
        // qualified name
        val baseName = me.base.reconstructedImportName
        var isImportedNamespace = isImportedNamespace(baseName, me)

        if (isImportedNamespace) {
            with(me) {
                val ref = newReference(baseName.fqn(me.name.localName)).codeAndLocationFrom(this)
                walker.replace(me.astParent, me, ref)
            }
        }
    }

    private fun isImportedNamespace(name: Name, me: MemberExpression): Boolean {
        val resolved =
            scopeManager.lookupSymbolByName(
                name,
                language = me.language,
                location = me.location,
                startScope = me.scope
            )
        var isImportedNamespace = resolved.singleOrNull() is NamespaceDeclaration
        if (!isImportedNamespace) {
            // It still could be an imported namespace of an imported package that we do not know.
            // The problem is that we do not really know at this point whether we import a
            // (sub)module or a global variable of the namespace. We tend to assume that this is a
            // namespace
            val imports = me.translationUnit.imports
            isImportedNamespace =
                imports.any {
                    var toMatch = it.name
                    it.alias?.let { toMatch = it }
                    toMatch.lastPartsMatch(name) || toMatch.startsWith(name)
                }
        }
        return isImportedNamespace
    }

    override fun cleanup() {
        // Nothing to do
    }
}

/**
 * This utility function tries to reconstruct the name as if the expression was part of an imported
 * symbol. This is needed because the [MemberExpression.name] includes the [MemberExpression.base]'s
 * type instead of the name, and thus it might be "UNKNOWN".
 */
val Expression.reconstructedImportName: Name
    get() {
        return if (this is MemberExpression) {
            this.base.reconstructedImportName.fqn(this.name.localName)
        } else {
            this.name
        }
    }
