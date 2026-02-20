/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.scopes

import com.fasterxml.jackson.annotation.JsonBackReference
import de.fraunhofer.aisec.cpg.PopulatedByPass
import de.fraunhofer.aisec.cpg.frontends.HasBuiltins
import de.fraunhofer.aisec.cpg.frontends.HasImplicitReceiver
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.AstNode
import de.fraunhofer.aisec.cpg.graph.ContextProvider
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.Import
import de.fraunhofer.aisec.cpg.graph.declarations.Typedef
import de.fraunhofer.aisec.cpg.graph.edges.scopes.ImportStyle
import de.fraunhofer.aisec.cpg.graph.edges.scopes.Imports
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.firstScopeParentOrNull
import de.fraunhofer.aisec.cpg.graph.statements.LabelStatement
import de.fraunhofer.aisec.cpg.graph.statements.LookupScopeStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.passes.ImportResolver
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.NodeEntity
import org.neo4j.ogm.annotation.Relationship

/**
 * A symbol is a simple, local name. It is valid within the scope that declares it and all of its
 * child scopes. However, a child scope can usually "shadow" a symbol of a higher scope.
 */
typealias Symbol = String

typealias SymbolMap = MutableMap<Symbol, MutableList<Declaration>>

/**
 * Represent semantic scopes in the language. Depending on the language scopes can have visibility
 * restriction and can act as namespaces to avoid name collisions.
 */
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
@NodeEntity
sealed class Scope(
    @Relationship(value = "SCOPE", direction = Relationship.Direction.INCOMING)
    @JsonBackReference
    open var astNode: AstNode?
) : Node() {

    /** FQN Name currently valid */
    var scopedName: String? = null

    /**
     * Scopes are nested and therefore have a parent child relationship, this two members will help
     * navigate through the scopes,e.g. when looking up variables.
     */
    @Relationship(value = "PARENT", direction = Relationship.Direction.OUTGOING)
    var parent: Scope? = null

    /** The list of child scopes. */
    @Transient
    @Relationship(value = "PARENT", direction = Relationship.Direction.INCOMING)
    var children = mutableListOf<Scope>()

    @Transient var labelStatements = mutableMapOf<String, LabelStatement>()

    /** A map of symbols and their respective [Declaration] nodes that declare them. */
    @Transient var symbols: SymbolMap = mutableMapOf()

    /**
     * A list of [Import] nodes that have an [ImportStyle.IMPORT_ALL_SYMBOLS_FROM_NAMESPACE] import
     * style ("wildcard" import).
     */
    @Transient var wildcardImports: MutableSet<Import> = mutableSetOf()

    /**
     * This set of edges is used to store [Import] edges that denotes foreign [NamespaceScope]
     * information that is imported into this scope. The edge holds information about the "style" of
     * the import (see [ImportStyle]) and the [Import] that is responsible for this. The property is
     * populated by the [ImportResolver].
     */
    @Relationship(value = "IMPORTS_SCOPE", direction = Relationship.Direction.OUTGOING)
    @PopulatedByPass(ImportResolver::class)
    val importedScopeEdges =
        Imports(this, mirrorProperty = NamespaceScope::importedByEdges, outgoing = true)

    /** Virtual property for accessing [importedScopeEdges] without property edges. */
    val importedScopes by unwrapping(Scope::importedScopeEdges)

    /**
     * In some languages, the lookup scope of a symbol that is being resolved (e.g. of a
     * [Reference]) can be adjusted through keywords (such as `global` in Python or PHP).
     *
     * We store this information in the form of a [LookupScopeStatement] in the AST, but we need to
     * also store this information in the scope to avoid unnecessary AST traversals when resolving
     * symbols using [lookupSymbol].
     */
    @Transient var predefinedLookupScopes: MutableMap<Symbol, LookupScopeStatement> = mutableMapOf()

    /**
     * A map of typedefs keyed by their alias name. This is still needed as a bridge until we
     * completely redesign the alias / typedef system.
     */
    @Transient val typedefs = mutableMapOf<Name, Typedef>()

    /**
     * Adds a [typedef] declaration to the scope. This is used to store typedefs in the scope, so
     * that they can be resolved later on.
     */
    fun addTypedef(typedef: Typedef) {
        typedefs[typedef.alias.name] = typedef
    }

    /** Adds a [declaration] with the defined [symbol]. */
    context(provider: ContextProvider)
    open fun addSymbol(symbol: Symbol, declaration: Declaration) {
        if (
            declaration is Import &&
                declaration.style == ImportStyle.IMPORT_ALL_SYMBOLS_FROM_NAMESPACE
        ) {
            // Because a wildcard import does not really have a valid "symbol", we store it in a
            // separate list
            wildcardImports += declaration
        } else {
            val list = symbols.computeIfAbsent(symbol) { mutableListOf() }
            list += declaration
        }
    }

    /**
     * Looks up a list of [Declaration] nodes for the specified [symbol]. Optionally, [predicate]
     * can be used for additional filtering.
     *
     * By default, the lookup algorithm will go to the [Scope.parent] if no match was found in the
     * current scope. This behaviour can be turned off with [qualifiedLookup]. This is useful for
     * qualified lookups, where we want to stay in our lookup-scope.
     *
     * We need to consider the language trait [HasImplicitReceiver] here as well. If the language
     * requires explicit member access, we must not consider symbols from record scopes unless we
     * are in a qualified lookup.
     *
     * @param symbol the symbol to lookup
     * @param qualifiedLookup whether the lookup is looked to a specific namespace, and we therefore
     *   should stay in the current scope for lookup. If the lookup is unqualified we traverse the
     *   current scopes parents if no match was found.
     * @param replaceImports whether any symbols pointing to [Import.importedSymbols] or wildcards
     *   should be replaced with their actual nodes
     * @param predicate An optional predicate which should be used in the lookup.
     */
    context(provider: ContextProvider)
    fun lookupSymbol(
        symbol: Symbol,
        languageOnly: Language<*>? = null,
        qualifiedLookup: Boolean = false,
        replaceImports: Boolean = true,
        predicate: ((Declaration) -> Boolean)? = null,
    ): List<Declaration> {
        // First, try to look for the symbol in the current scope (unless we have a predefined
        // search scope). In the latter case we also need to restrict the lookup to the search scope
        var modifiedScoped = this.predefinedLookupScopes[symbol]?.targetScope
        var scope: Scope? = modifiedScoped ?: this

        var list: MutableList<Declaration>? = null

        while (scope != null) {
            list = scope.symbols[symbol]?.toMutableList()

            // Also add any wildcard imports that we have to the list
            val wildcards = scope.wildcardImports
            if (list == null) {
                list = wildcards.toMutableList()
            } else {
                list.addAll(wildcards.toMutableList())
            }

            // We need to resolve any imported symbols
            if (replaceImports) {
                list.replaceImports(symbol)
            }

            // Filter according to the language
            if (languageOnly != null) {
                list.removeIf { it.language != languageOnly }
            }

            // Filter the list according to the predicate, if we have any
            if (predicate != null) {
                list.removeIf { !predicate.invoke(it) }
            }

            // If we have a hit, we can break the loop
            if (list.isNotEmpty()) {
                break
            }

            // If we do not have a hit, we can go up one scope, unless [qualifiedLookup] is set to
            // true
            // (or we had a modified scope)
            scope =
                if (qualifiedLookup || modifiedScoped != null) {
                    break
                } else {
                    // If our language needs explicit lookup for fields (and other class members),
                    // we need to skip record scopes unless we are in a qualified lookup
                    if (languageOnly !is HasImplicitReceiver && scope.parent is RecordScope) {
                        scope.firstScopeParentOrNull { it !is RecordScope }
                    } else {
                        // Otherwise, we can just go to the next parent
                        scope.parent
                    }
                }
        }

        // If the symbol was still not resolved, and we are performing an unqualified resolution, we
        // search in the
        // language's builtins scope for the symbol
        val scopeManager = provider.ctx.scopeManager
        if (list.isNullOrEmpty() && !qualifiedLookup && languageOnly is HasBuiltins) {
            // If the language has builtins we can search there for the symbol
            val builtinsNamespace = languageOnly.builtinsNamespace
            // Retrieve the builtins scope from the builtins namespace
            val builtinsScope = scopeManager.lookupScope(builtinsNamespace)
            if (builtinsScope != null) {
                // Obviously we don't want to search in the builtins scope if we already failed
                // finding the symbol in the builtins scope
                if (builtinsScope != this) {
                    list =
                        builtinsScope
                            .lookupSymbol(
                                symbol,
                                languageOnly = languageOnly,
                                replaceImports = replaceImports,
                                predicate = predicate,
                            )
                            .toMutableList()
                }
            }
        }

        return list ?: listOf()
    }

    fun addLabelStatement(labelStatement: LabelStatement) {
        labelStatement.label?.let { labelStatements[it] = labelStatement }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Scope

        if (astNode != other.astNode) return false
        return name == other.name
    }

    override fun hashCode(): Int {
        var result = astNode?.hashCode() ?: 0
        result = 31 * result + name.hashCode()
        return result
    }

    /** Returns the [GlobalScope] of this scope by traversing its parents upwards. */
    val globalScope: Scope?
        get() {
            var scope: Scope? = this
            while (scope !is GlobalScope) {
                if (scope == null) {
                    return null
                }

                scope = scope.parent
            }

            return scope
        }

    override fun toString(): String {
        val builder = ToStringBuilder(this, TO_STRING_STYLE)

        if (name.isNotEmpty() == true) {
            builder.append("name", name)
        }

        return builder.toString()
    }
}

/**
 * This function loops through all [Import] nodes in the [MutableSet] and resolves the imports to a
 * set of [Import.importedSymbols] with the name [symbol]. The [Import] is then removed from the
 * list.
 */
private fun MutableList<Declaration>.replaceImports(symbol: Symbol) {
    val imports = this.filterIsInstance<Import>()
    for (import in imports) {
        val set = import.importedSymbols[symbol]
        if (set != null) {
            this.addAll(set)
        }

        this.remove(import)
    }
}

/** This function merges in all entries from the [symbolMap] into the current [SymbolMap]. */
fun SymbolMap.mergeFrom(symbolMap: SymbolMap) {
    for (entry in symbolMap) {
        val list = this.computeIfAbsent(entry.key) { mutableListOf() }
        list += entry.value
    }
}
