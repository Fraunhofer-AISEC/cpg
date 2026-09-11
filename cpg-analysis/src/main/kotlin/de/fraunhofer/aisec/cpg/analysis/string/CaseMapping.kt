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
package de.fraunhofer.aisec.cpg.analysis.string

/**
 * Maps every [StringPattern.Const] leaf of [p] through [f], re-normalising the result via the smart
 * constructors. [StringPattern.Unknown] leaves have their [CharSet] mapped via [mapCharSet], which
 * soundly accounts for characters whose full-string case mapping under [f] produces more than one
 * character (e.g. German `ß` uppercasing to `"SS"`).
 *
 * Provably terminating: this is a structural recursion over [p], which is already a finite term
 * (bounded by the evaluator's `maxTermSize`/`maxTermDepth`) - no new nesting is introduced.
 *
 * Shared between [de.fraunhofer.aisec.cpg.analysis.string.python.PythonStringOperationHandler]'s
 * `upper`/`lower` and [de.fraunhofer.aisec.cpg.analysis.string.golang.GoStringOperationHandler]'s
 * `strings.ToUpper`/`strings.ToLower`, whose case-mapping logic is otherwise identical.
 */
internal fun mapConstLeaves(p: StringPattern, f: (String) -> String): StringPattern =
    when (p) {
        is StringPattern.Bottom -> p
        is StringPattern.Const -> const(f(p.value))
        is StringPattern.Concat -> concat(p.parts.map { mapConstLeaves(it, f) })
        is StringPattern.Union -> union(p.alternatives.map { mapConstLeaves(it, f) })
        is StringPattern.Star -> star(mapConstLeaves(p.inner, f), p.min, p.max)
        is StringPattern.Unknown -> p.copy(charSet = mapCharSet(p.charSet, f))
    }

/**
 * Maps [charSet] through [f], applied to each character's full-string representation (so that a
 * character whose mapping under [f] is itself multiple characters, e.g. German `ß` uppercasing to
 * `"SS"`, contributes *all* of those resulting characters). Never drops a possible output character
 * - dropping would under-approximate the resulting [CharSet], violating this domain's soundness
 *   invariant (see the design doc: results must always be supersets of what is actually reachable).
 */
internal fun mapCharSet(charSet: CharSet, f: (String) -> String): CharSet =
    when (charSet) {
        is CharSet.Empty,
        is CharSet.Any -> charSet
        is CharSet.Chars -> charsOf(charSet.chars.flatMap { c -> f(c.toString()).toList() }.toSet())
    }
