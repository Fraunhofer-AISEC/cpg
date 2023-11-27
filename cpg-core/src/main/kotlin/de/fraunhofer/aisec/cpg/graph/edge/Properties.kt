/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.edge

import java.util.*

/**
 * INDEX: (int) Indicates the position in a list of edges
 *
 * BRANCH: (boolean) If we have multiple EOG edges the branch property indicates which EOG edge
 * leads to true branch (expression evaluated to true) or the false branch (e.g. with an if/else
 * condition)
 *
 * DEFAULT: (boolean) Indicates which arguments edge of a CallExpression leads to a default argument
 *
 * NAME: (String) An optional name for the property edge
 *
 * [UNREACHABLE]:(boolean) True if the edge flows into unreachable code i.e. a branch condition
 * which is always false.
 *
 * [DEPENDENCE]: ([DependenceType] Specifies the type of dependence the property edge might
 * represent
 */
enum class Properties {
    INDEX,
    BRANCH,
    NAME,
    INSTANTIATION,
    UNREACHABLE,
    ACCESS,
    DEPENDENCE,
    DYNAMIC_INVOKE,
    SENSITIVITY,
    CALLING_CONTEXT_IN,
    CALLING_CONTEXT_OUT
}

/** The types of dependencies that might be represented in the CPG */
enum class DependenceType {
    CONTROL,
    DATA
}

/** Sensitivity options (of DFG edges). */
enum class SensitivitySpecifier {
    FIELD,
    CONTEXT;

    infix fun and(other: SensitivitySpecifier) = Sensitivities.of(this, other)
}

typealias Sensitivities = EnumSet<SensitivitySpecifier>

infix fun Sensitivities.allOf(other: Sensitivities) = this.containsAll(other)

infix fun Sensitivities.and(other: SensitivitySpecifier) =
    Sensitivities.of(other, *this.toTypedArray())
