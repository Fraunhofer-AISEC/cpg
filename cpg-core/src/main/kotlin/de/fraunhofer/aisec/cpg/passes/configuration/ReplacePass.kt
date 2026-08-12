/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass
import de.fraunhofer.aisec.cpg.passes.Pass
import de.fraunhofer.aisec.cpg.passes.TranslationUnitPass
import kotlin.reflect.KClass

/**
 * This annotation can be used to replace a certain [Pass] (identified by [old]) for a specific
 * [Language] (identified by [lang]) with another [Pass] (identified by [with]).
 *
 * The primary use-case for this annotation is to allow language frontends to override specific
 * passes, such as the [EvaluationOrderGraphPass] in order to optimize language specific graphs.
 *
 * Please, be careful: DO NOT register the to-be-replaced pass with registerPass. Additionally,
 * currently, only a [TranslationUnitPass] can be replaced.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Repeatable
annotation class ReplacePass(
    val old: KClass<out Pass<*>>,
    val lang: KClass<out Language<*>>,
    val with: KClass<out Pass<*>>,
)
