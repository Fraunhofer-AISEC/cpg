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
