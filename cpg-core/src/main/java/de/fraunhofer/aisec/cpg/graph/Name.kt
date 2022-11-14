/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import java.util.*
import kotlin.reflect.KProperty

/**
 * This class represents anything that can have a "Name". In the simplest case it only represents a
 * local name in a flat hierarchy, such as `myVariable`. However, it can also be used to represent
 * fully qualified name with a complex name hierarchy, such as `my::namespace::function`.
 */
class Name(
    /** The local name (sometimes also called simple name) without any namespace information. */
    var localName: String,
    /** The parent name, e.g., the namespace this name lives in. */
    var parent: Name? = null,
    /** A potential namespace delimiter, usually either `.` or `::`. */
    val delimiter: String = "."
) : Cloneable {
    constructor(
        localName: String,
        parent: Name? = null,
        language: Language<out LanguageFrontend>?
    ) : this(localName, parent, language?.namespaceDelimiter ?: ".")

    public override fun clone(): Name {
        return Name(localName, parent?.clone(), delimiter)
    }

    /**
     * Returns the string representation of this name using a fully qualified name notation with the
     * specified [delimiter].
     */
    override fun toString() =
        (if (parent != null) parent.toString() + delimiter else "") + localName

    /** Implements kotlin propety delegation for a string getter. Returns the local name. */
    operator fun getValue(node: Node, property: KProperty<*>) = localName

    /**
     * Implements kotlin property delegation for a string setter. Sets the local name to the
     * supplied string.
     */
    operator fun setValue(node: Node, property: KProperty<*>, s: String) {
        localName = s
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Name) return false

        return localName == other.localName &&
            parent == other.parent &&
            delimiter == other.delimiter
    }

    override fun hashCode(): Int {
        return Objects.hash(localName, parent, delimiter)
    }

    companion object {
        fun parse(fqn: String, language: Language<out LanguageFrontend>?): Name {
            return parse(
                fqn,
                language?.namespaceDelimiter ?: ".",
                *(language?.nameSplitter ?: arrayOf())
            )
        }
        /**
         * Tries to parse the given fully qualified name using the specified [delimiter] into a
         * [Name].
         */
        fun parse(fqn: String, delimiter: String = ".", vararg splitDelimiters: String): Name {
            val parts = fqn.split(delimiter, *splitDelimiters)

            var name: Name? = null
            for (part in parts) {
                val localName = part.replace(")", "").replace("*", "")
                if (localName.isNotEmpty()) {
                    name = Name(localName, name, delimiter)
                }
            }

            // Actually this should not occur, but otherwise the compiler won't let us return a
            // non-null Name
            if (name == null) {
                return Name(fqn, null, delimiter)
            }

            return name
        }
    }
}
