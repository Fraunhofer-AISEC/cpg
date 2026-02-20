/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.ini

import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.types.StringType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.persistence.DoNotPersist
import kotlin.reflect.KClass

/**
 * A simple language representing classical [INI files](https://en.wikipedia.org/wiki/INI_file). As
 * there are conflicting definitions of an INI file, we go with:
 * - the file extension is `.ini` or `.conf`
 * - all entries live in a unique `section`
 * - all `key`s are unique per section
 * - the file is accepted by the [ini4j library](https://ini4j.sourceforge.net/)
 */
class IniFileLanguage : Language<IniFileFrontend>() {
    override val fileExtensions = listOf("ini", "conf")
    override val namespaceDelimiter: String = "." // no such thing

    @DoNotPersist override val frontend: KClass<out IniFileFrontend> = IniFileFrontend::class
    override val builtInTypes: Map<String, Type> =
        mapOf("string" to StringType("string", language = this)) // everything is a string

    override val compoundAssignmentOperators: Set<String> = emptySet() // no such thing
}
