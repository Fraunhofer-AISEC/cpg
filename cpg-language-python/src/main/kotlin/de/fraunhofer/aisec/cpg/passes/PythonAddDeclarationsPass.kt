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
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Field
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.expressions.*
import de.fraunhofer.aisec.cpg.graph.scopes.RecordScope
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.graph.types.InitializerTypePropagation
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.SecondOrderType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteBefore
import de.fraunhofer.aisec.cpg.passes.configuration.RequiresLanguage
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy

@ExecuteBefore(ImportResolver::class)
@ExecuteBefore(SymbolResolver::class)
@RequiresLanguage(PythonLanguage::class)
@Description("Adds declarations for python's variables as the CPG requires them.")
class PythonAddDeclarationsPass(ctx: TranslationContext) :
    TranslationUnitPass(ctx), LanguageProvider {

    var walker: SubgraphWalker.ScopedWalker<AstNode> =
        SubgraphWalker.ScopedWalker(ctx.scopeManager, Strategy::AST_FORWARD)

    override fun cleanup() {
        // nothing to do
    }

    init {
        walker.registerHandler { node -> handle(node) }
    }

    override fun accept(tu: TranslationUnit) {
        walker.iterate(tu)
    }

    /**
     * This function checks for each [Assign], [Comprehension] and [ForEach] whether there is
     * already a matching variable or not. New variables can be one of:
     * - [Field] if we are currently in a record
     * - [Variable] otherwise
     */
    private fun handle(node: Node?) {
        when (node) {
            is Comprehension -> handleComprehension(node)
            is Assign -> handleAssign(node)
            is ForEach -> handleForEach(node)
            else -> {
                // Nothing to do for all other types of nodes
            }
        }
    }

    /**
     * This function will create a new dynamic [Variable] if there is a write access to the [ref].
     */
    private fun handleWriteToReference(ref: Reference): Variable? {
        if (ref.access != AccessValues.WRITE) {
            return null
        }

        // If this is a member expression, and we do not know the base's type, we cannot create a
        // declaration. However, we need to exclude the receiver (self) because its type is
        // not yet resolved at this point, but we still need to
        // create fields for assignments.
        if (ref is MemberAccess && ref.base.type is UnknownType && !ref.refersToReceiver) {
            return null
        }

        // Look for a potential scope modifier for this reference
        val targetScope =
            scopeManager.currentScope.predefinedLookupScopes[ref.name.toString()]?.targetScope

        // Try to see whether our symbol already exists. There are basically three rules to follow
        // here.
        val symbol =
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
                    scopeManager.lookupSymbolByNodeName(ref) {
                        it.scope == scopeManager.currentScope
                    }
            }

        // If the symbol is already defined in the designed scope, there is nothing to create
        if (symbol.isNotEmpty()) return null

        // First, check if we need to create a field
        var decl: Variable? =
            when {
                // Check, whether we are referring to a "self.X", which would create a field
                scopeManager.isInRecord && scopeManager.isInFunction && ref.refersToReceiver -> {
                    val recordScope = scopeManager.firstScopeIsInstanceOrNull<RecordScope>()
                    // If the field already exists in the record scope, do not create a duplicate
                    if (recordScope?.symbols?.containsKey(ref.name.localName) == true) {
                        return null
                    }
                    // We need to temporarily jump into the scope of the current record to
                    // add the field. These are instance attributes.
                    //
                    // The name is built from the record rather than taken from `ref`, because the
                    // name of a member access is qualified by the type of its base: `self` has no
                    // annotation, so `self._pool` arrives as `UNKNOWN._pool`. A field stored under
                    // that name is never found by a lookup on the record, so every later
                    // `self._pool.<method>()` resolves against an unknown receiver -- even where
                    // the field's own assigned type is perfectly well known. The class-attribute
                    // branch below already names its fields this way.
                    scopeManager.withScope(recordScope) {
                        newField(scopeManager.currentNamespace.fqn(ref.name.localName))
                    }
                }
                scopeManager.isInRecord && scopeManager.isInFunction && ref is MemberAccess -> {
                    // If this is any other member expression, we are usually not interested in
                    // creating fields, except if this is a receiver
                    return null
                }
                scopeManager.isInRecord && !scopeManager.isInFunction -> {
                    // We end up here for fields declared directly in the class body. These are
                    // class attributes; more or less static fields.
                    newField(scopeManager.currentNamespace.fqn(ref.name.localName))
                }
                else -> {
                    null
                }
            }

        // Let the language turn the declaration's "modifiers" into semantics. For Python this
        // projects the PEP 8 naming convention onto the visibility of a record member. A field is
        // always a record member, but the current scope is not necessarily the record scope: for
        // instance attributes (`self.x`) we are inside a method here. We therefore hand over the
        // record scope the field belongs to.
        (decl as? Field)?.let { field ->
            language.applyModifiers(field, scopeManager.firstScopeIsInstanceOrNull<RecordScope>())
        }

        // If we didn't create any declaration up to this point and are still here, we need to
        // create a (local) variable. We need to take scope modifications into account.
        if (decl == null) {
            decl =
                if (targetScope != null) {
                    scopeManager.withScope(targetScope) { newVariable(ref.name) }
                } else {
                    newVariable(ref.name)
                }
        }

        decl.code = ref.code
        decl.location = ref.location
        decl.isImplicit = true

        log.debug(
            "Creating dynamic {} {} in {}",
            if (decl is Field) {
                "field"
            } else {
                "variable"
            },
            decl.name,
            decl.scope,
        )

        // Make sure we add the declaration at the correct place, i.e. with the scope we set at the
        // creation time
        scopeManager.withScope(decl.scope) {
            scopeManager.addDeclaration(decl)
            if (decl is Field) {
                (it?.astNode as? DeclarationHolder)?.addDeclaration(decl)
            }
            decl
        }

        return decl
    }

    private val Reference.refersToReceiver: Boolean
        get() {
            return this is MemberAccess &&
                this.base.name == scopeManager.currentMethod?.receiver?.name
        }

    /**
     * Generates a new [Variable] for [Reference] (and those included in a [InitializerList]) in the
     * [Comprehension.variable].
     */
    private fun handleComprehension(comprehensionExpression: Comprehension) {
        when (val variable = comprehensionExpression.variable) {
            is Reference -> {
                variable.access = AccessValues.WRITE
                handleWriteToReference(variable)?.let {
                    if (it !is Field) {
                        comprehensionExpression.addDeclaration(it)
                    }
                }
            }
            is InitializerList -> {
                variable.initializers.forEach {
                    (it as? Reference)?.let { ref ->
                        ref.access = AccessValues.WRITE
                        handleWriteToReference(ref)?.let {
                            if (it !is Field) {
                                comprehensionExpression.addDeclaration(it)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Generates a new [Variable] if [target] is a [Reference] and there is no existing declaration
     * yet.
     */
    private fun handleAssignmentToTarget(
        assignExpression: Assign,
        target: Node,
        setAccessValue: Boolean = false,
    ) {
        (target as? Reference)?.let {
            if (setAccessValue) it.access = AccessValues.WRITE
            val handled = handleWriteToReference(target)
            if (handled != null) {
                // We cannot assign an initializer here because this will lead to duplicate
                // DFG edges, but we need to propagate the type information from our value
                // to the declaration. We therefore add the declaration to the observers of
                // the value's type, so that it gets informed and can change its own type
                // accordingly.
                assignExpression
                    .findValue(target)
                    ?.registerTypeObserver(InitializerTypePropagation(handled))

                // Propagate type annotations from the LHS reference itself. An annotated
                // assignment `self._connections: list[AsyncConnectionInterface] = []` stores
                // the parsed annotation type on the reference's `assignedTypes`. The value
                // side (`[]`) only knows it is a `list`, not what it contains, so without this
                // the field's type is `list` and every member call on an element is unresolved.
                if (target.assignedTypes.isNotEmpty()) {
                    log.debug(
                        "Propagating {} assigned types from {} to field {}: {}",
                        target.assignedTypes.size,
                        target.name,
                        handled.name,
                        target.assignedTypes,
                    )
                    handled.addAssignedTypes(target.assignedTypes)
                }

                if (handled !is Field) {
                    // Add it to our assign expression, so that we can find it in the AST
                    assignExpression.declarations += handled
                }
            }
        }
    }

    private fun handleAssign(assignExpression: Assign) {
        val parentCollectionComprehensions = ArrayDeque<CollectionComprehension>()
        var parentCollectionComprehension =
            assignExpression.firstParentOrNull<CollectionComprehension>()
        while (assignExpression.operatorCode == ":=" && parentCollectionComprehension != null) {
            scopeManager.leaveScope(parentCollectionComprehension)
            parentCollectionComprehensions.addLast(parentCollectionComprehension)
            parentCollectionComprehension =
                parentCollectionComprehension.firstParentOrNull<CollectionComprehension>()
        }
        for (target in assignExpression.lhs) {
            handleAssignmentToTarget(assignExpression, target, setAccessValue = false)
            // If the lhs is an InitializerList, we have to handle the individual elements
            // in the initializers.
            (target as? InitializerList)?.let {
                it.initializers.forEach { initializer ->
                    handleAssignmentToTarget(assignExpression, initializer, setAccessValue = true)
                }
            }
        }
        while (
            assignExpression.operatorCode == ":=" && parentCollectionComprehensions.isNotEmpty()
        ) {
            scopeManager.enterScope(parentCollectionComprehensions.removeLast())
        }
    }

    // New variables can also be declared as `variable` in a [ForEach]
    private fun handleForEach(node: ForEach) {
        when (val forVar = node.variable) {
            is Reference -> {
                val handled = handleWriteToReference(forVar)
                if (handled != null) {
                    if (handled !is Field) {
                        node.addDeclaration(handled)
                    }
                    // Register a type observer on the iterable so the loop variable gets the
                    // element type when the iterable's type is resolved. At pass time the type
                    // is usually UnknownType because SymbolResolver hasn't run yet; the observer
                    // fires later when the type becomes a ListType/SetType/MapType.
                    val iterable = node.iterable
                    if (iterable != null) {
                        val observer = ElementTypePropagation(handled, iterable)
                        iterable.registerTypeObserver(observer)
                        // If the iterable is a container constructor call like `list(self._items)`,
                        // the call's return type is ListType(elementType=object) — the built-in
                        // default. The real element type is on the argument (the annotated field).
                        // Register the observer on the argument too, so when the field's type is
                        // resolved the element type propagates to the loop variable.
                        if (iterable is Call) {
                            val arg = iterable.arguments.firstOrNull()
                            if (arg is HasType) {
                                arg.registerTypeObserver(observer)
                            }
                        }
                    }
                }
            }
        }
    }

    override val language: Language<*>
        get() = ctx.availableLanguage<PythonLanguage>() ?: UnknownLanguage
}

/**
 * A type observer that extracts the element type from a container (ListType, SetType, MapType) and
 * propagates it to the loop variable's [assignedTypes].
 *
 * At the time [PythonAddDeclarationsPass] runs, the iterable's type is usually [UnknownType]
 * because [SymbolResolver] hasn't run yet. This observer fires later, when the type is resolved to
 * a [SecondOrderType], and propagates the element type to the declaration.
 *
 * Without this, `for conn in self._connections` (where `_connections` is annotated
 * `list[AsyncConnectionInterface]`) leaves `conn` untyped and `conn.is_closed()` is unresolved,
 * even though the annotation spells out exactly what `conn` is.
 */
private class ElementTypePropagation(
    private val decl: HasType,
    private val iterable: Expression? = null,
) : HasType.TypeObserver {

    override fun typeChanged(newType: Type, src: HasType) {
        propagateElementType(newType)
        // Handle `for x in list(self.items):` — the list() call returns
        // ListType(elementType=object), but the argument carries the real element type.
        propagateFromCallArgument()
    }

    override fun assignedTypeChanged(assignedTypes: Set<Type>, src: HasType) {
        for (t in assignedTypes) {
            propagateElementType(t)
        }
        propagateFromCallArgument()
    }

    private fun propagateFromCallArgument() {
        val call = iterable as? Call ?: return
        val arg = call.arguments.firstOrNull() ?: return
        for (t in arg.assignedTypes) {
            propagateElementType(t)
        }
        propagateElementType(arg.type)
    }

    private fun propagateElementType(type: Type) {
        if (type is SecondOrderType) {
            val elemType = type.elementType
            // Skip UnknownType and bare "object" — these carry no useful information.
            // The real element type may arrive later via propagateFromCallArgument().
            if (elemType !is UnknownType && elemType.name.localName != "object") {
                decl.addAssignedTypes(setOf(elemType))
            } else if (type is ObjectType && type.generics.isNotEmpty()) {
                val g = type.generics.first()
                if (g !is UnknownType && g.name.localName != "object") {
                    decl.addAssignedTypes(setOf(g))
                }
            }
        }
    }
}
