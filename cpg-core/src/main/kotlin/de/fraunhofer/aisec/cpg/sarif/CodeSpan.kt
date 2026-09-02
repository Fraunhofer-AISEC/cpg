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
package de.fraunhofer.aisec.cpg.sarif

/**
 * A `[start, end)` char range into [content], shared by every node whose `code` happens to be
 * exactly that range of the file. Storing this instead of a dedicated copy of the substring avoids
 * the O(depth) duplication that otherwise comes from nested AST nodes each holding an overlapping
 * copy of their ancestors' code.
 */
internal class CodeSpan(val content: String, val start: Int, val end: Int) {
    fun materialize(): String = content.substring(start, end)
}

/**
 * Returns a [CodeSpan] into [location]'s file that reproduces [rawCode] exactly, or `null` if no
 * content is registered yet for that file (see [PhysicalLocation.ArtifactLocation.indexedContent]),
 * or the region computed from [location] does not match [rawCode] verbatim.
 *
 * This is opportunistic and self-verifying: it only ever returns a [CodeSpan] once it has
 * confirmed, character for character, that the range it computed reproduces [rawCode] exactly. This
 * makes it safe to use regardless of how a particular language frontend actually derived [rawCode]
 * (an exact substring of the file, a pretty-printed reconstruction, byte- vs. UTF-16- indexed
 * columns, a different line-break convention, ...): a mismatch simply yields `null`, and the caller
 * falls back to storing [rawCode] directly, exactly as if this never existed.
 */
internal fun tryInternCode(location: PhysicalLocation, rawCode: String): CodeSpan? {
    val indexed = location.artifactLocation.indexedContent ?: return null
    val region = location.region

    val start = indexed.offsetOf(region.startLine, region.startColumn) ?: return null
    val end = indexed.offsetOf(region.endLine, region.endColumn) ?: return null
    if (end < start || end - start != rawCode.length) {
        return null
    }

    return if (indexed.text.regionMatches(start, rawCode, 0, rawCode.length)) {
        CodeSpan(indexed.text, start, end)
    } else {
        null
    }
}
