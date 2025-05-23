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
package de.fraunhofer.aisec.codyze.dsl

import de.fraunhofer.aisec.codyze.AnalysisProject
import de.fraunhofer.aisec.codyze.CodyzeScript
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.assumptions.Assumption
import de.fraunhofer.aisec.cpg.assumptions.AssumptionStatus
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.allChildrenWithOverlays
import de.fraunhofer.aisec.cpg.query.QueryTree
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.uuid.Uuid

@DslMarker annotation class CodyzeDsl

/** Represents a builder for a single requirement of the TOE. */
class RequirementBuilder(var name: String = "") {
    var query: ((result: TranslationResult) -> QueryTree<Boolean>)? = null
}

/** Represents a builder for a list of all requirements of the TOE. */
class RequirementsBuilder {
    val requirements = mutableMapOf<String, (TranslationResult) -> QueryTree<Boolean>>()
}

/** Represents a builder for a list of all assumptions of the evaluation project. */
class AssumptionsBuilder

/** Represents a builder for tool metadata and configuration. */
class ToolBuilder {
    internal var translationConfigurationBuilder: ((TranslationConfiguration.Builder) -> (Unit))? =
        null
}

/** Represents a builder for the TOE with its name, version and a description. */
class ToEBuilder {
    /** The (unique) name of the TOE. */
    var name: String? = null

    /** The description of the TOE. */
    var description: String? = null

    /** The version number of the TOE. */
    var version: String? = null

    val architectureBuilder = ArchitectureBuilder()
}

/** Represents a builder for the architecture (in terms of modules) of the TOE. */
class ArchitectureBuilder {
    val modulesBuilder = ModulesBuilder()
}

/** Represents a builder for a list of modules of the TOE. */
class ModulesBuilder {
    val modules = mutableListOf<ModuleBuilder>()
}

/**
 * Represents a builder for a single module of the TOE. This more or less the same what is
 * translated into a CPG [Component].
 */
class ModuleBuilder(
    /** The name of the module/[Component]. */
    var name: String = ""
) {
    /** The directory containing the code. */
    var directory: String = ""

    /** The files (patterns) which should be included during the translation. */
    internal var include: List<String> = emptyList()

    /** The files (patterns) which should explicitly not be considered during the translation. */
    internal var exclude: List<String> = emptyList()

    /** Adds a file/pattern to include in the translation. */
    fun include(vararg includes: String) {
        include += includes
    }

    /** Includes all files in the [directory] in the translation. */
    fun includeAll() {
        include = emptyList()
    }

    /** Adds a file/pattern to exclude from the translation. */
    fun exclude(vararg excludes: String) {
        exclude += excludes
    }
}

/** Represents a builder for the container for the whole analysis project. */
class ProjectBuilder(val projectDir: Path = Path(".")) {
    var name: String? = null

    internal val toolBuilder = ToolBuilder()
    internal var toeBuilder = ToEBuilder()
    internal val requirementsBuilder = RequirementsBuilder()
    internal val assumptionsBuilder = AssumptionsBuilder()

    /** Builds an [AnalysisProject] out of the current state of the builder. */
    fun build(
        configModifier: ((TranslationConfiguration.Builder) -> TranslationConfiguration.Builder)? =
            null
    ): AnalysisProject {
        val name = name

        val configBuilder =
            TranslationConfiguration.builder()
                .defaultPasses()
                .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage")
                .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.cxx.CPPLanguage")
                .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage")
                .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.golang.GoLanguage")
                .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.llvm.LLVMIRLanguage")
                .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage")
                .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.typescript.TypeScriptLanguage")
                .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.ruby.RubyLanguage")
                .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.jvm.JVMLanguage")
                .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.ini.IniFileLanguage")

        if (name == null) {
            throw IllegalArgumentException("Project name must be set")
        }

        val components = mutableMapOf<String, List<File>>()
        val topLevels = mutableMapOf<String, File>()

        // Build software components and "top levels" from the specified architecture
        toeBuilder.architectureBuilder.modulesBuilder.modules.forEach { it ->
            // Exclude all files in the exclude list
            it.exclude.forEach { exclude -> configBuilder.exclusionPatterns(exclude) }

            // Build the file list from the include list
            val componentTopLevel = projectDir.resolve(it.directory).toFile()
            var files = it.include.map { include -> componentTopLevel.resolve(include) }

            // If the include list is empty, we include the directory itself
            if (files.isEmpty()) {
                files = listOf(componentTopLevel)
            }

            components += it.name to files
            topLevels += it.name to componentTopLevel
        }

        configBuilder.softwareComponents(components)
        configBuilder.topLevels(topLevels)

        // Adjust config from the "external" config modifier as well as from any configuration
        // builder inside the script
        configModifier?.invoke(configBuilder)
        toolBuilder.translationConfigurationBuilder?.invoke(configBuilder)

        val requirementsFunctions = requirementsBuilder.requirements

        return AnalysisProject(
            name,
            projectDir = projectDir,
            requirementFunctions = requirementsFunctions,
            config = configBuilder.build(),
        )
    }
}

/** Spans the project-Block */
@CodyzeDsl
fun CodyzeScript.project(block: ProjectBuilder.() -> Unit) {
    block(project)
}

