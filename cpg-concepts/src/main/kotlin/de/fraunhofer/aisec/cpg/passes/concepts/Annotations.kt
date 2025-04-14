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
package de.fraunhofer.aisec.cpg.passes.concepts

import de.fraunhofer.aisec.cpg.graph.OverlayNode
import kotlin.reflect.KClass

/** Annotation to mark a function as a producer of a specific [OverlayNode] type. */
@Target(AnnotationTarget.FUNCTION) annotation class Produces(val klass: KClass<out OverlayNode>)

/** Annotation to mark a function as consuming a specific [OverlayNode] type. */
@Target(AnnotationTarget.FUNCTION) annotation class Consumes(val klass: KClass<out OverlayNode>)

/**
 * Annotation to mark a function as requiring a specific language. This is used to indicate that the
 * function should only be called for nodes of a specific language.
 */
@Target(AnnotationTarget.FUNCTION) annotation class RequiresLanguage(val language: String)
