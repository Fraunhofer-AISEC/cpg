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
package de.fraunhofer.aisec.cpg.graph.statements.expressions

import de.fraunhofer.aisec.cpg.persistence.AttributeConverter
import de.fraunhofer.aisec.cpg.persistence.Convert
import java.math.BigInteger
import java.util.*
import org.apache.commons.lang3.builder.ToStringBuilder

/**
 * Represents a literal value, meaning the value is fixed and not depending on the runtime
 * evaluation of the expression.
 *
 * @param <T> the literal type. </T>
 */
class Literal<T> : Expression() {
    @Convert(ValueConverter::class) var value: T? = null

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("value", value)
            .toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Literal<*>) return false
        return super.equals(other) && value == other.value
    }

    // include the value in the hash code, otherwise the hash set/map implementation falls back
    // to equals() because the node's hash code only depends on the name
    override fun hashCode() = Objects.hash(super.hashCode(), value)
}

class ValueConverter : AttributeConverter<Any?, Any?> {
    override fun toGraphProperty(value: Any?): Any? {
        // Neo4J only supports a limited set of primitive values natively, everything else, we need
        // to convert to a string.
        return when (value) {
            null -> null
            (value is Number && value !is BigInteger) -> value
            is Boolean -> value
            is String -> value
            else -> value.toString()
        }
    }

    override fun toEntityAttribute(value: Any?): Any? {
        // Note: this will most likely produce false results when we try to load back a BigInteger.
        // But loading the graph from a Neo4J database is not really supported because a lot of
        // things, such as scopes are not populated correctly.
        return value
    }
}
