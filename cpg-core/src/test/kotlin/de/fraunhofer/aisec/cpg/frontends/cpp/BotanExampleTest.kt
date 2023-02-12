/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.cpp

import de.fraunhofer.aisec.cpg.BaseTest
import de.fraunhofer.aisec.cpg.TestUtils.analyzeAndGetFirstTU
import de.fraunhofer.aisec.cpg.assertLocalName
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class BotanExampleTest : BaseTest() {
    @Test
    fun testExample() {
        val file = File("src/test/resources/botan/symm_block_cipher.cpp")
        val declaration = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), false)
        assertNotNull(declaration)

        val declarations = declaration.declarations
        assertEquals(5, declarations.size)

        val doCrypt = declarations.firstOrNull { it.name.localName == "do_crypt" }
        assertTrue(doCrypt is FunctionDeclaration)
        assertLocalName("do_crypt", doCrypt)

        val encrypt = declarations.firstOrNull { it.name.localName == "encrypt" }
        assertTrue(encrypt is FunctionDeclaration)
        assertLocalName("encrypt", encrypt)

        val decrypt = declarations.firstOrNull { it.name.localName == "decrypt" }
        assertTrue(decrypt is FunctionDeclaration)
        assertLocalName("decrypt", decrypt)

        val main = declarations.firstOrNull { it.name.localName == "main" }
        assertTrue(main is FunctionDeclaration)
        assertLocalName("main", main)
    }
}