/** Spans the block for the tagging logic. */
@CodyzeDsl fun CodyzeScript.tagging(block: ProjectBuilder.() -> Unit) {}

/** Describes some configuration and metadata about the evaluation tool */
fun ProjectBuilder.tool(block: ToolBuilder.() -> Unit) {
    toolBuilder.apply(block)
}

/** Can be used to modify the tool configuration, specifically the [TranslationConfiguration]. */
fun ToolBuilder.configuration(block: (TranslationConfiguration.Builder).() -> Unit) {
    translationConfigurationBuilder = block
}

/** Describes a Target of Evaluation (ToE). */
@CodyzeDsl
fun ProjectBuilder.toe(block: ToEBuilder.() -> Unit) {
    toeBuilder.apply(block)
}

/** Describes the architecture of the ToE. */
@CodyzeDsl
fun ToEBuilder.architecture(block: ArchitectureBuilder.() -> Unit) {
    architectureBuilder.apply(block)
}

@CodyzeDsl
fun ProjectBuilder.requirements(block: RequirementsBuilder.() -> Unit) {
    requirementsBuilder.apply(block)
}

/** Describes the different modules, such as (sub)-components, of the ToE. */
@CodyzeDsl
fun ArchitectureBuilder.modules(block: ModulesBuilder.() -> Unit) {
    block(modulesBuilder)
}

/** Describes one module of the ToE. This is translated into a CPG [Component]. */
@CodyzeDsl
fun ModulesBuilder.module(name: String, block: ModuleBuilder.() -> Unit) {
    val builder = ModuleBuilder(name)
    block(builder)
    modules += builder
}

/** Describes a single requirement of the TOE. */
@CodyzeDsl
fun RequirementsBuilder.requirement(name: String, block: RequirementBuilder.() -> Unit) {
    val builder = RequirementBuilder(name)
    block(builder)
    requirements[name] = builder.query ?: throw IllegalStateException("Requirement not set")
}

/**
 * Determines that the requirement is fulfilled if the query returns a [QueryTree] with
 * [QueryTree.value] set to `true`.
 */
@CodyzeDsl
fun RequirementBuilder.byQuery(query: (result: TranslationResult) -> QueryTree<Boolean>) {
    this.query = query
}

/** Describes that the requirement had to be checked manually. */
@CodyzeDsl
fun RequirementBuilder.byManualAssessment(id: String): Unit {
    this.query = { result -> QueryTree(true) }
}

/** Describes the assumptions which have been handled and assessed. */
@CodyzeDsl
fun ProjectBuilder.assumptions(block: AssumptionsBuilder.() -> Unit) {
    assumptionsBuilder.apply(block)
}

/**
 * Allows to explicitly list a custom assumption which has to hold and is always accepted for the
 * current evaluation project.
 */
@CodyzeDsl fun AssumptionsBuilder.assume(message: () -> String) {}

/**
 * Describes that the assumption with the given [uuid] was assessed and considered as
 * acceptable/valid.
 *
 * @param uuid The UUID of the assumption must be provided in string in the format
 *   "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx", where each 'x' is a hexadecimal digit, either lowercase
 *   or uppercase.
 */
@CodyzeDsl
fun AssumptionsBuilder.accept(uuid: String) {
    parseUuidAndAnnotateAssumptions(uuid, AssumptionStatus.Accepted)
}

/**
 * Describes that the assumption with the given [uuid] was assessed and considered as
 * rejected/invalid.
 *
 * @param uuid The UUID of the assumption must be provided in string in the format
 *   "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx", where each 'x' is a hexadecimal digit, either lowercase
 *   or uppercase.
 */
@CodyzeDsl
fun AssumptionsBuilder.reject(uuid: String) {
    parseUuidAndAnnotateAssumptions(uuid, AssumptionStatus.Rejected)
}

/**
 * Describes that the assumption with the given [uuid] requires assessment.
 *
 * @param uuid The UUID of the assumption must be provided in string in the format
 *   "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx", where each 'x' is a hexadecimal digit, either lowercase
 *   or uppercase.
 */
@CodyzeDsl
fun AssumptionsBuilder.undecided(uuid: String) {
    parseUuidAndAnnotateAssumptions(uuid, AssumptionStatus.Undecided)
}

/**
 * Describes that the assumption with the given [uuid] was assessed and can be ignored in this
 * evaluation project.
 *
 * @param uuid The UUID of the assumption must be provided in string in the format
 *   "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx", where each 'x' is a hexadecimal digit, either lowercase
 *   or uppercase.
 */
@CodyzeDsl
fun AssumptionsBuilder.ignore(uuid: String) {
    parseUuidAndAnnotateAssumptions(uuid, AssumptionStatus.Ignored)
}

private fun parseUuidAndAnnotateAssumptions(uuid: String, status: AssumptionStatus) {
    val parsedUuid = Uuid.parse(uuid)
    // TODO: Acutally get the TranslationResult
    val result: TranslationResult? = null
    // TODO: Do we find all assumptions like this (i.e., also those related to overlays of a node
    // and edges)?
    result
        .allChildrenWithOverlays<Assumption> { it.id == parsedUuid }
        .forEach { assumption -> assumption.status = status }
}
