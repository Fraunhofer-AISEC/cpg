/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.passes.configuration

import de.fraunhofer.aisec.cpg.passes.Pass
import kotlin.reflect.KClass

/**
 * A simple helper class to match a pass with its dependencies. [dependenciesRemaining] shows the
 * currently remaining / unsatisfied dependencies. These values are updated during the ordering
 * procedure.
 */
data class PassWithDependencies(
    /** the pass itself */
    val passClass: KClass<out Pass<*>>,
    /** currently unsatisfied dependencies (soft / hard / [ExecuteBefore] from other passes) */
    val dependenciesRemaining: MutableSet<KClass<out Pass<*>>>,
)
