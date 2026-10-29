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
package de.fraunhofer.aisec.cpg

import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.types.Type

/**
 * A [Profile] bundles knowledge about code that is *not* part of the analyzed project, e.g., the
 * standard library of a language, so that it does not have to be parsed (or inferred) every time.
 *
 * A frontend can make a profile active by default ([RegisterDefaultProfile]); other profiles are
 * activated with [TranslationConfiguration.Builder.profile]. The default profiles can be turned off
 * ([TranslationConfiguration.Builder.useDefaultProfiles]) to get the precise behavior of parsing
 * the real sources, e.g., the headers of the standard library. A profile is meant as a shortcut and
 * as a fallback for missing sources: whatever the analyzed code (or a loaded header) declares
 * itself takes precedence over what a profile provides.
 *
 * A profile can contribute
 * - [builtInTypes], i.e., types which the language does not know itself, and
 * - [functionSummaries], i.e., DFG summaries for functions whose body is not available.
 */
abstract class Profile(
    /** A short, unique name of the profile, e.g., `stdlib`. */
    val name: String
) {
    /**
     * Returns the types this profile contributes to [language], by name. They are looked up after
     * the types of [Language.builtInTypes]. The default is an empty map, i.e., none.
     */
    open fun builtInTypes(language: Language<*>): Map<String, Type> = emptyMap()

    /**
     * The class path resources of function summary files (JSON or YAML, in the format described in
     * the documentation) of this profile. Summaries registered explicitly by the user with
     * [TranslationConfiguration.Builder.registerFunctionSummaries] take precedence. Use a path
     * specific to the frontend, e.g., `profiles/cxx/stdlib.yml`: the frontends usually share one
     * class path, so a resource of the same name in another module would be found instead.
     */
    open val functionSummaries: List<String> = emptyList()

    /** The class loader used to load [functionSummaries]. */
    open val classLoader: ClassLoader
        get() = this::class.java.classLoader

    override fun toString() = "Profile($name)"
}
