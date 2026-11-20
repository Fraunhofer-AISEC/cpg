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
package de.fraunhofer.aisec.cpg.frontends.python

/**
 * Checks that a `requirements.txt` only consists of packages from an index, i.e., that installing
 * it cannot make `pip` or `uv` build anything from the files of the project or from a URL.
 *
 * `--only-binary :all:` is not enough for this: a local path (`./pkg`), an editable install (`-e
 * .`) or a direct reference (`pkg @ https://...`, `git+https://...`) is still built with its build
 * backend, which runs arbitrary code. Options such as `-r`, `-c` or `--index-url` are rejected as
 * well, because they include other files or change where the packages are downloaded from.
 */
internal object PythonRequirements {
    private const val OPERATOR = """(?:===|==|~=|!=|<=|>=|<|>)"""

    /** A name, optionally with extras, and optionally followed by version specifiers. */
    private val PACKAGE =
        Regex(
            """^[A-Za-z0-9][A-Za-z0-9._-]*\s*(?:\[[^\]]*])?\s*(?:$OPERATOR\s*[A-Za-z0-9.*+!_-]+\s*,?\s*)*$"""
        )

    private val CONTINUATION = Regex("""\\\r?\n""")
    private val COMMENT = Regex("""(^|\s)#.*$""")
    private val HASH = Regex("""--hash=\S+""")

    /**
     * Returns the first requirement of [text] that is not a plain package from an index, or null if
     * all of them are.
     */
    fun findUnsupported(text: String): String? {
        for (line in text.replace(CONTINUATION, " ").lines()) {
            val requirement = line.replace(COMMENT, "").replace(HASH, "").trim()
            if (requirement.isEmpty()) {
                continue
            }

            // Environment markers (`; python_version < "3.9"`) do not select what is installed
            val specification = requirement.substringBefore(';').trim()
            if (
                specification.startsWith("-") ||
                    // a path relative to the home folder, `~=` is a version specifier
                    specification.startsWith("~") ||
                    specification.any { it in "@/\\:" } ||
                    !PACKAGE.matches(specification)
            ) {
                return requirement
            }
        }

        return null
    }
}
