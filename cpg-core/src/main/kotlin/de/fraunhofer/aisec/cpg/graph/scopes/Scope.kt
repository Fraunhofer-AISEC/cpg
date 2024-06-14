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
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.Node.Companion.TO_STRING_STYLE
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.ImportDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.LabelStatement
import de.fraunhofer.aisec.cpg.helpers.neo4j.NameConverter
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.GeneratedValue
import org.neo4j.ogm.annotation.Id
import org.neo4j.ogm.annotation.NodeEntity
import org.neo4j.ogm.annotation.Relationship
import org.neo4j.ogm.annotation.typeconversion.Convert

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
@NodeEntity
abstract class Scope(
    @Relationship(value = "SCOPE", direction = Relationship.Direction.INCOMING)
    @JsonBackReference
    open var astNode: Node?
) {

    /** Required field for object graph mapping. It contains the scope id. */
    @Id @GeneratedValue var id: Long? = null

    /** FQN Name currently valid */
    var scopedName: String? = null

    /** The real new name */
    @Convert(NameConverter::class) var name: Name? = null

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
     * A list of [ImportDeclaration] nodes that have [ImportDeclaration.wildcardImport] set to true.
     */
    @Transient var wildcardImports: MutableSet<ImportDeclaration> = mutableSetOf()

    /** Adds a [declaration] with the defined [symbol]. */
    fun addSymbol(symbol: Symbol, declaration: Declaration) {
        if (declaration is ImportDeclaration && declaration.wildcardImport) {
            // Because a wildcard import does not really have a valid "symbol", we store it in a
            // separate list
            wildcardImports += declaration
        } else {
            val list = symbols.computeIfAbsent(symbol) { mutableListOf() }
            list += declaration
        }
    }

    /** Looks up a list of [Declaration] nodes for the specified [symbol]. */
    fun lookupSymbol(symbol: Symbol): List<Declaration> {
        // First, try to look for the symbol in the current scope
        var scope: Scope? = this
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
            list.replaceImports(symbol)

            // If we have a hit, we can break the loop
            if (list.isNotEmpty()) {
                break
            }

            // If we do not have a hit, we can go up one scope
            scope = scope.parent
        }

        return list ?: listOf()
    }

    fun addLabelStatement(labelStatement: LabelStatement) {
        labelStatement.label?.let { labelStatements[it] = labelStatement }
    }

    fun isBreakable(): Boolean {
        return this is LoopScope || this is SwitchScope
    }

    fun isContinuable(): Boolean {
        return this is LoopScope
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
        result = 31 * result + (name?.hashCode() ?: 0)
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

        if (name?.isNotEmpty() == true) {
            builder.append("name", name)
        }

        return builder.toString()
    }

    fun addSymbols(other: MutableMap<Symbol, MutableSet<Declaration>>) {
        for ((key, value) in other.entries) {
            val list = this.symbols.computeIfAbsent(key) { mutableListOf() }
            list += value
        }
    }
}

/**
 * This function loops through all [ImportDeclaration] nodes in the [MutableSet] and resolves the
 * imports to a set of [ImportDeclaration.importedSymbols] with the name [symbol]. The
 * [ImportDeclaration] is then removed from the list.
 */
private fun MutableList<Declaration>.replaceImports(symbol: Symbol) {
    val imports = this.filterIsInstance<ImportDeclaration>()
    for (import in imports) {
        val set = import.importedSymbols[symbol]
        if (set != null) {
            this.addAll(set)
            this.remove(import)
        }
    }
}

/** This function merges in all entries from the [symbolMap] into the current [SymbolMap]. */
fun SymbolMap.mergeFrom(symbolMap: SymbolMap) {
    for (entry in symbolMap) {
        val list = this.computeIfAbsent(entry.key) { mutableListOf() }
        list += entry.value
    }
}
