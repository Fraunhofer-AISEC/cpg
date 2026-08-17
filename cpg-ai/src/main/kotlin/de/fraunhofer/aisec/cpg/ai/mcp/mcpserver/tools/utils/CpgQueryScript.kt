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
package de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils

import de.fraunhofer.aisec.cpg.TranslationResult
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvm.util.classpathFromClassloader

/**
 * Script template for CPG query scripts submitted by the LLM. Only used to compile scripts (see
 * `cpg_validate_query`).
 */
@KotlinScript(
    fileExtension = "cpg.query.kts",
    compilationConfiguration = CpgQueryScriptCompilationConfiguration::class,
)
abstract class CpgQueryScript(val result: TranslationResult)

/** Compilation configuration for [CpgQueryScript] */
class CpgQueryScriptCompilationConfiguration :
    ScriptCompilationConfiguration({
        defaultImports.append(
            "de.fraunhofer.aisec.cpg.*",
            "de.fraunhofer.aisec.cpg.graph.*",
            "de.fraunhofer.aisec.cpg.graph.declarations.*",
            "de.fraunhofer.aisec.cpg.graph.expressions.*",
            "de.fraunhofer.aisec.cpg.graph.types.*",
            "de.fraunhofer.aisec.cpg.query.*",
        )
        jvm {
            val cp = classpathFromClassloader(CpgQueryScript::class.java.classLoader)
            checkNotNull(cp) { "Could not read classpath" }
            updateClasspath(cp)
        }
        compilerOptions("-jvm-target=21")
        ide { acceptedLocations(ScriptAcceptedLocation.Everywhere) }
    })
