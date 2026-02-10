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
package de.fraunhofer.aisec.cpg.frontends.rust

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.nio.file.Path
import kotlin.test.*

class RustTraitsTest : BaseTest() {
    @Test
    fun testTraits() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("traits.rs").toFile()), topLevel, true) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val myTrait = tu.records["MyTrait"]
        assertNotNull(myTrait)
        assertEquals("trait", myTrait.kind)

        val requiredMethod = myTrait.methods["required_method"]
        assertNotNull(requiredMethod)
        assertFalse(requiredMethod.hasBody())

        val defaultMethod = myTrait.methods["default_method"]
        assertNotNull(defaultMethod)
        assertTrue(defaultMethod.hasBody())

        val myStruct = tu.records["MyStruct"]
        assertNotNull(myStruct)

        // Check implementation
        val implMethod = myStruct.methods["required_method"]
        assertNotNull(implMethod)
        assertEquals(myStruct, implMethod.recordDeclaration)

        // In Rust, we want to link implementations to their traits if possible.
        // For now, let's just ensure the method is there and has a body.
        assertTrue(implMethod.hasBody())
    }
}
