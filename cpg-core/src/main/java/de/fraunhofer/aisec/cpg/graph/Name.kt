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

import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend

/** Represents a fully qualified name */
class Name
@JvmOverloads
constructor(
    var simpleName: String,
    var namespaceSeparator: String = ".",
    var scopes: MutableList<String>? = null
) {

    override fun toString(): String {
        return scopes?.joinToString(namespaceSeparator) + simpleName
    }

    operator fun plus(s: String): Name {
        return Name(simpleName + s, namespaceSeparator, scopes)
    }

    fun startsWith(prefix: String, ignoreCase: Boolean = false): Boolean {
        return simpleName.startsWith(prefix, ignoreCase)
    }

    fun replace(oldValue: String, newValue: String): Name {
        return Name(simpleName.replace(oldValue, newValue), namespaceSeparator)
    }

    fun firstScopeOrNull(): String? {
        return scopes?.firstOrNull()
    }

    val isQualified: Boolean
        get() {
            return scopes != null && scopes!!.isNotEmpty()
        }

    val fullyQualified: String
        get() {
            return scopes?.joinToString(namespaceSeparator) + simpleName
        }
}

fun Name?.isEmpty(): Boolean {
    return this?.scopes?.size == 0 && simpleName.isEmpty()
}

fun Name?.isNotEmpty(): Boolean {
    if (this == null) {
        return false
    }

    return scopes != null && scopes!!.isNotEmpty() && simpleName.isNotEmpty()
}

infix fun String.fqnize(lang: LanguageFrontend): Name {
    val rr = this.split(lang.namespaceDelimiter)

    val name =
        if (rr.size > 1) {
            Name(rr.last(), lang.namespaceDelimiter, rr.subList(0, rr.size - 1).toMutableList())
        } else {
            Name(this, lang.namespaceDelimiter)
        }

    if (lang.scopeManager.currentNamePrefix.isNotEmpty()) {
        if (name.scopes == null) {
            name.scopes = mutableListOf()
        }
        name.scopes?.add(0, lang.scopeManager.currentNamePrefix)
    }

    return name
}
