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
 * The tokenize/walk-matches/literal-segment-stitching skeleton shared by every `printf`/`.format()`
 * -style format handler (C's `handleSnprintf`, Go's `handleSprintf`,
 * [de.fraunhofer.aisec.cpg.analysis.string.jvm.JvmStringOperationHandler.handleFormat], Python's
 * `handleBinaryOperator` (`%`-formatting) and `handleFormat`): find every match of [token] in
 * [formatString], emit the literal text before each match as a [StringPattern.Const], let [render]
 * decide what [StringPattern] a match itself contributes (consuming positional arguments,
 * dispatching on the conversion/verb/field - this is where the handlers genuinely differ and is
 * deliberately *not* unified here), and emit any trailing literal text after the last match.
 * [render] is called once per match, in order, so a `var` captured from the caller's enclosing
 * scope (e.g. a positional argument index) can be mutated across calls exactly as it was in each
 * handler's own loop before this was factored out.
 */
internal fun renderFormatString(
    formatString: String,
    token: Regex,
    render: (MatchResult) -> StringPattern,
): StringPattern {
    val parts = mutableListOf<StringPattern>()
    var lastEnd = 0
    for (m in token.findAll(formatString)) {
        if (m.range.first > lastEnd) {
            parts.add(const(formatString.substring(lastEnd, m.range.first)))
        }
        parts.add(render(m))
        lastEnd = m.range.last + 1
    }
    if (lastEnd < formatString.length) {
        parts.add(const(formatString.substring(lastEnd)))
    }
    return concat(parts)
}
