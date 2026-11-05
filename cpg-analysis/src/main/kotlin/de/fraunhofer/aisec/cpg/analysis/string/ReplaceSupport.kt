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
 * `true` iff [old] provably cannot start a match within [prefix], including a match that starts
 * inside [prefix] and extends past its end - the condition under which `Concat(prefix, Unknown)`
 * soundly over-approximates `receiver.replace(old, new)` when `receiver`'s known prefix is exactly
 * [prefix]. Conservative: returns `false` (i.e. "cannot rule it out") whenever unsure.
 *
 * Shared between [de.fraunhofer.aisec.cpg.analysis.string.python.PythonStringOperationHandler] and
 * [de.fraunhofer.aisec.cpg.analysis.string.jvm.JvmStringOperationHandler], whose `replace`
 * soundness-fallback logic is otherwise identical.
 */
internal fun cannotOccurWithinPrefix(prefix: String, old: String): Boolean {
    if (prefix.contains(old)) return false
    val maxOverlap = minOf(prefix.length, old.length - 1)
    for (k in 1..maxOverlap) {
        if (prefix.endsWith(old.substring(0, k))) return false
    }
    return true
}

/**
 * Replaces at most [count] non-overlapping occurrences of [old] in [receiver] with [new], left to
 * right, leaving the rest of [receiver] untouched. `count <= 0` is a no-op (mirrors both Python's
 * `str.replace(old, new, count)` and Go's `strings.Replace(s, old, new, n)`, which treat a
 * non-positive bound as "replace nothing" - "replace all" is `n < 0`/no `count` argument in those
 * APIs, handled by the caller passing `Int.MAX_VALUE` rather than by this function).
 *
 * Shared between [de.fraunhofer.aisec.cpg.analysis.string.python.PythonStringOperationHandler] and
 * [de.fraunhofer.aisec.cpg.analysis.string.golang.GoStringOperationHandler], whose exact-replace
 * logic is otherwise identical.
 */
internal fun boundedReplace(receiver: String, old: String, new: String, count: Int): String {
    if (count <= 0) return receiver
    val sb = StringBuilder()
    var i = 0
    var remaining = count
    while (i < receiver.length) {
        if (remaining > 0 && receiver.startsWith(old, i)) {
            sb.append(new)
            i += old.length
            remaining--
        } else {
            sb.append(receiver[i])
            i++
        }
    }
    return sb.toString()
}
