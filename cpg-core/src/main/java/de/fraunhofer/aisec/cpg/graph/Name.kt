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

/**
 * This class represents anything that can have a "Name". In the simplest case it only represents a
 * local name in a flat hierarchy, such as `myVariable`. However, it can also be used to represent
 * fully qualified name with a complex name hierarchy, such as `my::namespace::function`.
 */
class Name(
    /** The local name (sometimes also called simple name) without any namespace information. */
    val localName: String,
    /** The parent name, e.g., the namespace this name lives in. */
    val parent: Name? = null,
    /** A potential namespace delimiter, usually either `.` or `::`. */
    val delimiter: String = "."
) : Cloneable, Comparable<Name>, CharSequence {
    constructor(
        localName: String,
        parent: Name? = null,
        language: Language<out LanguageFrontend>?
    ) : this(localName, parent, language?.namespaceDelimiter ?: ".")

    /**
     * The full string representation of this name. Since [localName] and [parent] are immutable,
     * this is basically a cache for [toString]. Otherwise, we would need to call [toString] a lot
     * of times, to implement the necessary functions for [CharSequence].
     */
    val fullName: String
    init {
        fullName = (if (parent != null) parent.toString() + delimiter else "") + localName
    }

    public override fun clone(): Name {
        return Name(localName, parent?.clone(), delimiter)
    }

    /**
     * Returns the string representation of this name using a fully qualified name notation with the
     * specified [delimiter].
     */
    override fun toString() = fullName

    override val length: Int
        get() = fullName.length

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Name) return false

        return localName == other.localName &&
            parent == other.parent &&
            delimiter == other.delimiter
    }

    override fun get(index: Int) = fullName[index]

    override fun hashCode(): Int {
        return Objects.hash(localName, parent, delimiter)
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
        fullName.subSequence(startIndex, endIndex)

    /**
     * Determines if this name ends with the [ending] (i.e., the localNames match until the [ending]
     * has no parent anymore).
     */
    fun endsWith(ending: Name): Boolean {
        return this.localName == ending.localName &&
            (ending.parent == null || this.parent != null && this.parent.endsWith(ending.parent))
    }

    /**
     * Determines if this name ends with the [ending] (i.e., the localNames match until the [ending]
     * has no parent any more).
     */
    fun endsWith(ending: String): Boolean {
        return this.endsWith(parseName(ending, this.delimiter))
    }

    /** This function appends a string to the local name and returns a new [Name]. */
    fun append(s: String): Name {
        return Name(localName + s, parent, delimiter)
    }

    override fun compareTo(other: Name): Int {
        // Compare names according to the string representation of the full name
        return fullName.compareTo(other.toString())
    }
}

/**
 * A small utility extension function that uses the namespace information in a [Language] to parse a
 * fully qualified name.
 */
fun Language<out LanguageFrontend>?.parseName(fqn: CharSequence): Name {
    return parseName(fqn, this?.namespaceDelimiter ?: ".", *(this?.nameSplitter ?: arrayOf()))
}

/** Tries to parse the given fully qualified name using the specified [delimiter] into a [Name]. */
fun parseName(fqn: CharSequence, delimiter: String = ".", vararg splitDelimiters: String): Name {
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
        return Name(fqn.toString(), null, delimiter)
    }

    return name
}
