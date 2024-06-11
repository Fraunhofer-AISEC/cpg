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
package de.fraunhofer.aisec.cpg.query

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.rules.BufferOverflowMemCpySizeLargerThanDest
import de.fraunhofer.aisec.cpg.rules.BufferOverreadMemCpySrcSmallerThanSize
import java.io.File

/**
 * A class that runs a set of rules on a given codebase and reports the results
 *
 * @param location the path to the codebase as used by [TranslationConfiguration.sourceLocations]
 * @param language the language of the codebase, \in {"C", "CPP", "Go", "Java"}
 * @param rules List of [Rule]s to run
 * @param reporter the [Reporter] to use for reporting
 */
class RuleRunner(
    language: String,
    private val rules: List<Rule>,
    private val reporter: Reporter,
    vararg locations: File
) {

    private val config: TranslationConfiguration =
        TranslationConfiguration.builder()
            .sourceLocations(locations.toList())
            // .optionalLanguage(
            //     when (language) {
            //         "C" -> "de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage"
            //         "CPP" -> "de.fraunhofer.aisec.cpg.frontends.cxx.CPPLanguage"
            //         "Go" -> "de.fraunhofer.aisec.cpg.frontends.golang.GoLanguage"
            //         "Java" -> "de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage"
            //         else -> {
            //             println("Unsupported/No language, defaulting to C to avoid exception")
            //             "de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage" // don't throw
            // exception
            //         }
            //     }
            // )
            .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.cxx.CPPLanguage")
            .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage")
            .useParallelPasses(true)
            .useParallelFrontends(true)
            .defaultPasses()
            .build()

    private val result: TranslationResult =
        TranslationManager.builder().config(config).build().analyze().get()

    fun runRules() {
        for (rule in rules) {
            rule.run(result)
        }
    }

    fun report() {
        reporter.toFile(reporter.report(rules), reporter.getDefaultPath())
    }
}

fun main() {
    val path =
        // "/home/layla/code/cpg/cpg-analysis/src/main/kotlin/de/fraunhofer/aisec/cpg/query/programs/"
        "/home/layla/code/cpg/cpg-analysis/src/main/kotlin/de/fraunhofer/aisec/cpg/query/programs/CWE121_Stack_Based_Buffer_Overflow__CWE805_struct_alloca_memcpy_31.c"
    val runner =
        RuleRunner(
            language = "C",
            rules =
                listOf(
                    BufferOverflowMemCpySizeLargerThanDest(),
                    BufferOverflowMemCpySizeLargerThanDest(),
                    BufferOverreadMemCpySrcSmallerThanSize()
                ),
            reporter = SarifReporter(),
            /* locations = */ File(path)
        )
    runner.runRules()
    runner.report()
}
