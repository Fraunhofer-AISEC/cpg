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

import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import java.util.*
import kotlin.uuid.Uuid

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
    val delimiter: String = ".",
) : Cloneable, Comparable<Name>, CharSequence {
    constructor(
        localName: String,
        parent: Name? = null,
        language: Language<*>?,
    ) : this(localName, parent, language?.namespaceDelimiter ?: ".")

    companion object {
        /**
         * Creates a temporary name starting with a prefix plus a UUID (version 4) seeded by the
         * hash code of [prefix] and [seed]. The Name is prefixed by [prefix], followed by a
         * separator character [separatorChar] and finalized by the UUID ("-" separators also
         * replaced with [separatorChar]).
         */
        fun temporary(prefix: String, separatorChar: Char = '_', vararg seed: Node): Name {
            val uuid =
                Uuid.fromLongs(prefix.hashCode().toLong(), seed.sumOf { it.hashCode().toLong() })
            return Name(localName = prefix + separatorChar + uuid)
        }
    }

    /**
     * The full string representation of this name. Since [localName] and [parent] are immutable,
     * this is basically a cache for [toString]. Otherwise, we would need to call [toString] a lot
     * of times, to implement the necessary functions for [CharSequence].
     */
    private val fullName: String by lazy {
        (if (parent != null) parent.toString() + delimiter else "") + localName
    }

    public override fun clone(): Name = Name(localName, parent?.clone(), delimiter)

    /**
     * This function splits a fully qualified name into its parts. For example,
     * `my::namespace::name` would be split into `["my::namespace::name", "my::namespace", "my"]`.
     */
    fun splitTo(out: MutableList<Name>): MutableList<Name> {
        var current: Name? = this
        while (current != null) {
            out += current
            current = current.parent
        }

        return out
    }

    /**
     * Returns the string representation of this name using a fully qualified name notation with the
     * specified [delimiter].
     */
    override fun toString() = fullName

    override val length = fullName.length

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is String) return this.fullName == other
        if (other is Name)
            return localName == other.localName &&
                parent == other.parent &&
                delimiter == other.delimiter

        return false
    }

    override fun get(index: Int) = fullName[index]

    override fun hashCode() = Objects.hash(localName, parent, delimiter)

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
        fullName.subSequence(startIndex, endIndex)

    /**
     * Determines if this name ends with the [ending] (i.e., the localNames match until the [ending]
     * has no parent anymore).
     */
    fun lastPartsMatch(ending: Name): Boolean =
        this.localName == ending.localName &&
            (ending.parent == null ||
                this.parent != null && this.parent.lastPartsMatch(ending.parent))

    /**
     * Determines if this name ends with the [ending] (i.e., the localNames match until the [ending]
     * has no parent anymore).
     */
    fun lastPartsMatch(ending: String) = this.lastPartsMatch(parseName(ending, this.delimiter))

    /** This function appends a string to the local name and returns a new [Name]. */
    fun append(s: String) = Name(localName + s, parent, delimiter)

    /**
     * This functions replaces all occurrences of [oldValue] with [newValue] in the local name and
     * returns a new [Name].
     */
    fun replace(oldValue: String, newValue: String) =
        Name(localName.replace(oldValue, newValue), parent, delimiter)

    /** Compare names according to the string representation of the [fullName]. */
    override fun compareTo(other: Name) = fullName.compareTo(other.toString())

    /**
     * A name can be considered as "qualified", if it has any specified [parent]. Otherwise, only
     * the [localName] is specified and the name is "unqualified".
     */
    fun isQualified(): Boolean {
        return parent != null
    }
}

/**
 * A small utility extension function that uses the language information in a [LanguageProvider]
 * (such as a [Node], a [Language], a [LanguageFrontend] or a [Handler]) to parse a fully qualified
 * name.
 */
fun LanguageProvider.parseName(fqn: CharSequence): Name {
    return parseName(fqn, this.language.namespaceDelimiter)
}

/** Tries to parse the given fully qualified name using the specified [delimiter] into a [Name]. */
internal fun parseName(fqn: CharSequence, delimiter: String, vararg splitDelimiters: String): Name {
    // We can take a shortcut, if this is already a name
    if (fqn is Name) {
        return fqn
    }

    val parts = fqn.split(delimiter, *splitDelimiters)

    var name: Name? = null
    for (part in parts) {
        name = Name(part, name, delimiter)
    }

    // Actually this should not occur, but otherwise the compiler won't let us return a
    // non-null Name
    if (name == null) {
        return Name(fqn.toString(), null, delimiter)
    }

    return name
}

/** Returns a new [Name] based on the [localName] and the current name as parent. */
fun Name?.fqn(localName: String, delimiter: String = this?.delimiter ?: ".") =
    if (this == null) {
        Name(localName, null, delimiter)
    } else {
        Name(localName, this, delimiter)
    }
