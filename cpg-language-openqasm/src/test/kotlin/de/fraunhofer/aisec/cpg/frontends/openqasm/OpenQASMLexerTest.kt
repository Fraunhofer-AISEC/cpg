/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.openqasm

import de.fraunhofer.aisec.cpg.BaseTest
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.net.URI
import java.nio.file.Path
import org.junit.jupiter.api.Test

class OpenQASMLexerTest : BaseTest() {
    // TODO ensure gradle doesn't remove those classes
    private val dummyRegion = Region()
    private val dummyPhysicalLocation = PhysicalLocation(URI(""), dummyRegion)

    @Test
    fun testLexer() {
        val topLevel = Path.of("src", "test", "resources", "openqasm")

        val lexer = OpenQASMLexer(topLevel.resolve("adder.qasm").toFile())
        lexer.run()
        assert(lexer.tokens.isNotEmpty())
    }

    @Test
    fun testLexer2() {
        val topLevel = Path.of("src", "test", "resources", "openqasm")

        val lexer = OpenQASMLexer(topLevel.resolve("comments_and_symbols.qasm").toFile())
        lexer.run()
        assert(lexer.tokens.isNotEmpty())
    }

    @Test
    fun testLexer3() {
        val topLevel = Path.of("src", "test", "resources", "openqasm")

        val lexer =
            OpenQASMLexer(topLevel.resolve("comments_and_symbols_windows_endline.qasm").toFile())
        lexer.run()
        assert(lexer.tokens.isNotEmpty())
    }

    @Test
    fun testLexer4() {
        val topLevel = Path.of("src", "test", "resources", "openqasm")

        val lexer = OpenQASMLexer(topLevel.resolve("qiskit_intro_tutorial_1.qasm").toFile())
        lexer.run()
        assert(lexer.tokens.isNotEmpty())
    }
}
