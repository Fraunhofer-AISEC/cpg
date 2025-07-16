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
package de.fraunhofer.aisec.cpg.helpers

import de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage
import de.fraunhofer.aisec.cpg.frontends.cxx.CPPLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.test.*
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CommentsTest {
    @Test
    fun testCommentMatcher() {
        val file = File("src/test/resources/c/comments.c")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
                it.registerLanguage<CLanguage>()
            }
        assertNotNull(tu)

        val functionDoc = tu.dFunctions["functionDoc"]
        assertNotNull(functionDoc)
        assertEquals("/**\n * Documentation comment\n */", functionDoc.comment)

        val functionMultiline = tu.dFunctions["functionMultiline"]
        assertNotNull(functionMultiline)
        assertEquals("/*\n * Multi-line\n * comment\n */", functionMultiline.comment)

        val functionSimple = tu.dFunctions["functionSimple"]
        assertNotNull(functionSimple)
        assertEquals("// Simple comment", functionSimple.comment)
    }
}
