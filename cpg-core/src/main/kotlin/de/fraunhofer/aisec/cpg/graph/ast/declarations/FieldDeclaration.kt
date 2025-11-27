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
package de.fraunhofer.aisec.cpg.graph.ast.declarations

import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.Expression
import java.util.*
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/**
 * Declaration of a field within a [RecordDeclaration]. It contains the modifiers associated with
 * the field as well as an initializer [Expression] which provides an initial value for the field.
 */
class FieldDeclaration : VariableDeclaration() {
    /** Specifies, whether this field declaration is also a definition, i.e. has an initializer. */
    var isDefinition = false

    /** If this is only a declaration, this provides a link to the definition of the field. */
    @Relationship(value = "DEFINES")
    var definition: FieldDeclaration = this
        get() {
            return if (isDefinition) {
                this
            } else {
                field
            }
        }

    var modifiers: List<String> = mutableListOf()

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("initializer", initializer)
            .append("modifiers", modifiers)
            .toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is FieldDeclaration) {
            return false
        }
        return (super.equals(other) && modifiers == other.modifiers)
    }

    override fun hashCode() = Objects.hash(super.hashCode(), initializer, modifiers)
}
