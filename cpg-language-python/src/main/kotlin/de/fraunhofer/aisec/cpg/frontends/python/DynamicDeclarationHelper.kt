/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.python

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.scopes.RecordScope
import de.fraunhofer.aisec.cpg.graph.statements.ForEachStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.AssignExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.types.InitializerTypePropagation
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.passes.Pass
import de.fraunhofer.aisec.cpg.passes.Pass.Companion.log

/**
 * This function will create a new dynamic [VariableDeclaration] if there is a write access to the
 * [ref].
 */
internal fun Pass<*>.handleWriteToReference(ref: Reference): VariableDeclaration? {
    if (ref.access != AccessValues.WRITE) {
        return null
    }

    // If this is a member expression, and we do not know the base's type, we cannot create a
    // declaration
    if (ref is MemberExpression && ref.base.type is UnknownType) {
        return null
    }

    // Look for a potential scope modifier for this reference
    var targetScope =
        scopeManager.currentScope?.predefinedLookupScopes[ref.name.toString()]?.targetScope

    // Try to see whether our symbol already exists. There are basically three rules to follow
    // here.
    var symbol =
        when {
            // When a target scope is set, then we have a `global` or `nonlocal` keyword for
            // this symbol, and we need to start looking in this scope
            targetScope != null -> scopeManager.lookupSymbolByNodeName(ref, targetScope)
            // When we have a qualified reference (such as `self.a`), we do not have any
            // specific restrictions, because the lookup will anyway be a qualified lookup,
            // and it will consider only the scope of `self`.
            ref.name.isQualified() -> scopeManager.lookupSymbolByNodeName(ref)
            // In any other case, we need to restrict the lookup to the current scope. The
            // main reason for this is that Python requires the `global` keyword in functions
            // for assigning to global variables. See
            // https://docs.python.org/3/reference/simple_stmts.html#the-global-statement. So
            // basically we need to ignore all global variables at this point and only look
            // for local ones.
            else ->
                scopeManager.lookupSymbolByNodeName(ref) { it.scope == scopeManager.currentScope }
        }

    // If the symbol is already defined in the designed scope, there is nothing to create
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
    decl.language = ref.language

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

context(Pass<*>)
private val Reference.refersToReceiver: Boolean
    get() {
        return this is MemberExpression &&
            this.base.name == scopeManager.currentMethod?.receiver?.name
    }

/**
 * Generates a new [VariableDeclaration] if [target] is a [Reference] and there is no existing
 * declaration yet.
 */
internal fun Pass<*>.handleAssignmentToTarget(
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

/*private fun Pass<*>.handleAssignExpression(assignExpression: AssignExpression) {
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
}*/

// New variables can also be declared as `variable` in a [ForEachStatement]
private fun Pass<*>.handleForEach(node: ForEachStatement) {
    when (val forVar = node.variable) {
        is Reference -> {
            val handled = handleWriteToReference(forVar)
            if (handled is Declaration) {
                handled.let { node.addDeclaration(it) }
            }
        }
    }
}
