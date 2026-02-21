/*
 * Copyright (c) 2020, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.declarations

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgesOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.overlays.BasicBlock
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.persistence.DoNotPersist
import java.util.*
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/** The top most declaration, representing a translation unit, for example a file. */
class TranslationUnit : Declaration(), DeclarationHolder, StatementHolder, EOGStarterHolder {
    /** A list of declarations within this unit. */
    @Relationship(value = "DECLARATIONS", direction = Relationship.Direction.OUTGOING)
    val declarationEdges = astEdgesOf<Declaration>()
    override val declarations by unwrapping(TranslationUnit::declarationEdges)

    /** A list of includes within this unit. */
    val includes
        get() = declarations.filterIsInstance<Include>()

    /** A list of namespaces within this unit. */
    val namespaces
        get() = declarations.filterIsInstance<Namespace>()

    /** The list of statements. */
    @Relationship(value = "STATEMENTS", direction = Relationship.Direction.OUTGOING)
    override var statementEdges = astEdgesOf<Statement>()
    override var statements by unwrapping(TranslationUnit::statementEdges)

    override fun addDeclaration(declaration: Declaration) {
        addIfNotContains(declarationEdges, declaration)
    }

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .append("declarations", declarationEdges)
            .toString()
    }

    @DoNotPersist
    override val eogStarters: List<Node>
        get() {
            val list = mutableListOf<Node>()
            // Add all top-level declarations
            list += declarations
            // Add the TU itself, so that we can catch any static statements in the TU
            list += this

            return list
        }

    override var firstBasicBlock: BasicBlock? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TranslationUnit) return false
        return super.equals(other) && declarations == other.declarations
    }

    override fun hashCode() = Objects.hash(super.hashCode(), declarations)
}
