/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
package de.fraunhofer.aisec.cpg.frontends.php

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PHPLanguageFrontendTest {
    @Test
    fun testSimpleFunctionParsing() {
        val topLevel = Path.of("src", "test", "resources", "php")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("functions.php").toFile()), topLevel, true) {
                it.registerLanguage<PHPLanguage>()
            }
        assertNotNull(tu)

        val greet = tu.functions["greet"]
        assertNotNull(greet)
        assertLocalName("name", greet.parameters[0])

        val sum = tu.functions["sum"]
        assertNotNull(sum)
        assertLocalName("a", sum.parameters[0])
        assertLocalName("b", sum.parameters[1])

        val joinAll = tu.functions["joinAll"]
        assertNotNull(joinAll)
        assertLocalName("parts", joinAll.parameters[0])
        assertTrue(joinAll.parameters[0].isVariadic)
    }
}
