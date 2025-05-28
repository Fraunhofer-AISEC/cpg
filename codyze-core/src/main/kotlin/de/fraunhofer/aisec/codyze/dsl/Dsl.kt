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
@file:Suppress("CONTEXT_RECEIVERS_DEPRECATED")

package de.fraunhofer.aisec.codyze.dsl

import de.fraunhofer.aisec.codyze.AnalysisProject
import de.fraunhofer.aisec.codyze.AnalysisResult
import de.fraunhofer.aisec.codyze.CodyzeScript
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.assumptions.Assumption
import de.fraunhofer.aisec.cpg.assumptions.AssumptionStatus
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.allChildrenWithOverlays
import de.fraunhofer.aisec.cpg.passes.concepts.TagOverlaysPass
import de.fraunhofer.aisec.cpg.passes.concepts.TaggingContext
import de.fraunhofer.aisec.cpg.query.*
import de.fraunhofer.aisec.cpg.query.Decision
import de.fraunhofer.aisec.cpg.query.DecisionState
import de.fraunhofer.aisec.cpg.query.NotYetEvaluated
import de.fraunhofer.aisec.cpg.query.QueryTree
import de.fraunhofer.aisec.cpg.query.decide
import de.fraunhofer.aisec.cpg.query.toQueryTree
import io.github.detekt.sarif4k.ReportingDescriptor
import io.github.detekt.sarif4k.Result
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.uuid.Uuid

@DslMarker annotation class CodyzeDsl

interface IncludeCategory

object AssumptionDecisions : IncludeCategory

object ManualAssessment : IncludeCategory

object Tagging : IncludeCategory

/** Represents a builder to include other scripts. */
class IncludeBuilder {
    val includes: MutableMap<IncludeCategory, String> = mutableMapOf()
}

/** Represents a builder for a list of all requirements of the TOE. */
class RequirementsBuilder {
    var description: String? = null
    internal val requirements = mutableMapOf<String, RequirementBuilder>()
}

/** Represents a builder for a single requirement of the evaluation project. */
class RequirementBuilder(
    /** The unique identifier of the requirement. This is used to reference the requirement. */
    var id: String
) {
    /** A optional human-readable name of the requirement. This is used for display purposes. */
    var name: String? = id

    /**
     * An optional human-readable description of the requirement. This is used for display purposes.
     */
    var description: String? = null

    /**
     * A function that returns a [Decision] that evaluates whether the requirement is fulfilled.
     * This function is expected to be used in the context of a [TranslationResult].
     */
    var fulfilledBy: (TranslationResult) -> Decision = { NotYetEvaluated.toQueryTree() }
}

/** Represents a builder for a list of all assumptions of the evaluation project. */
class AssumptionsBuilder {
    internal val decisionBuilder = DecisionBuilder()

    class DecisionBuilder
}

/** Represents a builder for manual assessments of requirements. */
class ManualAssessmentBuilder {
    internal val assessments = mutableMapOf<String, () -> Decision>()
}

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
    internal val manualAssessmentBuilder = ManualAssessmentBuilder()
    internal var taggingCtx = TaggingContext()

    /** Builds an [AnalysisProject] out of the current state of the builder. */
    fun build(
        postProcess:
            (AnalysisProject.(AnalysisResult) -> Pair<List<ReportingDescriptor>, List<Result>>)?,
        configModifier: ((TranslationConfiguration.Builder) -> TranslationConfiguration.Builder)? =
            null,
    ): AnalysisProject {
        val name = name

        val configBuilder =
            TranslationConfiguration.builder()
                .defaultPasses()
                .registerPass<TagOverlaysPass>()
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

        // Configure tagging from tagging builder
        configBuilder.configurePass<TagOverlaysPass>(
            TagOverlaysPass.Configuration(tag = taggingCtx)
        )

        return AnalysisProject(
            builder = this,
            name,
            projectDir = projectDir,
            requirementFunctions =
                requirementsBuilder.requirements
                    .map { Pair(it.key, it.value.fulfilledBy) }
                    .associate { it },
            config = configBuilder.build(),
            postProcess = postProcess,
        )
    }
}

/** Includes other script files. */
@CodyzeDsl
fun CodyzeScript.include(block: IncludeBuilder.() -> Unit) {
    includeBuilder.apply(block)
}

context(IncludeBuilder)
@CodyzeDsl
infix fun IncludeCategory.from(path: String) {
    (this@IncludeBuilder).includes[this] = path
}

/** Spans the project-Block */
@CodyzeDsl
fun CodyzeScript.project(block: ProjectBuilder.() -> Unit) {
    projectBuilder.apply(block)
}

/** Spans the block for the tagging logic. */
@CodyzeDsl
fun ProjectBuilder.tagging(block: () -> TaggingContext) {
    taggingCtx = block()
}

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

sealed class FulfilledByReturnType

object FulfilledByDecision : FulfilledByReturnType()

