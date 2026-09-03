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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.AnnotationMember
import kotlin.test.Test
import kotlin.test.assertTrue

class SccPassTest {
    /**
     * Regression test for a bug where [SccPass.tarjan] used `break` instead of `continue` when
     * hitting a blacklisted node while iterating a node's `nextEOG` successors. `break` aborts the
     * whole successor scan on the first blacklisted node, silently dropping any successors that
     * come after it - instead of just skipping that one blacklisted successor and continuing to
     * scan the others.
     *
     * The blacklist is only ever non-empty for `level >= 2` (nested-loop decomposition, see
     * [SccPass.tarjan]'s call at the end of the method), so we exercise this directly by
     * pre-seeding a [SccPass.TarjanInfo] with a blacklist, bypassing the need for a real nested
     * loop to be parsed from source.
     *
     * We use [AnnotationMember] as a stand-in graph node rather than
     * [BasicBlock][de.fraunhofer.aisec.cpg.graph.overlays.BasicBlock]: `BasicBlock.location` is a
     * computed property that returns a non-null placeholder even when the block is empty, which
     * pushes [de.fraunhofer.aisec.cpg.graph.Node.equals] into structural comparison - two empty
     * `BasicBlock`s then compare as equal to each other, breaking the distinct node identities this
     * test depends on. Any plain [Node][de.fraunhofer.aisec.cpg.graph.Node] subclass with `location
     * == null` correctly falls back to reference equality instead.
     */
    @Test
    fun testBlacklistedNodeDoesNotAbortSuccessorScan() {
        val pass = SccPass(TranslationContext())

        val bb = AnnotationMember()
        val blacklisted = AnnotationMember()
        val live = AnnotationMember()

        // blacklisted comes first, live comes after it - this ordering is what the `break` bug
        // depended on to drop `live` entirely.
        bb.nextEOG.add(blacklisted)
        bb.nextEOG.add(live)

        val level = 2
        pass.tarjanInfoMap[level] = SccPass.TarjanInfo(listOf(blacklisted))

        pass.tarjan(bb, level)

        assertTrue(
            live in pass.tarjanInfoMap.getValue(level).visited,
            "live successor after a blacklisted one must still be visited",
        )
    }
}
