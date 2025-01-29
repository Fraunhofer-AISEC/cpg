/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.UnknownLanguage
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.scopes.RecordScope
import de.fraunhofer.aisec.cpg.graph.statements.ForEachStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.AssignExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.InitializerListExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.types.InitializerTypePropagation
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteBefore
import de.fraunhofer.aisec.cpg.passes.configuration.RequiredFrontend

@ExecuteBefore(ImportResolver::class)
@ExecuteBefore(SymbolResolver::class)
@RequiredFrontend(PythonLanguageFrontend::class)
class PythonAddDeclarationsPass(ctx: TranslationContext) : ComponentPass(ctx), LanguageProvider {

    lateinit var walker: SubgraphWalker.ScopedWalker

    override fun cleanup() {
        // nothing to do
    }

    override fun accept(p0: Component) {
        walker = SubgraphWalker.ScopedWalker(ctx.scopeManager)
        walker.registerHandler { _, _, currNode -> handle(currNode) }

        for (tu in p0.translationUnits) {
            walker.iterate(tu)
        }
    }

    /**
     * This function checks for each [AssignExpression] whether there is already a matching variable
     * or not. New variables can be one of:
     * - [FieldDeclaration] if we are currently in a record
     * - [VariableDeclaration] otherwise
     *
     * TODO: loops
     */
    private fun handle(node: Node?) {
        when (node) {
            is AssignExpression -> handleAssignExpression(node)
            is ForEachStatement -> handleForEach(node)
            else -> {
                // Nothing to do for all other types of nodes
            }
        }
    }

    /**
     * This function will create a new dynamic [VariableDeclaration] if there is a write access to
     * the [ref].
     */
    private fun handleWriteToReference(ref: Reference): VariableDeclaration? {
        if (ref.access != AccessValues.WRITE) {
            return null
        }

        // If this is a member expression, and we do not know the base's type, we cannot create a
        // declaration
        if (ref is MemberExpression && ref.base.type is UnknownType) {
            return null
        }

        // Look for a potential scope modifier for this reference
        // lookupScope
        var targetScope =
            scopeManager.currentScope?.predefinedLookupScopes[ref.name.toString()]?.targetScope

        // There are a couple of things to consider now
        var symbol =
            // Since this is a WRITE access, we need to look for a local symbol, unless
            //   - a global keyword is present for this symbol and scope
            //   - the name is qualified
            if (targetScope != null) {
                // When a target scope is set, then we have a `global` or `nonlocal` keyword for
                // this symbol, and we need to start looking in this scope
                scopeManager.lookupSymbolByNodeName(ref, targetScope)
            } else {
                scopeManager.lookupSymbolByNodeName(ref) {
                    // Otherwise, we need to stick to the current scope unless the name is qualified
                    it.scope == scopeManager.currentScope || ref.name.isQualified()
                }
            }

        // Nothing to create
        if (symbol.isNotEmpty()) return null

        // First, check if we need to create a field
        var decl: VariableDeclaration? =
            when {
                // Check, whether we are referring to a "self.X", which would create a field
                scopeManager.isInRecord && scopeManager.isInFunction && ref.refersToReceiver -> {
                    // We need to temporarily jump into the scope of the current record to
                    // add the field. These are instance attributes
                    scopeManager.withScope(scopeManager.firstScopeIsInstanceOrNull<RecordScope>()) {
                        newFieldDeclaration(ref.name)
                    }
                }
                scopeManager.isInRecord && scopeManager.isInFunction && ref is MemberExpression -> {
                    // If this is any other member expression, we are usually not interested in
                    // creating fields, except if this is a receiver
                    return null
                }
                scopeManager.isInRecord && !scopeManager.isInFunction -> {
                    // We end up here for fields declared directly in the class body. These are
                    // class attributes; more or less static fields.
                    newFieldDeclaration(scopeManager.currentNamespace.fqn(ref.name.localName))
                }
                else -> {
                    null
                }
            }

        // If we didn't create any declaration up to this point and are still here, we need to
        // create a (local) variable. We need to take scope modifications into account.
        if (decl == null) {
            decl =
                if (targetScope != null) {
                    scopeManager.withScope(targetScope) { newVariableDeclaration(ref.name) }
                } else {
                    newVariableDeclaration(ref.name)
                }
        }

        decl.code = ref.code
        decl.location = ref.location
        decl.isImplicit = true

        log.debug(
            "Creating dynamic {} {} in {}",
            if (decl is FieldDeclaration) {
                "field"
            } else {
                "variable"
            },
            decl.name,
            decl.scope,
        )

        // Make sure we add the declaration at the correct place, i.e. with the scope we set at the
        // creation time
        scopeManager.withScope(decl.scope) { scopeManager.addDeclaration(decl) }

        return decl
    }

    private val Reference.refersToReceiver: Boolean
        get() {
            return this is MemberExpression &&
                this.base.name == scopeManager.currentMethod?.receiver?.name
        }

    /**
     * Generates a new [VariableDeclaration] if [target] is a [Reference] and there is no existing
     * declaration yet.
     */
    private fun handleAssignmentToTarget(
        assignExpression: AssignExpression,
        target: Node,
        setAccessValue: Boolean = false,
    ) {
        (target as? Reference)?.let {
            if (setAccessValue) it.access = AccessValues.WRITE
            val handled = handleWriteToReference(target)
            if (handled is Declaration) {
                // We cannot assign an initializer here because this will lead to duplicate
                // DFG edges, but we need to propagate the type information from our value
                // to the declaration. We therefore add the declaration to the observers of
                // the value's type, so that it gets informed and can change its own type
                // accordingly.
                assignExpression
                    .findValue(target)
                    ?.registerTypeObserver(InitializerTypePropagation(handled))

                // Add it to our assign expression, so that we can find it in the AST
                assignExpression.declarations += handled
            }
        }
    }

    private fun handleAssignExpression(assignExpression: AssignExpression) {
        for (target in assignExpression.lhs) {
            handleAssignmentToTarget(assignExpression, target, setAccessValue = false)
            // If the lhs is an InitializerListExpression, we have to handle the individual elements
            // in the initializers.
            (target as? InitializerListExpression)?.let {
                it.initializers.forEach { initializer ->
                    handleAssignmentToTarget(assignExpression, initializer, setAccessValue = true)
                }
            }
        }
    }

    // New variables can also be declared as `variable` in a [ForEachStatement]
    private fun handleForEach(node: ForEachStatement) {
        when (val forVar = node.variable) {
            is Reference -> {
                val handled = handleWriteToReference(forVar)
                if (handled is Declaration) {
                    handled.let { node.addDeclaration(it) }
                }
            }
        }
    }

    override val language: Language<*>
        get() = ctx.config.languages.firstOrNull { it is PythonLanguage } ?: UnknownLanguage
}
