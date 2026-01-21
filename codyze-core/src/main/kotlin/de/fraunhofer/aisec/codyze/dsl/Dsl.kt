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
import de.fraunhofer.aisec.cpg.passes.concepts.TagOverlaysPass
import de.fraunhofer.aisec.cpg.passes.concepts.TaggingContext
import de.fraunhofer.aisec.cpg.query.NotYetEvaluated
import de.fraunhofer.aisec.cpg.query.QueryTree
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

object Suppressions : IncludeCategory

/** Represents a builder to include other scripts. */
class IncludeBuilder {
    val includes: MutableMap<IncludeCategory, String> = mutableMapOf()
}

/**
 * Represents the default category for requirements. This is used when no specific category is
 * provided for a requirement. This category is created automatically if
 * [RequirementsBuilder.requirement] is called.
 */
val DefaultCategory =
    RequirementCategoryBuilder("DEFAULT").apply {
        name = "Default Requirements Category"
        description =
            "This is the default category for requirements that do not fit into any other category."
    }

/** Represents a builder for a list of all requirements of the TOE. */
class RequirementsBuilder {
    var description: String? = null

    internal val categoryBuilders = mutableMapOf<String, RequirementCategoryBuilder>()
}

/** Represents a builder for a single category of requirements of the evaluation project. */
class RequirementCategoryBuilder(
    /** The unique identifier of the category. */
    var id: String
) {
    /** An optional human-readable name of the requirement. This is used for display purposes. */
    var name: String? = id

    /** A human-readable description of the category. This is used for display purposes. */
    var description: String? = null

    /** The requirements in this category. */
    val requirements = mutableMapOf<String, RequirementBuilder>()
}

/** Represents a builder for a single requirement of the evaluation project. */
class RequirementBuilder(
    /** The unique identifier of the requirement. This is used to reference the requirement. */
    var id: String
) {
    /** An optional human-readable name of the requirement. This is used for display purposes. */
    var name: String? = id

    /**
     * An optional human-readable description of the requirement. This is used for display purposes.
     */
    var description: String? = null

    /**
     * A function that returns a [QueryTree] that evaluates whether the requirement is fulfilled.
     * This function is expected to be used in the context of a [TranslationResult].
     */
    var fulfilledBy: TranslationResult.() -> QueryTree<Boolean> = { NotYetEvaluated }
}

/** Represents a builder for suppressions of the evaluation project. */
class SuppressionsBuilder {
    val suppressions = mutableMapOf<(QueryTree<*>) -> Boolean, Any>()
}

/** Represents a builder for a list of all assumptions of the evaluation project. */
class AssumptionsBuilder {
    internal val decisionBuilder = DecisionBuilder()

    class DecisionBuilder {
        val assumptionStatusFunctions = mutableMapOf<(Assumption) -> Boolean, AssumptionStatus>()
    }
}

/** Represents a builder for manual assessments of requirements. */
class ManualAssessmentBuilder {
    internal val assessments = mutableMapOf<String, TranslationResult.() -> QueryTree<Boolean>>()
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
    internal val suppressionsBuilder = SuppressionsBuilder()
    internal val manualAssessmentBuilder = ManualAssessmentBuilder()
    internal var taggingCtx = TaggingContext()

    /**
     * Returns a list of all requirements in all categories of the project. This is useful to
     * collect all requirements in a single list, for example, to generate reports or to evaluate
     * them.
     */
    val allRequirements: Map<String, RequirementBuilder>
        get() {
            return requirementsBuilder.categoryBuilders
                .map { it.value }
                .flatMap { it.requirements.entries }
                .associate { it.key to it.value }
        }

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

        // Collect all requirements functions from all categories
        val requirementFunctions =
            requirementsBuilder.categoryBuilders
                .map { it.value }
                .flatMap { it.requirements.map { rq -> Pair(rq.key, rq.value.fulfilledBy) } }
                .associate { it }