object FulfilledByQueryTree : FulfilledByReturnType()

/**
 * Generates a default identifier for the next requirement in the format "RQ-001", "RQ-002", etc.
 */
fun RequirementsBuilder.nextRQ(): String {
    // Generate a default identifier based on the current size of the requirements map
    return "RQ-${String.format("%03d", requirements.size + 1)}"
}

/**
 * Describes a single requirement of the TOE by a function that returns a [Decision].
 *
 * An [id] can be provided to identify the requirement. If no [id] is provided, a default identifier
 * is generated based on the current size of the requirements map in the format specified by
 * [nextRQ].
 */
@CodyzeDsl
fun RequirementsBuilder.requirement(id: String = nextRQ(), block: RequirementBuilder.() -> Unit) {
    val builder = RequirementBuilder(id)
    block(builder)

    requirements[id] = builder
}

@CodyzeDsl
@OverloadResolutionByLambdaReturnType
fun RequirementBuilder.fulfilledBy(query: (TranslationResult) -> Decision): FulfilledByDecision {
    fulfilledBy = query
    return FulfilledByDecision
}

/**
 * Describes a single requirement of the TOE by a function that returns a [QueryTree] of [Boolean].
 */
@CodyzeDsl
fun RequirementBuilder.fulfilledBy(
    query: (TranslationResult) -> QueryTree<Boolean>
): FulfilledByQueryTree {
    fulfilledBy = { query(it).decide() }
    return FulfilledByQueryTree
}

/** Describes that the requirement had to be checked manually. */
context(ProjectBuilder)
@CodyzeDsl
fun RequirementsBuilder.manualAssessmentOf(id: String): Decision {
    val manualAssessment = this@ProjectBuilder.manualAssessmentBuilder.assessments[id]
    if (manualAssessment == null) {
        return NotYetEvaluated.toQueryTree()
    }

    return manualAssessment()
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

/** Allows specifying in a block whether assumptions are accepted, rejected or undecided. */
@CodyzeDsl
fun AssumptionsBuilder.decisions(block: AssumptionsBuilder.DecisionBuilder.() -> Unit) {
    decisionBuilder.apply(block)
}

/**
 * Describes that the assumption with the given [uuid] was assessed and considered as
 * acceptable/valid.
 *
 * @param uuid The UUID of the assumption must be provided in string in the format
 *   "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx", where each 'x' is a hexadecimal digit, either lowercase
 *   or uppercase.
 */
@CodyzeDsl
fun AssumptionsBuilder.DecisionBuilder.accept(uuid: String) {
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
fun AssumptionsBuilder.DecisionBuilder.reject(uuid: String) {
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
fun AssumptionsBuilder.DecisionBuilder.undecided(uuid: String) {
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
fun AssumptionsBuilder.DecisionBuilder.ignore(uuid: String) {
    parseUuidAndAnnotateAssumptions(uuid, AssumptionStatus.Ignored)
}

/** Describes the manual assessments. */
@CodyzeDsl
fun ProjectBuilder.manualAssessment(block: ManualAssessmentBuilder.() -> Unit) {
    manualAssessmentBuilder.apply(block)
}

sealed class OfReturnType

object OfDecision : OfReturnType()

object OfDecisionState : OfReturnType()

object OfQueryTree : OfReturnType()

object OfBoolean : OfReturnType()

/**
 * Describes a manual assessment of a requirement with the given [id]. The [block] is expected to
 * return a [Decision] that evaluates to [Succeeded] if the requirement is fulfilled.
 */
@CodyzeDsl
@OverloadResolutionByLambdaReturnType
fun ManualAssessmentBuilder.of(id: String, block: () -> Decision): OfDecision {
    assessments[id] = block
    return OfDecision
}

/**
 * Describes a manual assessment of a requirement with the given [id]. The [block] is expected to
 * return a [DecisionState] that evaluates to [Succeeded] if the requirement is fulfilled.
 */
fun ManualAssessmentBuilder.of(id: String, block: () -> DecisionState): OfDecisionState {
    assessments[id] = { block().toQueryTree() }
    return OfDecisionState
}

/**
 * Describes a manual assessment of a requirement with the given [id]. The [block] is expected to
 * return a [QueryTree] that evaluates to `true` if the requirement is fulfilled.
 */
@CodyzeDsl
fun ManualAssessmentBuilder.of(id: String, block: () -> QueryTree<Boolean>): OfQueryTree {
    assessments[id] = { block().decide() }
    return OfQueryTree
}

/**
 * Describes a manual assessment of a requirement with the given [id]. The [block] is expected to
 * return a [Boolean] that evaluates to `true` if the requirement is fulfilled.
 */
@CodyzeDsl
fun ManualAssessmentBuilder.of(id: String, block: () -> Boolean): OfBoolean {
    assessments[id] = { block().toQueryTree().decide() }
    return OfBoolean
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
