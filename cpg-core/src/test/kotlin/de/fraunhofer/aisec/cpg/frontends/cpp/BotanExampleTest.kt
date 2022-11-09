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

        val doCrypt =
            declarations.stream().filter { it.fullName.localName == "do_crypt" }.findFirst().get()
        assertTrue(doCrypt is FunctionDeclaration)
        assertEquals("do_crypt", doCrypt.fullName.localName)

        val encrypt =
            declarations.stream().filter { it.fullName.localName == "encrypt" }.findFirst().get()
        assertTrue(encrypt is FunctionDeclaration)
        assertEquals("encrypt", encrypt.fullName.localName)

        val decrypt =
            declarations.stream().filter { it.fullName.localName == "decrypt" }.findFirst().get()
        assertTrue(decrypt is FunctionDeclaration)
        assertEquals("decrypt", decrypt.fullName.localName)

        val main =
            declarations.stream().filter { it.fullName.localName == "main" }.findFirst().get()
        assertTrue(main is FunctionDeclaration)
        assertEquals("main", main.fullName.localName)
    }
}