        return AnalysisProject(
            builder = this,
            name,
            projectDir = projectDir,
            requirementFunctions = requirementFunctions,
            requirementCategories = requirementsBuilder.categoryBuilders,
            assumptionStatusFunctions =
                assumptionsBuilder.decisionBuilder.assumptionStatusFunctions,
            suppressedQueryTreeIDs = suppressionsBuilder.suppressions,
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

context(builder: IncludeBuilder)
@CodyzeDsl
infix fun IncludeCategory.from(path: String) {
    (builder).includes[this] = path
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

/**
 * Generates a default identifier for the next requirement in the format
 * `RQ-<category_id>-<number>`,
 */
fun RequirementCategoryBuilder.nextRQ(): String {
    // Generate a default identifier based on the current size of the requirements map
    return "RQ-${id}-${String.format("%03d", requirements.size + 1)}"
}

/**
 * Describes a single requirement of the TOE (in the [DefaultCategory]) by a function that returns a
 * [QueryTree] of [Boolean].
 *
 * An [id] can be provided to identify the requirement. If no [id] is provided, a default identifier
 * is generated based on the current size of the requirements map in the format specified by
 * [nextRQ].
 */
@CodyzeDsl
fun RequirementsBuilder.requirement(id: String? = null, block: RequirementBuilder.() -> Unit) {
    // Check, if the default category exists, if not, create it
    val defaultCategory = categoryBuilders.getOrPut(DefaultCategory.id) { DefaultCategory }

    return defaultCategory.requirement(id, block)
}

/**
 * Describes a single requirement category of the TOE.
 *
 * An [id] is needed to identify the category. This is used to reference the category in creating
 * requirements using [nextRQ]
 */
@CodyzeDsl
fun RequirementsBuilder.category(id: String, block: RequirementCategoryBuilder.() -> Unit) {
    categoryBuilders[id] = RequirementCategoryBuilder(id).apply(block)
}

/**
 * Describes a single requirement of the TOE (within the current requirement category) by a function
 * that returns a [QueryTree] of [Boolean].
 *
 * An [id] can be provided to identify the requirement. If no [id] is provided, a default identifier
 * is generated based on the current size of the requirements map in the format specified by
 * [nextRQ].
 */
@CodyzeDsl
fun RequirementCategoryBuilder.requirement(
    id: String? = null,
    block: RequirementBuilder.() -> Unit,
) {
    val id = id ?: nextRQ()
    val builder = RequirementBuilder(id)
    block(builder)

    requirements[id] = builder
}

/**
 * Describes a single requirement of the TOE by a function that returns a [QueryTree] of [Boolean].
 */
@CodyzeDsl
fun RequirementBuilder.fulfilledBy(query: TranslationResult.() -> QueryTree<Boolean>) {
    fulfilledBy = { query() }
}

/** Describes that the requirement had to be checked manually. */
context(builder: ProjectBuilder, _: RequirementsBuilder, result: TranslationResult)
@CodyzeDsl
fun manualAssessmentOf(id: String): QueryTree<Boolean> {
    val manualAssessment = builder.manualAssessmentBuilder.assessments[id] ?: return NotYetEvaluated

    return manualAssessment(result)
}

/** Describes the assumptions which have been handled and assessed. */
@CodyzeDsl
fun ProjectBuilder.assumptions(block: AssumptionsBuilder.() -> Unit) {
    assumptionsBuilder.apply(block)
}

/**
 * Describes possible suppressions of the query tree. This is used to suppress certain queries that
 * are known to be problematic or not relevant for the current evaluation project.
 */
@CodyzeDsl
fun ProjectBuilder.suppressions(block: SuppressionsBuilder.() -> Unit) {
    suppressionsBuilder.apply(block)
}

/**
 * Allows suppressing a query tree by its [Uuid]. The [suppression] pair is expected to contain the
 * [Uuid] as the first element and an optional value as the second element. The value is the value
 * used in the suppression.
 */
@CodyzeDsl
fun SuppressionsBuilder.queryTreeById(suppression: Pair<String, Any>) {
    suppressions += Pair({ it.id == Uuid.parse(suppression.first) }, suppression.second)
}

/**
 * Allows suppressing a query tree by a predicate function. The [suppression] pair is expected to
 * contain a predicate function as the first element and an optional value as the second element.
 */
@CodyzeDsl
fun <T> SuppressionsBuilder.queryTree(suppression: Pair<(QueryTree<T>) -> Boolean, T>) {
    suppressions +=
        Pair(
            { @Suppress("UNCHECKED_CAST") suppression.first(it as QueryTree<T>) },
            suppression.second as Any,
        )
}

/**
 * Allows explicitly listing a custom assumption which has to hold and is always accepted for the
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
    assumptionStatusFunctions[{ it.id == Uuid.parse(uuid) }] = AssumptionStatus.Accepted
}

/**
 * Describes that the assumptions matching [filter] were assessed and considered as
 * acceptable/valid.
 *
 * @param [filter] A function that takes an [Assumption] and returns `true` if the assumption is
 *   accepted. It must make sure that this only matches the intended assumptions.
 */
@CodyzeDsl
fun AssumptionsBuilder.DecisionBuilder.accept(filter: (Assumption) -> Boolean) {
    assumptionStatusFunctions[filter] = AssumptionStatus.Accepted
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
    assumptionStatusFunctions[{ it.id == Uuid.parse(uuid) }] = AssumptionStatus.Rejected
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
    assumptionStatusFunctions[{ it.id == Uuid.parse(uuid) }] = AssumptionStatus.Undecided
}

/** Describes the manual assessments. */
@CodyzeDsl
fun ProjectBuilder.manualAssessment(block: ManualAssessmentBuilder.() -> Unit) {
    manualAssessmentBuilder.apply(block)
}

sealed class OfReturnType

object OfQueryTree : OfReturnType()

object OfBoolean : OfReturnType()

/**
 * Describes a manual assessment of a requirement with the given [id]. The [block] is expected to
 * return a [QueryTree] that evaluates to `true` if the requirement is fulfilled.
 */
@CodyzeDsl
@OverloadResolutionByLambdaReturnType
fun ManualAssessmentBuilder.of(
    id: String,
    block: TranslationResult.() -> QueryTree<Boolean>,
): OfQueryTree {
    assessments[id] = { block() }
    return OfQueryTree
}

/**
 * Describes a manual assessment of a requirement with the given [id]. The [block] is expected to
 * return a [Boolean] that evaluates to `true` if the requirement is fulfilled.
 */
@CodyzeDsl
fun ManualAssessmentBuilder.of(id: String, block: TranslationResult.() -> Boolean): OfBoolean {
    assessments[id] = { block().toQueryTree(collectCallerInfo = true) }
    return OfBoolean
}
