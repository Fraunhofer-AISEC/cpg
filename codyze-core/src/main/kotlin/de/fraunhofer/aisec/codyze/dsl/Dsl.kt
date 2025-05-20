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

class RequirementBuilder(var name: String = "") {
    var query: ((result: TranslationResult) -> QueryTree<Boolean>)? = null
}

class RequirementsBuilder {}

class ToEBuilder {
    var name: String? = null
}

class ArchitectureBuilder {}

class ModulesBuilder {}

/**
 * Represents that all files of a module should be included. This is done by using an empty list.
 */
val ALL = listOf<String>()

class ModuleBuilder(var name: String = "") {
    var directory: String = ""
    var files: List<String> = ALL
}

class ProjectBuilder {}

@CodyzeDsl fun CodyzeScript.project(block: ProjectBuilder.() -> Unit) {}

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

@CodyzeDsl
fun ToEBuilder.requirement(name: String, block: RequirementBuilder.() -> Unit) {
    val builder = RequirementBuilder(name)
    block(builder)
}

fun RequirementBuilder.byQuery(query: (result: TranslationResult) -> QueryTree<Boolean>) {
    this.query = query
}

fun RequirementBuilder.byManualCheck() {
    this.query = { result -> QueryTree(true) }
}
