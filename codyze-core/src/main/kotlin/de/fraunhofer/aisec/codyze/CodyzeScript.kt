/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.codyze

import de.fraunhofer.aisec.codyze.dsl.Import
import de.fraunhofer.aisec.codyze.dsl.ProjectBuilder
import java.io.File
import java.net.JarURLConnection
import java.net.URL
import java.nio.file.Path
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.CompoundDependenciesResolver
import kotlin.script.experimental.dependencies.ExternalDependenciesResolver
import kotlin.script.experimental.dependencies.FileSystemDependenciesResolver
import kotlin.script.experimental.host.FileBasedScriptSource
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvm.util.classpathFromClassloader
import kotlin.script.experimental.util.filterByAnnotationType
import kotlin.script.templates.ScriptTemplateDefinition

/**
 * Abstract base class for Codyze scripts. Codyze scripts are Kotlin scripts that can be used to
 * - Define a project structure that describes the "target of evaluation" (ToE) for the analysis as
 *   well as requirements the ToE must fulfill
 * - Define queries that evaluate whether the requirements are met
 *
 * This class is the scription definition (template) needed for the Kotlin compiler to recognize
 * this as a script.
 */
@ScriptTemplateDefinition(scriptFilePattern = ".*\\.codyze\\.kts")
@KotlinScript(
    // File extension for the script type
    fileExtension = "codyze.kts",
    // Compilation configuration for the script type
    compilationConfiguration = CodyzeScriptCompilationConfiguration::class,
)
abstract class CodyzeScript(projectDir: Path) {

    internal var projectBuilder: ProjectBuilder = ProjectBuilder(projectDir = projectDir)
}

val baseLibraries =
    arrayOf(
        "codyze-core",
        "cpg-core",
        "cpg-concepts",
        "cpg-analysis",
        "kotlin-stdlib",
        "kotlin-reflect",
    )

/**
 * Contains the configuration for the compilation of Codyze scripts. This includes the imports that
 * are required and some specifications of the compiler options.
 */
class CodyzeScriptCompilationConfiguration :
    ScriptCompilationConfiguration({
        defaultImports.append(
            "de.fraunhofer.aisec.codyze.*",
            "de.fraunhofer.aisec.codyze.dsl.*",
            "de.fraunhofer.aisec.codyze.dsl.Import",
            "de.fraunhofer.aisec.cpg.*",
            "de.fraunhofer.aisec.cpg.graph.*",
            "de.fraunhofer.aisec.cpg.query.*",
        )
        jvm {
            val cp = classpathFromClassloader(CodyzeScript::class.java.classLoader)
            checkNotNull(cp) { "Could not read classpath" }
            updateClasspath(cp)
        }
        refineConfiguration {
            onAnnotations(Import::class, handler = CodyzeScriptConfigurator())
            beforeCompiling {
                val includes = it.script.text.lines().filter { it.startsWith("include(\"") }
                it.compilationConfiguration.with {}.asSuccess()
            }
        }
        compilerOptions("-Xcontext-receivers", "-jvm-target=21")
        ide { acceptedLocations(ScriptAcceptedLocation.Everywhere) }
    })

class CodyzeScriptConfigurator(
    private val resolver: ExternalDependenciesResolver =
        CompoundDependenciesResolver(FileSystemDependenciesResolver())
) : RefineScriptCompilationConfigurationHandler {
    override fun invoke(
        context: ScriptConfigurationRefinementContext
    ): ResultWithDiagnostics<ScriptCompilationConfiguration> = processAnnotations(context)

    private fun processAnnotations(
        context: ScriptConfigurationRefinementContext
    ): ResultWithDiagnostics<ScriptCompilationConfiguration> {
        val diagnostics = arrayListOf<ScriptDiagnostic>()

        val annotations =
            context.collectedData?.get(ScriptCollectedData.collectedAnnotations)?.takeIf {
                it.isNotEmpty()
            } ?: return context.compilationConfiguration.asSuccess()
        val scriptBaseDir = (context.script as? FileBasedScriptSource)?.file?.parentFile
        val importedSources = linkedMapOf<String, Pair<File, String>>()
        var hasImportErrors = false

        annotations.filterByAnnotationType<Import>().forEach { scriptAnnotation ->
            scriptAnnotation.annotation.paths.forEach { sourceName ->
                val file = (scriptBaseDir?.resolve(sourceName) ?: File(sourceName)).normalize()
                val keyPath = file.absolutePath
                val prevImport = importedSources.put(keyPath, file to sourceName)
                if (prevImport != null) {
                    diagnostics.add(
                        ScriptDiagnostic(
                            ScriptDiagnostic.unspecifiedError,
                            "Duplicate imports: \"${prevImport.second}\" and \"$sourceName\"",
                            sourcePath = context.script.locationId,
                            location = scriptAnnotation.location?.locationInText,
                        )
                    )
                    hasImportErrors = true
                }
            }
        }
        if (hasImportErrors) return ResultWithDiagnostics.Failure(diagnostics)

        /*val resolveResult = try {
            @Suppress("DEPRECATION_ERROR")
            internalScriptingRunSuspend {
                resolver.resolveFromScriptSourceAnnotations(annotations.filter { it.annotation is DependsOn || it.annotation is Repository })
            }
        } catch (e: Throwable) {
            diagnostics.add(e.asDiagnostics(path = context.script.locationId))
            ResultWithDiagnostics.Failure(diagnostics)
        }*/

        return ScriptCompilationConfiguration(context.compilationConfiguration) {
                if (importedSources.isNotEmpty())
                    importScripts.append(importedSources.values.map { FileScriptSource(it.first) })
            }
            .asSuccess()
    }
}

internal fun URL.toContainingJarOrNull(): File? =
    if (protocol == "jar") {
        (openConnection() as? JarURLConnection)?.jarFileURL?.toFileOrNull()
    } else null

internal fun URL.toFileOrNull() =
    try {
        File(toURI())
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: java.net.URISyntaxException) {
        null
    } ?: run { if (protocol != "file") null else File(file) }
