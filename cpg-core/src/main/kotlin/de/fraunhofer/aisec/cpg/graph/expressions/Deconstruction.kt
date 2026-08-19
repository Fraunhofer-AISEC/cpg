/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.expressions

/**
 * Base class for pattern-matching expressions that unpack a value into its constituent parts, each
 * typically bound to a new identifier — the inverse of a [Construction], with data/type flow
 * running from the matched value down into the bindings instead of up into a result.
 *
 * Subclasses: [ObjectDeconstruction] (positional/named decomposition, e.g. Rust's `Point { x, y
 * }`), [NamedDeconstruction] (one named field of it, e.g. `y: y_coord`), and
 * [AlternativeDeconstruction] (or-patterns, e.g. Rust's `1 | 2`). Used e.g. by Rust's `let`, `if
 * let`, `while let`, and `match`.
 */
abstract class Deconstruction : Expression()
