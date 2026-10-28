/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.cxx

import de.fraunhofer.aisec.cpg.Profile
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.types.IntegerType
import de.fraunhofer.aisec.cpg.graph.types.NumericType
import de.fraunhofer.aisec.cpg.graph.types.Type

/**
 * A [Profile] for the C and C++ standard library. It can be activated using
 * [de.fraunhofer.aisec.cpg.TranslationConfiguration.Builder.profile] and saves us from parsing the
 * standard library headers:
 * - It provides the types which the headers define, but which are not part of the language itself:
 *   `size_t`, `ssize_t` (POSIX), `ptrdiff_t`, `intptr_t` and `uintptr_t`. Without the headers (or
 *   this profile), they would be inferred as unknown record types, so arithmetic on them has an
 *   unknown type and calls with parameters of these types do not resolve. A typedef that the code
 *   or a loaded header declares takes precedence over them.
 * - It provides data flow summaries for common functions, such as `memcpy` or `strcpy` (see
 *   `profiles/stdlib.yml`), which are used for all calls to a function of this name that has no
 *   body.
 *
 * To get the precise behavior, leave the profile out and load the real headers (see
 * [de.fraunhofer.aisec.cpg.TranslationConfiguration.Builder.loadIncludes]).
 */
object StdlibProfile : Profile("stdlib") {
    override fun builtInTypes(language: Language<*>): Map<String, Type> {
        if (language !is CLanguage) {
            return emptyMap()
        }

        // We assume a 64-bit platform, like for wchar_t. TODO: Use the architecture of the target
        // environment, which would also cover 32-bit platforms.
        return with(language) {
            mapOf(
                "size_t" to IntegerType("size_t", 64, this, NumericType.Modifier.UNSIGNED),
                "ssize_t" to IntegerType("ssize_t", 64, this, NumericType.Modifier.SIGNED),
                "ptrdiff_t" to IntegerType("ptrdiff_t", 64, this, NumericType.Modifier.SIGNED),
                "intptr_t" to IntegerType("intptr_t", 64, this, NumericType.Modifier.SIGNED),
                "uintptr_t" to IntegerType("uintptr_t", 64, this, NumericType.Modifier.UNSIGNED),
            )
        }
    }

    override val functionSummaries = listOf("profiles/stdlib.yml")
}
