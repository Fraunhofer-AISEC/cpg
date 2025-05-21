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

import de.fraunhofer.aisec.codyze.CodyzeScript
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.query.QueryTree

@DslMarker annotation class CodyzeDsl

/** Represents a single requirement of the TOE. */
class RequirementBuilder(var name: String = "") {
    var query: ((result: TranslationResult) -> QueryTree<Boolean>)? = null
}

/** Contains a list of all requirements of the TOE. */
class RequirementsBuilder

/** Represents the TOE with its name, version and a description. */
class ToEBuilder {
    /** The (unique) name of the TOE. */
    var name: String? = null

    /** The description of the TOE. */
    var description: String? = null

    /** The version number of the TOE. */
    var version: String? = null
}

/** Contains the architecture (in terms of modules) of the TOE. */
class ArchitectureBuilder {}

/** Contains a list of modules of the TOE. */
class ModulesBuilder {}

/**
 * Represents that all files of a module should be included. This is done by using an empty list.
 */
val ALL = listOf<String>()

/**
 * Represents a single module of the TOE. This more or less the same what is translated into a CPG
 * [Component].
 */
class ModuleBuilder(
    /** The name of the module/[Component]. */
    var name: String = ""
) {
    /** The directory containing the code. */
    var directory: String = ""

    /** The files (patterns) which should be included during the translation. */
    private var include: List<String> = emptyList()

    /** The files (patterns) which should explicitly not be considered during the translation. */
    private var exclude: List<String> = emptyList()

    /** Adds a file/pattern to include in the translation. */
    fun include(vararg includes: String) {
        include += includes
    }

    /** Adds a file/pattern to exclude from the translation. */
    fun exclude(vararg excludes: String) {
        exclude += excludes
    }
}

/** A container for the whole analysis project. */
class ProjectBuilder {}

/** Spans the project-Block */
@CodyzeDsl fun CodyzeScript.project(block: ProjectBuilder.() -> Unit) {}

/** Spans the block for the tagging logic. */
@CodyzeDsl fun CodyzeScript.tagging(block: ProjectBuilder.() -> Unit) {}

/** Describes a Target of Evaluation (ToE). */
@CodyzeDsl fun ProjectBuilder.toe(block: ToEBuilder.() -> Unit) {}

/** Describes the architecture of the ToE. */
@CodyzeDsl fun ToEBuilder.architecture(block: ArchitectureBuilder.() -> Unit) {}

@CodyzeDsl fun ToEBuilder.requirements(block: RequirementsBuilder.() -> Unit) {}

/** Describes the different modules, such as (sub)-components, of the ToE. */
@CodyzeDsl fun ArchitectureBuilder.modules(block: ModulesBuilder.() -> Unit) {}

/** Describes one module of the ToE. This is translated into a CPG [Component]. */
@CodyzeDsl
fun ModulesBuilder.module(name: String, block: ModuleBuilder.() -> Unit) {
    val builder = ModuleBuilder(name)
    block(builder)
}

/** Describes a single requirement of the TOE. */
@CodyzeDsl
fun ToEBuilder.requirement(name: String, block: RequirementBuilder.() -> Unit) {
    val builder = RequirementBuilder(name)
    block(builder)
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
fun RequirementBuilder.byManualCheck() {
    this.query = { result -> QueryTree(true) }
}
