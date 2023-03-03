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
import de.fraunhofer.aisec.cpg.graph.statements.LabelStatement
import de.fraunhofer.aisec.cpg.helpers.neo4j.NameConverter
import org.neo4j.ogm.annotation.GeneratedValue
import org.neo4j.ogm.annotation.Id
import org.neo4j.ogm.annotation.NodeEntity
import org.neo4j.ogm.annotation.Relationship
import org.neo4j.ogm.annotation.typeconversion.Convert

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

    fun addLabelStatement(labelStatement: LabelStatement) {
        labelStatement.label?.let { labelStatements[it] = labelStatement }
    }

    fun isBreakable(): Boolean {
        return this is LoopScope || this is SwitchScope
    }

    fun isContinuable(): Boolean {
        return this is LoopScope
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
}
