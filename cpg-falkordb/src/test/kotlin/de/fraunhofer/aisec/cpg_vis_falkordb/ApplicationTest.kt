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
package de.fraunhofer.aisec.cpg_vis_falkordb

import de.fraunhofer.aisec.cpg.ConfigurationException
import de.fraunhofer.aisec.cpg.passes.ControlDependenceGraphPass
import de.fraunhofer.aisec.cpg.passes.ControlFlowSensitiveDFGPass
import de.fraunhofer.aisec.cpg.passes.DFGPass
import de.fraunhofer.aisec.cpg.passes.PrepareSerialization
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import picocli.CommandLine

/**
 * Tests the command line handling of [Application]. They deliberately stop at the translation
 * configuration, since actually analyzing code requires a language frontend, which is only
 * optionally enabled.
 */
class ApplicationTest {

    /** Parses [args] into a fresh [Application]. */
    private fun parse(vararg args: String): Application {
        val application = Application()
        CommandLine(application).parseArgs(*args)
        return application
    }

    private fun resource(name: String) =
        File(javaClass.classLoader.getResource(name)?.file ?: error("Missing resource $name"))

    @Test
    fun testListPasses() {
        // --list-passes must not try to analyze anything
        assertEquals(0, CommandLine(Application()).execute("--list-passes"))
    }

    @Test
    fun testSourceLocations() {
        val config = parse(resource("client.c").absolutePath).setupTranslationConfiguration()

        assertEquals(
            listOf(resource("client.c").absolutePath),
            config.sourceLocations.map { it.absolutePath },
        )
        // Serialization has to be prepared, otherwise persisting the result does not work
        assertContains(config.registeredPasses.flatten(), PrepareSerialization::class)
    }

    @Test
    fun testSoftwareComponents() {
        val file = resource("client.c").absolutePath
        val config = parse("-S", "App1=$file").setupTranslationConfiguration()

        assertEquals(setOf("App1"), config.softwareComponents.keys)
        assertEquals(listOf(file), config.softwareComponents["App1"]?.map { it.absolutePath })
    }

    @Test
    fun testNonExistingFileIsRejected() {
        val application = parse("this-file-does-not-exist.c")

        assertFailsWith<IllegalArgumentException> { application.setupTranslationConfiguration() }
    }

    @Test
    fun testDefaultPasses() {
        val config = parse(resource("client.c").absolutePath).setupTranslationConfiguration()
        val passes = config.registeredPasses.flatten()

        assertContains(passes, ControlDependenceGraphPass::class)
        assertContains(passes, DFGPass::class)
    }

    @Test
    fun testNoDefaultPasses() {
        val config =
            parse("--no-default-passes", resource("client.c").absolutePath)
                .setupTranslationConfiguration()
        val passes = config.registeredPasses.flatten()

        assertFalse(ControlDependenceGraphPass::class in passes)
        // The serialization pass is always needed, even without the default passes
        assertContains(passes, PrepareSerialization::class)
    }

    @Test
    fun testCustomPassByName() {
        val config =
            parse(
                    "--no-default-passes",
                    "--custom-pass-list=DFGPass",
                    resource("client.c").absolutePath,
                )
                .setupTranslationConfiguration()

        assertContains(config.registeredPasses.flatten(), DFGPass::class)
    }

    @Test
    fun testCustomPassByFullyQualifiedName() {
        val config =
            parse(
                    "--no-default-passes",
                    "--custom-pass-list=de.fraunhofer.aisec.cpg.passes.DFGPass",
                    resource("client.c").absolutePath,
                )
                .setupTranslationConfiguration()

        assertContains(config.registeredPasses.flatten(), DFGPass::class)
    }

    @Test
    fun testUnknownCustomPassIsRejected() {
        val application =
            parse("--custom-pass-list=ThisPassDoesNotExist", resource("client.c").absolutePath)

        assertFailsWith<ConfigurationException> { application.setupTranslationConfiguration() }
    }

    @Test
    fun testMaxComplexity() {
        val config =
            parse("--max-complexity-cf-dfg=42", resource("client.c").absolutePath)
                .setupTranslationConfiguration()

        assertEquals(
            42,
            config.passConfigurations[ControlFlowSensitiveDFGPass::class]
                .let { it as? ControlFlowSensitiveDFGPass.Configuration }
                ?.maxComplexity,
        )
    }

    @Test
    fun testTopLevelAndIncludePaths() {
        val topLevel = resource("client.c").parentFile
        val config =
            parse(
                    "--top-level=${topLevel.absolutePath}",
                    "-IP",
                    "/some/include/path",
                    "--load-includes",
                    "--exclusion-patterns=ignore-me",
                    resource("client.c").absolutePath,
                )
                .setupTranslationConfiguration()

        assertTrue(config.loadIncludes)
        assertEquals(listOf(topLevel.absolutePath), config.topLevels.values.map { it.absolutePath })
        assertTrue(config.includePaths.any { it.toString() == "/some/include/path" })
        assertContains(config.exclusionPatternsByString, "ignore-me")
    }

    @Test
    fun testIncludesFile() {
        val includesFile = resource("includes.txt")
        val config =
            parse("--includes-file=${includesFile.absolutePath}", resource("client.c").absolutePath)
                .setupTranslationConfiguration()

        // Relative entries of the includes file are resolved against the directory of that file
        val baseDir = includesFile.parentFile.absolutePath
        assertTrue(config.includePaths.any { it.toString() == "$baseDir/include-a" })
        assertTrue(config.includePaths.any { it.toString() == "$baseDir/include-b" })
    }

    @Test
    fun testInferNodes() {
        val config =
            parse("--infer-nodes", resource("client.c").absolutePath)
                .setupTranslationConfiguration()

        assertTrue(config.inferenceConfiguration.inferRecords)
    }

    @Test
    fun testUseUnityBuild() {
        val config =
            parse("--use-unity-build", "--load-includes", resource("client.c").absolutePath)
                .setupTranslationConfiguration()

        assertTrue(config.useUnityBuild)
    }

    @Test
    fun testJsonCompilationDatabase() {
        val source = resource("client.c")
        // Write the database next to the source, so that the relative entry resolves correctly
        val db = File.createTempFile("compile_commands", ".json")
        db.deleteOnExit()
        db.writeText(
            """
            [{"directory": "${source.parentFile.absolutePath}",
              "arguments": ["cc", "-c", "client.c"],
              "file": "client.c"}]
            """
                .trimIndent()
        )

        val config =
            parse("--json-compilation-database=${db.absolutePath}").setupTranslationConfiguration()

        assertEquals(listOf(source.absolutePath), config.sourceLocations.map { it.absolutePath })
    }

    @Test
    fun testFalkorDbOptionDefaults() {
        val application = parse(resource("client.c").absolutePath)

        // Without credentials we connect anonymously, which is how FalkorDB ships by default
        assertEquals(null, application.falkorDbUsername)
        assertEquals(null, application.falkorDbPassword)
    }
}
