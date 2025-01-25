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
import de.fraunhofer.aisec.cpg.frontends.HasMemberExpressionAmbiguity
import de.fraunhofer.aisec.cpg.graph.HasBase
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.codeAndLocationFrom
import de.fraunhofer.aisec.cpg.graph.declarations.ImportDeclaration
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
@ExecuteBefore(ResolveCallExpressionAmbiguityPass::class)
@DependsOn(ImportResolver::class)
@RequiresLanguageTrait(HasMemberExpressionAmbiguity::class)
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
        val importName = referredImportName(baseName, me)

        if (importName != null) {
            with(me) {
                val ref = newReference(importName.fqn(me.name.localName)).codeAndLocationFrom(this)
                walker.replace(me.astParent, me, ref)
            }
        }
    }

    /**
     * This function checks if the given name refers to an import, e.g., because it directly has the
     * name of an import or if its parent name does. If it does refer to an import, then the
     * function returns the fully qualified name of the import. If the name does not refer to an
     * import, returns null.
     *
     * The check happens in two stages:
     * - First, the function looks up the name in the current scope. If a symbol is found that
     *   represents a [NamespaceDeclaration], the name of the declaration is returned.
     * - Second, if no symbol is found in the current scope, the function looks up the name in the
     *   list of [ImportDeclaration]s of the translation unit. If an import is found that matches
     *   the name, the name of the import is returned.
     *
     * @param name The name to check for an import.
     * @param hint The expression that hints at the language and location.
     * @return The fully qualified name of the import if the name refers to an import, or null
     */
    private fun referredImportName(name: Name, hint: Expression): Name? {
        val resolved =
            scopeManager.lookupSymbolByName(
                name,
                language = hint.language,
                location = hint.location,
                startScope = hint.scope,
            )
        var declaration = resolved.singleOrNull()
        var isImportedNamespace = declaration is NamespaceDeclaration
        if (!isImportedNamespace) {
            // It still could be an imported namespace of an imported package that we do not know.
            // The problem is that we do not really know at this point whether we import a
            // (sub)module or a global variable of the namespace. We tend to assume that this is a
            // namespace
            val imports = hint.translationUnit.imports
            declaration =
                imports.firstOrNull { it.name.lastPartsMatch(name) || it.name.startsWith(name) }
            isImportedNamespace = declaration != null
        }

        return declaration?.name
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
        return if (this is HasBase) {
            this.base?.reconstructedImportName.fqn(this.name.localName)
        } else {
            this.name
        }
    }
